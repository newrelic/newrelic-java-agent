/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.instrumentation.InstrumentationUtils;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceClassTransformer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.asm.Utils;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class InstrumentationClassTransformer implements ClassFileTransformer {

    /**
     * Marker interfaces for generated proxy classes that we never want to instrument.
     *
     * @see InstrumentationContextManager#skipInterfaceMarkers(ClassReader)
     */
    private static final Set<String> MARKER_INTERFACES_TO_SKIP = ImmutableSet.of("org/hibernate/proxy/HibernateProxy",
            "org/springframework/aop/SpringProxy", "java/security/PrivilegedAction");

    private final InstrumentationContextManager manager;
    private final TraceClassTransformer traceTransformer;
    private final boolean bootstrapClassloaderEnabled;
    private final boolean defaultMethodTracingEnabled;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final FinalClassTransformer finalClassTransformer = new FinalClassTransformer();

    public InstrumentationClassTransformer(InstrumentationContextManager manager,
            TraceClassTransformer traceTransformer, boolean bootstrapClassloaderEnabled, boolean defaultMethodTracingEnabled) {
        this.manager = manager;
        this.traceTransformer = traceTransformer;
        this.bootstrapClassloaderEnabled = bootstrapClassloaderEnabled;
        this.defaultMethodTracingEnabled = defaultMethodTracingEnabled;
    }

    public void setInitialized(boolean isInitialized) {
        initialized.set(isInitialized);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        long transformStartTimeInNs = System.nanoTime();

        //Submit the class for possible analysis from the jar collector
        submitTransformCandidateToJarCollector(protectionDomain);

        try {
            if (className == null) {
                return null;
            }

            if (!initialized.get() && className.startsWith("com/newrelic/agent/")) {
                return null;
            }

            if (loader == null && !bootstrapClassloaderEnabled) {
                Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by ''bootstrap'' rule: {0}", className));
                return null;
            }

            if (!manager.shouldTransform(className, loader)) {
                return null;
            }

            ClassReader reader = new ClassReader(classfileBuffer);
            if (InstrumentationUtils.isAnnotation(reader)) {
                return null;
            }

            if (InstrumentationUtils.isInterface(reader)) {
                manager.applyInterfaceVisitors(loader, classBeingRedefined, reader);
                if (!InstrumentationUtils.isDefaultMethodSupported(reader) || !defaultMethodTracingEnabled) {
                    return null;
                }
            }

            if (Utils.isJdkProxy(reader)) {
                Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by ''JDK proxy'' rule: {0}", className));
                return null;
            }

            InstrumentationContext context = new InstrumentationContext(classfileBuffer, classBeingRedefined, protectionDomain);
            context.match(loader, classBeingRedefined, reader, manager.getMatchVisitors().keySet());

            // Additional proxy detection tests
            if (context.isGenerated()) {
                if (context.hasSourceAttribute()) {
                    Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by ''generated'' rule: {0}", className));
                } else {
                    Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by ''no source'' rule: {0}", className));
                }
                return null;
            }

            if (!context.getMatches().isEmpty() && skipInterfaceMarkers(reader)) {
                Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by ''class name'' rule: {0}", className));
                return null;
            }

            for (Map.Entry<ClassMatchVisitorFactory, OptimizedClassMatcher.Match> entry : context.getMatches().entrySet()) {
                ContextClassTransformer transformer = manager.getMatchVisitors().get(entry.getKey());
                if (transformer != null && transformer != InstrumentationContextManager.NO_OP_TRANSFORMER) {
                    byte[] bytes = transformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer, context, entry.getValue());
                    if (bytes != null) {
                        context.markAsModified();
                        classfileBuffer = bytes;
                    }
                } else {
                    // this should never happen
                    Agent.LOG.log(Level.FINE, "Unable to find a class transformer to process match {0}", entry.getValue());
                }
            }

            if (context.isTracerMatch()) {
                byte[] bytes = traceTransformer.transform(loader, className, classBeingRedefined,
                        protectionDomain, classfileBuffer, context, null);
                if (bytes != null) {
                    context.markAsModified();
                    classfileBuffer = bytes;
                }
            }

            if (context.isModified()) {
                byte[] transformation = finalClassTransformer.transform(loader, className,
                        classBeingRedefined, protectionDomain, classfileBuffer, context, null);
                ServiceFactory.getStatsService().doStatsWork(
                        StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_CLASSLOADER_TRANSFORM_TIME,
                                System.nanoTime() - transformStartTimeInNs), MetricNames.SUPPORTABILITY_CLASSLOADER_TRANSFORM_TIME);
                return transformation;
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINE, t, "Unexpected exception thrown in class transformer: {0}--{1}", loader, className);
        }

        return null; // for transformers this is the same as throwing an exception
    }

    /**
     * Don't instrument classes that implement any of the interfaces specified in {@link #MARKER_INTERFACES_TO_SKIP}.
     */
    private static boolean skipInterfaceMarkers(ClassReader reader) {
        for (String interfaceName : reader.getInterfaces()) {
            if (MARKER_INTERFACES_TO_SKIP.contains(interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private void submitTransformCandidateToJarCollector(ProtectionDomain protectionDomain) {
        if ((protectionDomain != null) && (protectionDomain.getCodeSource() != null)) {
            ServiceFactory.getJarCollectorService().getClassToJarPathSubmitter().processUrl(protectionDomain.getCodeSource().getLocation());
        }
    }
}
