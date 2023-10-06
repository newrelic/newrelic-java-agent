/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.JarCollectorConfigImpl;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Responsible for gathering and sending jars with version to the collector.
 */
public class JarCollectorServiceImpl extends AbstractService implements JarCollectorService {
    private final Logger logger;
    private final boolean enabled;
    private final AtomicBoolean shouldSendAllJars;
    private final TrackedAddSet<JarData> analyzedJars;
    private final ClassToJarPathSubmitter classToJarPathSubmitter;
    private volatile List<JarData> jarsNotSentLastHarvest = Collections.emptyList();

    public JarCollectorServiceImpl(
            Logger logger,
            boolean enabled,
            AtomicBoolean shouldSendAllJars,
            TrackedAddSet<JarData> analyzedJars,
            ClassToJarPathSubmitter classToJarPathSubmitter) {
        super(JarCollectorService.class.getSimpleName());

        this.shouldSendAllJars = shouldSendAllJars;
        this.analyzedJars = analyzedJars;
        this.logger = logger;
        this.classToJarPathSubmitter = classToJarPathSubmitter;
        this.enabled = enabled;

        if (JarCollectorConfigImpl.isUsingDeprecatedConfigSettings()) {
            String deprecatedConfigMsg = "Jar Collector system properties prefixed with 'newrelic.config.module.' and environment variables prefixed with "
                    + "'NEW_RELIC_MODULE_' are deprecated and will be removed in a future agent release. Instead use the "
                    + "'newrelic.config.jar_collector.' and 'NEW_RELIC_JAR_COLLECTOR_' prefixes.";
            this.logger.log(Level.INFO, deprecatedConfigMsg);
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_DEPRECATED_CONFIG_JAR_COLLECTOR);
        }
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public void harvest(String appName) {
        logger.log(Level.FINER, "Harvesting Modules");

        List<JarData> jarsToSend = getJars();

        if (!jarsToSend.isEmpty()) {
            try {
                // send the jars to the collector
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(appName)
                        .sendModules(jarsToSend);

                jarsNotSentLastHarvest = Collections.emptyList();
            } catch (Exception e) {
                // HttpError/LicenseException handled here
                logger.log(Level.FINE, MessageFormat.format("Unable to send {0} jar(s). Will attempt next harvest.", jarsToSend.size()));

                // requeue all the jar urls
                jarsNotSentLastHarvest = jarsToSend;
            }
        }
    }

    @Override
    public ClassToJarPathSubmitter getClassToJarPathSubmitter() {
        return classToJarPathSubmitter;
    }

    private List<JarData> getJars() {
        if (shouldSendAllJars.getAndSet(false)) {
            // anything that might have failed last time will definitely
            // be sent this time, so no need to worry about jarsNotSentLastHarvest.
            return new ArrayList<>(analyzedJars.resetReturningAll());
        }

        Set<JarData> newJarsToSend = analyzedJars.resetReturningAdded();
        if (newJarsToSend.isEmpty()) {
            return jarsNotSentLastHarvest;
        }

        List<JarData> jarData = new ArrayList<>(newJarsToSend);
        jarData.addAll(jarsNotSentLastHarvest);

        return jarData;
    }
}
