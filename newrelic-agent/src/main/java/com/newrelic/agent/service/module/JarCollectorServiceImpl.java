/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.JarCollectorConfig;
import com.newrelic.agent.config.JarCollectorConfigImpl;
import com.newrelic.agent.extension.ExtensionsLoadedListener;
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
    private final AtomicBoolean shouldSendAllJars;
    private final String defaultAppName;
    private final boolean enabled;
    private final ClassMatchVisitorFactory classMatchVisitorFactory;
    private final TrackedAddSet<JarData> analyzedJars;
    private final ExtensionsLoadedListener extensionsLoadedListener;
    private final Logger logger;

    private volatile List<JarData> jarsNotSentLastHarvest = Collections.emptyList();

    public JarCollectorServiceImpl(
            ConfigService configService,
            AtomicBoolean shouldSendAllJars,
            TrackedAddSet<JarData> analyzedJars,
            Logger jarCollectorLogger,
            ClassNoticingFactory classNoticingFactory,
            ExtensionsLoadedListener extensionsLoadedListener) {
        super(JarCollectorService.class.getSimpleName());
        this.shouldSendAllJars = shouldSendAllJars;
        this.analyzedJars = analyzedJars;
        logger = jarCollectorLogger;

        // get the default application
        AgentConfig config = configService.getDefaultAgentConfig();
        defaultAppName = config.getApplicationName();

        JarCollectorConfig jarCollectorConfig = config.getJarCollectorConfig();
        enabled = jarCollectorConfig.isEnabled();

        this.extensionsLoadedListener = enabled ? extensionsLoadedListener : ExtensionsLoadedListener.NOOP;
        this.classMatchVisitorFactory = enabled ? classNoticingFactory : ClassMatchVisitorFactory.NO_OP_FACTORY;

        if (JarCollectorConfigImpl.isUsingDeprecatedConfigSettings()) {
            String deprecatedConfigMsg = "Jar Collector system properties prefixed with 'newrelic.config.module.' and environment variables prefixed with "
                    + "'NEW_RELIC_MODULE_' are deprecated and will be removed in a future agent release. Instead use the "
                    + "'newrelic.config.jar_collector.' and 'NEW_RELIC_JAR_COLLECTOR_' prefixes.";
            logger.log(Level.INFO, deprecatedConfigMsg);
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_DEPRECATED_CONFIG_JAR_COLLECTOR);
        }
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public ExtensionsLoadedListener getExtensionsLoadedListener() {
        return extensionsLoadedListener;
    }

    @Override
    public ClassMatchVisitorFactory getSourceVisitor() {
        return classMatchVisitorFactory;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public void harvest() {
        logger.log(Level.FINER, "Harvesting Modules");

        List<JarData> jarsToSend = getJars();

        if (!jarsToSend.isEmpty()) {
            try {
                // send the jars to the collector
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(defaultAppName)
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
