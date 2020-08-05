/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.language;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicLongMap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.text.MessageFormat.format;

public class SourceLanguageService extends AbstractService implements HarvestListener {
    private static final long LANGUAGE_DETECTOR_DELAY_MILLIS = 2 * 60 * 1000; // 2 minutes
    private static final String UNKNOWN_EXTENSION = "unknown";

    // Ignore all target versions prior to 1.5, these version strings should match metric_names.txt in angler
    private static final Map<Integer, String> byteCodeVersions = ImmutableMap.<Integer, String>builder()
            .put(49, "1.5")
            .put(50, "1.6")
            .put(51, "1.7")
            .put(52, "1.8")
            .put(53, "1.9")
            .put(54, "1.10")
            .put(55, "1.11")
            .put(56, "1.12")
            .put(57, "1.13")
            .put(58, "1.14")
            .put(59, "1.15")
            .build();

    // There are A LOT of languages that can run on the JVM; only look for certain ones and collapse the known extensions
    // into a single grouping (e.g., "sc", "scala" are both valid scala extensions, so collapse down to "scala").
    private static final Map<String, String> knownSourceExtensions = ImmutableMap.<String, String>builder()
            .put("java", "java")
            .put("scala", "scala")
            .put("sc", "scala")
            .put("kt", "kotlin")
            .put("kts", "kotlin")
            .put("clj", "clojure")
            .put("cljs", "clojure")
            .put("cljc", "clojure")
            .put("edn", "clojure")
            .put("groovy", "groovy")
            .build();

    private final AtomicLongMap<String> counts = AtomicLongMap.create();
    private final Set<String> supportabilityKeys = new HashSet<>();
    private final SourceLibraryDetector sourceLibraryDetector = new SourceLibraryDetector();
    private Closeable sourceLibraryDetectorCloseable;

    public SourceLanguageService() {
        super(SourceLanguageService.class.getSimpleName());

        // pre-generate the list of keys and set counts to 0
        for (String byteCodeVersion : byteCodeVersions.values()) {
            for (String knownSourceExtension : knownSourceExtensions.values()) {
                if (!supportabilityKeys.contains(knownSourceExtension)) {
                    String supportabilityKey = format(MetricNames.SUPPORTABILITY_LOADED_CLASSES_SOURCE_VERSION, knownSourceExtension, byteCodeVersion);
                    counts.put(supportabilityKey, 0);
                    supportabilityKeys.add(supportabilityKey);
                }
            }
            String supportabilityKey = format(MetricNames.SUPPORTABILITY_LOADED_CLASSES_SOURCE_VERSION, UNKNOWN_EXTENSION, byteCodeVersion);
            counts.put(supportabilityKey, 0);
            supportabilityKeys.add(supportabilityKey);
        }
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        if (this.isStopped()) {
            return;
        }
        for (String supportabilityKey : supportabilityKeys) {
            int currentCount = (int) counts.put(supportabilityKey, 0);
            statsEngine.getResponseTimeStats(supportabilityKey).incrementCallCount(currentCount);
        }

        // shutdown the source library sampler if it's done
        if (sourceLibraryDetector.isDone() && sourceLibraryDetectorCloseable != null) {
            shutdownLanguageLibraryDetector();
        }
    }

    private void shutdownLanguageLibraryDetector() {
        try {
            Agent.LOG.log(Level.FINEST, "Shutting down source language library sampler");
            sourceLibraryDetectorCloseable.close();
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, e, "Error shutting down source language library sampler");
        } finally {
            sourceLibraryDetectorCloseable = null;
        }
    }

    @Override
    public void afterHarvest(String appName) {
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getHarvestService().addHarvestListener(this);

        // submit a quick task to detect any source libraries on the classpath that will run after a specified delay
        sourceLibraryDetectorCloseable = ServiceFactory.getSamplerService()
                .addSampler(sourceLibraryDetector, LANGUAGE_DETECTOR_DELAY_MILLIS, LANGUAGE_DETECTOR_DELAY_MILLIS * 2, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(this);

        if (sourceLibraryDetectorCloseable != null) {
            sourceLibraryDetectorCloseable.close();
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private void recordSeenMetric(String extension, String version) {
        String supportabilityKey = format(MetricNames.SUPPORTABILITY_LOADED_CLASSES_SOURCE_VERSION, extension, version);
        counts.incrementAndGet(supportabilityKey);
    }

    public ClassMatchVisitorFactory getSourceVisitor() {
        return new ClassMatchVisitorFactory() {
            @Override
            public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
                    ClassReader reader, ClassVisitor cv, final InstrumentationContext context) {

                return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
                    private String version;
                    private String extension;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        super.visit(version, access, name, signature, superName, interfaces);
                        this.version = resolveTargetByteCodeVersion(version);
                    }

                    @Override
                    public void visitSource(String source, String debug) {
                        super.visitSource(source, debug);
                        this.extension = resolveExtension(source);
                    }

                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        if (version != null) {
                            recordSeenMetric(extension, version);
                        }
                    }
                };
            }
        };
    }

    @VisibleForTesting
    static String resolveTargetByteCodeVersion(int version) {
        return byteCodeVersions.get(version);
    }

    @VisibleForTesting
    static String resolveExtension(String source) {
        int separatorIndex = findExtensionSeparator(source);
        if (separatorIndex > -1) {
            String detectedExtension = source.substring(separatorIndex + 1).toLowerCase();
            if (knownSourceExtensions.containsKey(detectedExtension)) {
                return knownSourceExtensions.get(detectedExtension);
            }
        }
        return UNKNOWN_EXTENSION;
    }

    @VisibleForTesting
    Map<String, Long> getSourceCounts() {
        return counts.asMap();
    }

    private static int findExtensionSeparator(String source) {
        return source != null ? source.indexOf(".") : -1;
    }
}
