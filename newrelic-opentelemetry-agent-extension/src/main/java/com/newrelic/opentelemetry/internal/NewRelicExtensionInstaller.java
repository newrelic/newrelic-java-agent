/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.opentelemetry.internal;

import com.newrelic.opentelemetry.OpenTelemetryNewRelic;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesBuilder;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesConfigurer;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rewrites {@link com.newrelic.api.agent.NewRelic} to be defined by {@link OpenTelemetryNewRelic}, installs {@link OpenTelemetry} in the redefined class.
 */
public class NewRelicExtensionInstaller implements BeforeAgentListener, IgnoredTypesConfigurer {

    private static final Logger logger = Logger.getLogger(NewRelicExtensionInstaller.class.getName());
    private static final String NEWRELIC_API_FQCN = "com.newrelic.api.agent.NewRelic";

    @Override
    public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
        OpenTelemetryNewRelic.install(autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk());
        Instrumentation inst = InstrumentationHolder.getInstrumentation();
        if (inst == null) {
            return;
        }

        // Rewrite NewRelic to use OpenTelemetryNewRelic
        try {
            inst.addTransformer(rewriteNewRelicClassTransformer());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failure rewriting NewRelic API", e);
            return;
        }

        // Install OpenTelemetry to rewritten NewRelic API
        try {
            installOpenTelemetry(autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failure installing OpenTelemetry in NewRelic API", e);
        }
    }

    private static ClassFileTransformer rewriteNewRelicClassTransformer() throws IOException {
        Map<String, byte[]> helperClassNameToBytes = new LinkedHashMap<>();
        try (ClassFileLocator helperClassFileLocator = ClassFileLocator.ForClassLoader.of(NewRelicExtensionInstaller.class.getClassLoader())) {
            List<String> classNames = Arrays.asList(
                    // NOTE: order matters with this list.
                    "com.newrelic.opentelemetry.NoOpConfig",
                    "com.newrelic.opentelemetry.NoOpDistributedTracePayload",
                    "com.newrelic.opentelemetry.NoOpLogger",
                    "com.newrelic.opentelemetry.NoOpSegment",
                    "com.newrelic.opentelemetry.NoOpToken",
                    "com.newrelic.opentelemetry.NoOpTraceMetadata",
                    "com.newrelic.opentelemetry.OpenTelemetryAgent",
                    "com.newrelic.opentelemetry.OpenTelemetryErrorApi",
                    "com.newrelic.opentelemetry.OpenTelemetryErrorApi$ReportedError",
                    "com.newrelic.opentelemetry.OpenTelemetryInsights",
                    "com.newrelic.opentelemetry.OpenTelemetryMetricsAggregator",
                    "com.newrelic.opentelemetry.OpenTelemetryNewRelic",
                    "com.newrelic.opentelemetry.OpenTelemetryTracedMethod",
                    "com.newrelic.opentelemetry.OpenTelemetryTransaction"
            );
            for (String className : classNames) {
                helperClassNameToBytes.put(className, helperClassFileLocator.locate(className).resolve());
            }
        }

        return new AgentBuilder.Default()
                // NOTE: uncomment this line to see any errors which occur redefining OpenTelemetryNewRelic
                //.with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                .type(ElementMatchers.named("com.newrelic.api.agent.NewRelic"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    // Inject helper classes into application class loader which loaded NewRelic
                    PrivilegedAction<Map<String, Class<?>>> action = () -> new ClassInjector.UsingReflection(classLoader, protectionDomain).injectRaw(
                            helperClassNameToBytes);
                    if (System.getSecurityManager() != null) {
                        java.security.AccessController.doPrivileged(action);
                    } else {
                        action.run();
                    }
                    // Redefine OpenTelemetryNewRelic to be named com.newrelic.api.agent.NewRelic
                    return new ByteBuddy().redefine(OpenTelemetryNewRelic.class).name(NEWRELIC_API_FQCN);
                })
                .makeRaw();
    }

    private static void installOpenTelemetry(OpenTelemetry openTelemetry) throws Exception {
        try {
            // Reflectively load rewritten NewRelic class using system class loader, and install OpenTelemetry
            Class<?> newRelic = ClassLoader.getSystemClassLoader().loadClass(NEWRELIC_API_FQCN);
            Method install = newRelic.getMethod("install", OpenTelemetry.class);
            install.invoke(null, openTelemetry);
        } catch (ClassNotFoundException | NoSuchMethodException |
                 IllegalAccessException | InvocationTargetException e) {
            throw new Exception(e);
        }
    }

    @Override
    public void configure(IgnoredTypesBuilder builder, ConfigProperties config) {
        builder.ignoreClass("com.newrelic.opentelemetry");
    }
}
