/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jfr;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.JfrConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.jfr.daemon.*;
import com.newrelic.telemetry.Attributes;

import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import static com.newrelic.jfr.daemon.AttributeNames.ENTITY_GUID;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.SetupUtils.buildCommonAttributes;
import static com.newrelic.jfr.daemon.SetupUtils.buildUploader;

public class JfrService extends AbstractService implements AgentConfigListener {

    private final JfrConfig jfrConfig;
    private final AgentConfig defaultAgentConfig;
    private JfrController jfrController;

    public JfrService(JfrConfig jfrConfig, AgentConfig defaultAgentConfig) {
        super(JfrService.class.getSimpleName());
        this.jfrConfig = jfrConfig;
        this.defaultAgentConfig = defaultAgentConfig;
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
    }

    @Override
    protected void doStart() {
        if (coreApisExist() && isEnabled()) {
            Agent.LOG.log(Level.INFO, "Attaching New Relic JFR Monitor");

            NewRelic.getAgent().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_JFR_SERVICE_STARTED_SUCCESS);

            try {
                final DaemonConfig daemonConfig = buildDaemonConfig();
                final Attributes commonAttrs = buildCommonAttributes(daemonConfig);
                final String entityGuid = ServiceFactory.getRPMService().getEntityGuid();
                Agent.LOG.log(Level.INFO, "JFR Monitor obtained entity guid from agent: " + entityGuid);
                commonAttrs.put(ENTITY_GUID, entityGuid);
                final String hostname = getHostname();
                commonAttrs.put(HOSTNAME, hostname);
                final JFRUploader uploader = buildUploader(daemonConfig);
                String pattern = defaultAgentConfig.getValue(ThreadService.NAME_PATTERN_CFG_KEY, ThreadNameNormalizer.DEFAULT_PATTERN);
                uploader.readyToSend(new EventConverter(commonAttrs, pattern));
                jfrController = SetupUtils.buildJfrController(daemonConfig, uploader);

                ExecutorService jfrMonitorService = Executors.newSingleThreadExecutor();
                jfrMonitorService.submit(
                    () -> {
                        try {
                            startJfrLoop();
                        } catch (JfrRecorderException e) {
                            Agent.LOG.log(Level.INFO, "Error in JFR Monitor, shutting down", e);
                            jfrController.shutdown();
                        }
                    });
            } catch (Throwable t) {
                Agent.LOG.log(Level.INFO, "Unable to attach JFR Monitor", t);
            }
        } else {
            NewRelic.getAgent().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_JFR_SERVICE_STARTED_FAIL);
        }
    }

    // Using Mockito spy, verify execution of this method as start of jfr.
    void startJfrLoop() throws JfrRecorderException {
        jfrController.loop();
    }

    @Override
    public final boolean isEnabled() {
        final boolean enabled = jfrConfig.isEnabled();
        boolean isHighSecurity = defaultAgentConfig.isHighSecurity();
        if (!enabled) {
            Agent.LOG.log(Level.INFO, "New Relic JFR Monitor is disabled: JFR config has not been enabled in the Java agent.");
        } else if (isHighSecurity) {
            Agent.LOG.log(Level.INFO, "New Relic JFR Monitor is enabled but High Security mode is also enabled; JFR will not be activated.");
        }
        return enabled && !isHighSecurity;
    }

    @Override
    protected void doStop() {
        NewRelic.getAgent().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_JFR_SERVICE_STOPPED_SUCCESS);

        if (jfrController != null) {
            jfrController.shutdown();
        }
    }

    @VisibleForTesting
    boolean coreApisExist() {
        try {
            Class.forName("jdk.jfr.Recording");
            Class.forName("jdk.jfr.FlightRecorder");
        } catch (ClassNotFoundException __) {
            Agent.LOG.log(Level.WARNING, "Not starting JFR Service. Core JFR APIs do not exist in this JVM.");
            return false;
        }
        return true;
    }

    private String getHostname() {
        String host;
        String appPort = ServiceFactory
            .getEnvironmentService()
            .getEnvironment()
            .getAgentIdentity()
            .getServerPort()
            .toString();
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.error(e.getMessage());
            host = InetAddress.getLoopbackAddress().getHostAddress();
        }
        return String.format("%s:%s", host, appPort);
    }

    @VisibleForTesting
    DaemonConfig buildDaemonConfig() {
        DaemonConfig.Builder builder = DaemonConfig.builder()
            .daemonVersion(VersionFinder.getVersion())
            .useLicenseKey(jfrConfig.useLicenseKey())
            .apiKey(defaultAgentConfig.getLicenseKey())
            .monitoredAppName(defaultAgentConfig.getApplicationName())
            .auditLogging(jfrConfig.auditLoggingEnabled())
            .metricsUri(URI.create(defaultAgentConfig.getMetricIngestUri()))
            .eventsUri(URI.create(defaultAgentConfig.getEventIngestUri()))
            .proxyHost(defaultAgentConfig.getProxyHost())
            .proxyScheme(defaultAgentConfig.getProxyScheme())
            .proxyPort(defaultAgentConfig.getProxyPort())
            .proxyUser(defaultAgentConfig.getProxyUser())
            .proxyPassword(defaultAgentConfig.getProxyPassword());
        return builder.build();
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        boolean newJfrEnabledVal = agentConfig.getJfrConfig().isEnabled();

        if (newJfrEnabledVal != jfrConfig.isEnabled()) {
            Agent.LOG.log(Level.INFO, "JFR enabled flag changed to {0}", newJfrEnabledVal);
            jfrConfig.setEnabled(newJfrEnabledVal);

            if (newJfrEnabledVal) {
                doStart();
            } else {
                doStop();
            }
        }
    }
}
