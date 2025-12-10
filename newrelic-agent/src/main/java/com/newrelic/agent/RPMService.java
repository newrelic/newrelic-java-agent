/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.BrowserMonitoringConfig;
import com.newrelic.agent.config.BrowserMonitoringConfigImpl;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.environment.AgentIdentity;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentChangeListener;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.rpm.RPMConnectionServiceImpl;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.JarData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.agentcontrol.HealthDataProducer;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.transaction.TransactionNamingScheme;
import com.newrelic.agent.transport.ConnectionResponse;
import com.newrelic.agent.transport.DataSender;
import com.newrelic.agent.transport.DataSenderFactory;
import com.newrelic.agent.transport.DataSenderListener;
import com.newrelic.agent.transport.HostConnectException;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.transport.HttpResponseCode;
import com.newrelic.agent.transport.serverless.DataSenderServerlessConfig;
import com.newrelic.agent.utilization.UtilizationData;
import org.json.simple.JSONStreamAware;

import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.rmi.UnexpectedException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * The RPMService acts as a stub for communication between the agent and New Relic.
 */
public class RPMService extends AbstractService implements IRPMService, EnvironmentChangeListener,
        AgentConfigListener {

    public static final String COLLECT_TRACES_KEY = "collect_traces";
    public static final String COLLECT_ERRORS_KEY = "collect_errors";
    public static final String DATA_REPORT_PERIOD_KEY = "data_report_period";

    /**
     * If the exception has occurred 5 times, then print out the message.
     */
    private static final int LOG_MESSAGE_COUNT = 5;

    private final String host;
    private final int port;
    private final boolean serverlessMode;
    private final List<AgentConnectionEstablishedListener> agentConnectionEstablishedListeners;
    private volatile boolean connected = false;
    private final ErrorService errorService;
    private final String appName;
    private final List<String> appNames;
    private final ConnectionConfigListener connectionConfigListener;
    private final ConnectionListener connectionListener;
    private final boolean isMainApp;
    private volatile boolean hasEverConnected = false;
    private volatile String entityGuid = "";
    private final DataSender dataSender;
    private long connectionTimestamp = 0;
    private final AtomicInteger last503Error = new AtomicInteger(0);
    private final AtomicInteger retryCount = new AtomicInteger(0);

    private String rpmLink;
    private long lastReportTime;

    public RPMService(List<String> appNames, ConnectionConfigListener connectionConfigListener, ConnectionListener connectionListener,
            List<AgentConnectionEstablishedListener> agentConnectionEstablishedListeners) {
        this(appNames, connectionConfigListener, connectionListener, null, agentConnectionEstablishedListeners);
    }

    RPMService(List<String> appNames, ConnectionConfigListener connectionConfigListener, ConnectionListener connectionListener,
            DataSenderListener dataSenderListener, List<AgentConnectionEstablishedListener> agentConnectionEstablishedListeners) {
        super(RPMService.class.getSimpleName() + "/" + appNames.get(0));
        appName = appNames.get(0).intern();
        AgentConfig config = ServiceFactory.getConfigService().getAgentConfig(appName);
        this.serverlessMode = config.getServerlessConfig().isEnabled();
        if (serverlessMode) {
            dataSender = DataSenderFactory.createServerless(new DataSenderServerlessConfig(Agent.getVersion()), Agent.LOG, config.getServerlessConfig());
        } else {
            dataSender = DataSenderFactory.create(config, dataSenderListener);
        }
        this.appNames = appNames;
        this.connectionConfigListener = connectionConfigListener;
        this.connectionListener = connectionListener;
        lastReportTime = System.currentTimeMillis();
        errorService = new ErrorServiceImpl(appName);
        host = config.getHost();
        port = config.getPort();
        isMainApp = appName.equals(config.getApplicationName());
        this.agentConnectionEstablishedListeners = new ArrayList<>(agentConnectionEstablishedListeners);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() throws Exception {
        addHarvestablesToServices();
        connect();
        ServiceFactory.getEnvironmentService().getEnvironment().addEnvironmentChangeListener(this);
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
        ServiceFactory.getServiceManager().getCircuitBreakerService().addRPMService(this);
        errorService.start();
    }

    private Boolean getAndLogHighSecurity(AgentConfig config) {
        boolean isHighSec = config.isHighSecurity();
        if (isHighSec) {
            Agent.LOG.log(Level.INFO, "High security is configured locally for application {0}.", appName);
        }
        return isHighSec;
    }

    private void addHarvestablesToServices() {
        ServiceFactory.getServiceManager().getInsights().addHarvestableToService(appName);
        ServiceFactory.getServiceManager().getLogSenderService().addHarvestableToService(appName);
        ServiceFactory.getTransactionEventsService().addHarvestableToService(appName);
        errorService.addHarvestableToService();
        ServiceFactory.getSpanEventService().addHarvestableToService(appName);
    }

    /**
     * Returns a map of startup options to be sent to RPM when the RPM service connects.
     */
    protected Map<String, Object> getStartOptions() {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(appName);

        int pid = ServiceFactory.getEnvironmentService().getProcessPID();
        Map<String, Object> options = new HashMap<>();
        // options.add(System.getProperty("user.dir"));
        options.put("pid", pid);
        String language = agentConfig.getLanguage();
        options.put("language", language);
        String defaultHost = Hostname.getHostname(agentConfig, true);
        options.put("host", defaultHost);
        String displayHost = Hostname.getDisplayHostname(agentConfig, defaultHost);
        options.put("display_host", displayHost);
        Agent.LOG.log(Level.INFO, "Host name is {0}, display host {1} for application {2}", defaultHost, displayHost, appName);

        options.put("high_security", getAndLogHighSecurity(agentConfig));

        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        options.put("environment", environment);
        options.put("settings", getSettings(agentConfig.getProperty("send_environment_info", true)));

        UtilizationData utilizationData = ServiceFactory.getUtilizationService().updateUtilizationData();
        options.put("utilization", utilizationData.map());

        options.put(AgentConfigFactory.EVENT_HARVEST_CONFIG, ServiceFactory.getHarvestService().getEventDataHarvestLimits());

        String instanceName = environment.getAgentIdentity().getInstanceName();
        if (instanceName != null) {
            options.put("instance_name", instanceName);
        }

        // options.put("framework", "java"); // this belongs in the environment
        // options.put("launch_time",
        // JSON.serializeNumber(TimeConversion.convertMillisToSeconds(System.currentTimeMillis())));
        options.put("agent_version", Agent.getVersion());
        options.put("app_name", appNames);

        StringBuilder identifier = new StringBuilder(language);
        identifier.append(':').append(appName);
        Integer serverPort = environment.getAgentIdentity().getServerPort();
        if (serverPort != null) {
            identifier.append(':').append(serverPort);
        }
        options.put("identifier", identifier.toString());
        options.put("labels", agentConfig.getLabelsConfig());

        return options;
    }

    private Map<String, Object> getSettings(boolean sendEnvironmentInfo) {
        Map<String, Object> settings = new HashMap<>();
        if (sendEnvironmentInfo) {
            Map<String, Object> localSettings = ServiceFactory.getConfigService().getSanitizedLocalSettings();
            Map<String, Object> systemProperties = SystemPropertyFactory.getSystemPropertyProvider().getNewRelicPropertiesWithoutPrefix();
            Map<String, Object> envVars = SystemPropertyFactory.getSystemPropertyProvider().getNewRelicEnvVarsWithoutPrefix();

            settings.putAll(localSettings);

            if (!systemProperties.isEmpty()) {
                settings.put("system", systemProperties);
                for (Map.Entry<String, Object> entry : systemProperties.entrySet()) {
                    AgentConfigFactory.addSimpleMappedProperty(entry.getKey(), entry.getValue(), settings);
                }
            }

            if (!envVars.isEmpty()) {
                settings.put("environment", envVars);
            }

            settings.putAll(envVars);
            // add the RUM configuration values
            BrowserMonitoringConfig browserConfig = ServiceFactory.getConfigService().getAgentConfig(appName).getBrowserMonitoringConfig();
            settings.put(AgentConfigImpl.BROWSER_MONITORING + "." + BrowserMonitoringConfigImpl.LOADER_TYPE, browserConfig.getLoaderType());
            settings.put(AgentConfigImpl.BROWSER_MONITORING + "." + BrowserMonitoringConfigImpl.DEBUG, browserConfig.isDebug());
        }

        // Always send the services configuration as RPM thread profiler depends on it.
        String buildDate = AgentJarHelper.getBuildDate();
        if (buildDate != null) {
            settings.put("build_date", buildDate);
        }
        settings.put("services", ServiceFactory.getServicesConfiguration());

        return settings;
    }

    /**
     * Notify RPM that this agent has launched, and obtain the agent run id
     */
    @Override
    public synchronized void launch() throws Exception {
        if (isConnected()) {
            return;
        }

        Map<String, Object> data = doConnect();
        Agent.LOG.log(Level.FINER, "Connection response : {0}", data);
        List<String> requiredParams = new ArrayList<>(Arrays.asList(COLLECT_ERRORS_KEY, COLLECT_TRACES_KEY, DATA_REPORT_PERIOD_KEY));
        if (!data.keySet().containsAll(requiredParams) && !serverlessMode) {
            requiredParams.removeAll(data.keySet());
            throw new UnexpectedException(MessageFormat.format("Missing the following connection parameters: {0}", requiredParams));
        }
        Agent.LOG.log(Level.INFO, "Agent {0} connected to {1}", toString(), getHostString());

        try {
            logCollectorMessages(data);
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINEST, ex, "Error processing collector connect messages");
        }

        AgentConfig config = null;
        if (connectionConfigListener != null && serverlessMode == false) {
            // Merge server-side data with local config before notifying connection listeners
            config = connectionConfigListener.connected(this, data);
        }

        connectionTimestamp = System.nanoTime();
        connected = true;
        hasEverConnected = true;
        entityGuid = data.get("entity_guid") != null ? data.get("entity_guid").toString() : "";

        if (connectionListener != null) {
            config = config != null ? config : ServiceFactory.getConfigService().getDefaultAgentConfig();
            connectionListener.connected(this, config);
        }
        String agentRunToken = serverlessMode ? "serverless-run-token" : (String) data.get(ConnectionResponse.AGENT_RUN_ID_KEY);
        Map<String, String> requestMetadata = (Map<String, String>) data.get(ConnectionResponse.REQUEST_HEADERS);
        for (AgentConnectionEstablishedListener listener : agentConnectionEstablishedListeners) {
            listener.onEstablished(appName, agentRunToken, requestMetadata);
        }
    }

    private Map<String, Object> doConnect() throws Exception {
        try {
            return dataSender.connect(getStartOptions());
        } catch (LicenseException e) {
            logLicenseException(e);
            if (!((RPMConnectionServiceImpl) ServiceFactory.getRPMConnectionService()).shouldPreventNewConnectionTask()){
                reconnect();
            }
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void logCollectorMessages(Map<String, Object> data) {
        if (serverlessMode) {
            return;
        }
        List<Map<String, String>> messages = (List<Map<String, String>>) data.get("messages");
        if (messages != null) {
            for (Map<String, String> message : messages) {
                String level = message.get("level");
                String text = message.get("message");

                if (text.startsWith("Reporting to")) {
                    rpmLink = text.substring(14);
                }

                Agent.LOG.log(Level.parse(level), text);
            }
        }
    }

    @Override
    public String getEntityGuid() {
        return entityGuid;
    }

    public String getApplicationLink() {
        return rpmLink;
    }

    @Override
    public TransactionNamingScheme getTransactionNamingScheme() {
        AgentConfig appConfig = ServiceFactory.getConfigService().getAgentConfig(getApplicationName());
        return appConfig.getTransactionNamingScheme();
    }

    private void logForceDisconnectException(ForceDisconnectException e) {
        Agent.LOG.log(Level.SEVERE, "Received a ForceDisconnectException: {0}. The agent is no longer reporting"
                + " information. If this is not a misconfiguration, please contact support via https://support.newrelic.com/.", e.toString());
    }

    private void logLicenseException(LicenseException e) {
        Agent.LOG.log(Level.SEVERE, "Invalid license key, the agent is no longer reporting"
                + " information. If this is not a misconfiguration, please contact support via https://support.newrelic.com/.", e.toString());
    }

    private void shutdownAsync() {
        ServiceFactory.getCoreService().shutdownAsync();
    }

    private void logForceRestartException(ForceRestartException e) {
        Agent.LOG.log(Level.WARNING, "Received a ForceRestartException: {0}. The agent will attempt to reconnect for"
                + " data reporting. If this message continues, please contact support via https://support.newrelic.com/.", e.toString());
    }

    private void reconnectSync() throws Exception {
        disconnect();
        launch();
    }

    private void reconnectAsync() {
        disconnect();
        ServiceFactory.getRPMConnectionService().connectImmediate(this);
    }

    private void disconnect() {
        if (isConnected()) {
            try {
                dataSender.shutdown(System.currentTimeMillis());
            } catch (Exception e) {
                Agent.LOG.log(Level.FINER, e, "{0} is unable to notify shutdown", getApplicationName());
            }
        }
        connected = false;
    }

    @Override
    public synchronized void reconnect() {
        Agent.LOG.log(Level.INFO, "{0} is reconnecting", getApplicationName());
        try {
            shutdown();
        } catch (Exception e) {
            // ignore
        } finally {
            reconnectAsync();
        }
    }

    @Override
    public String getHostString() {
        if (serverlessMode) {
            return "serverless";
        }
        return MessageFormat.format("{0}:{1}", host, Integer.toString(port));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(ManagementFactory.getRuntimeMXBean().getName());
        builder.append('/').append(appName);
        return builder.toString();
    }

    @Override
    public void sendErrorData(List<TracedError> errors) {
        Agent.LOG.log(Level.FINE, "Sending {0} error(s)", errors.size());
        try {
            try {
                dataSender.sendErrorData(errors);
            } catch (IgnoreSilentlyException e) {
                // ignore
            } catch (ForceRestartException e) {
                logForceRestartException(e);
                reconnectAsync();
            } catch (ForceDisconnectException e) {
                logForceDisconnectException(e);
                shutdownAsync();
            } catch (HttpError e) {
                // In case of 413 status code, cut the size of the payload in half and try again
                if (e.isRequestPayloadTooLarge()) {
                    // This will halve the errors payload. If the payload only has 1 item left it will be cut to 0
                    sendErrorData(new ArrayList<>(errors.subList(0, errors.size() / 2)));
                } else {
                    throw e; // Otherwise re-throw the error so it can be logged
                }
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Error sending error data to New Relic: {0}", e);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, e);
            } else {
                Agent.LOG.warning(msg);
            }
        }
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        Agent.LOG.log(Level.INFO, "Sending {0} profile(s)", profiles.size());
        try {
            return sendProfileDataSyncRestart(profiles);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private List<Long> sendProfileDataSyncRestart(List<ProfileData> profiles) throws Exception {
        try {
            return dataSender.sendProfileData(profiles);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            return dataSender.sendProfileData(profiles);
        }
    }

    @Override
    public void sendModules(final List<JarData> jarDataList) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} module(s)", jarDataList.size());
        try {
            sendModulesSyncRestart(jarDataList);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendModulesSyncRestart(final List<JarData> jarDataList) throws Exception {
        try {
            dataSender.sendModules(jarDataList);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendModules(jarDataList);
        }
    }

    @Override
    public void sendAnalyticsEvents(int reservoirSize, int eventsSeen, final Collection<TransactionEvent> events) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} analytics event(s)", events.size());
        try {
            sendAnalyticsEventsSyncRestart(reservoirSize, eventsSeen, events);
        } catch (HttpError e) {
            // We don't want to resend the data for certain response codes, retry for all others
            if (e.isRetryableError()) {
                throw e;
            }
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEventsSyncRestart(int reservoirSize, int eventsSeen, final Collection<T> events)
            throws Exception {
        try {
            dataSender.sendAnalyticsEvents(reservoirSize, eventsSeen, events);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendAnalyticsEvents(reservoirSize, eventsSeen, events);
        }
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, final Collection<? extends CustomInsightsEvent> events) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} analytics event(s)", events.size());
        try {
            sendCustomAnalyticsEventsSyncRestart(reservoirSize, eventsSeen, events);
        } catch (HttpError e) {
            // We don't want to resend the data for certain response codes, retry for all others
            if (e.isRetryableError()) {
                throw e;
            }
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    @Override
    public void sendLogEvents(final Collection<? extends LogEvent> events) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} log event(s)", events.size());
        try {
            sendLogEventsSyncRestart(events);
        } catch (HttpError e) {
            // We don't want to resend the data for certain response codes, retry for all others
            if (e.isRetryableError()) {
                throw e;
            }
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendSpanEventsSyncRestart(int reservoirSize, int eventsSeen, final Collection<SpanEvent> events) throws Exception {
        try {
            dataSender.sendSpanEvents(reservoirSize, eventsSeen, events);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendSpanEvents(reservoirSize, eventsSeen, events);
        }
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, final Collection<SpanEvent> events) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} span event(s)", events.size());
        try {
            sendSpanEventsSyncRestart(reservoirSize, eventsSeen, events);
        } catch (HttpError e) {
            // We don't want to resend the data for certain response codes, retry for all others
            if (e.isRetryableError()) {
                throw e;
            }
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendCustomAnalyticsEventsSyncRestart(int reservoirSize, int eventsSeen, final Collection<? extends CustomInsightsEvent> events)
            throws Exception {
        try {
            dataSender.sendCustomAnalyticsEvents(reservoirSize, eventsSeen, events);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendCustomAnalyticsEvents(reservoirSize, eventsSeen, events);
        }
    }

    private void sendLogEventsSyncRestart(final Collection<? extends LogEvent> events)
            throws Exception {
        try {
            dataSender.sendLogEvents(events);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendLogEvents(events);
        }
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, final Collection<ErrorEvent> events) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} error event(s)", events.size());
        try {
            sendErrorEventsSyncRestart(reservoirSize, eventsSeen, events);
        } catch (HttpError e) {
            // We don't want to resend the data for certain response codes, retry for all others
            if (e.isRetryableError()) {
                throw e;
            }
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendErrorEventsSyncRestart(int reservoirSize, int eventsSeen, final Collection<ErrorEvent> events) throws Exception {
        try {
            dataSender.sendErrorEvents(reservoirSize, eventsSeen, events);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendErrorEvents(reservoirSize, eventsSeen, events);
        }
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} sql trace(s)", sqlTraces.size());
        try {
            sendSqlTraceDataSyncRestart(sqlTraces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendSqlTraceDataSyncRestart(List<SqlTrace> sqlTraces) throws Exception {
        try {
            dataSender.sendSqlTraceData(sqlTraces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendSqlTraceData(sqlTraces);
        }
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} trace(s)", traces.size());
        try {
            sendTransactionTraceDataSyncRestart(traces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendTransactionTraceDataSyncRestart(List<TransactionTrace> traces) throws Exception {
        try {
            dataSender.sendTransactionTraceData(traces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendTransactionTraceData(traces);
        }
    }

    @Override
    public ErrorService getErrorService() {
        return errorService;
    }

    /**
     * Returns the name of the application the rpm service is reporting data for.
     *
     * @return the name of the application (this is interned)
     */
    @Override
    public String getApplicationName() {
        return appName;
    }

    /**
     * Is this the main application?
     *
     * @return <tt>true</tt> if this is the main application
     */
    @Override
    public boolean isMainApp() {
        return isMainApp;
    }

    /**
     * notify RPM that the agent is shutting down
     */
    public synchronized void shutdown() throws Exception {
        disconnect();
    }

    @Override
    public void harvestNow() {

        // JAVA-2965. We are configured to send data on exit and the JVM is shutting down. We may be racing with a
        // reconnect. Up to a time limit, repeatedly take the lock and attempt to harvest. Since the shutdown part of
        // reconnect is synchronous, merely holding the lock on this object prevents the data sender from shutting down.
        // The connect part of reconnect is asynchronous; so if we are not connected, we have to keep dropping the lock
        // and (re-)checking to see if reconnect has completed.
        //
        // Note: even if we are not connected, we don't need to initiate a connection attempt - although there are
        // several cases, bottom line is that the Agent should already be attempting to connect.

        final int MAX_WAIT_SECONDS = 10;
        final long end = System.currentTimeMillis() + MAX_WAIT_SECONDS * 1000L;
        boolean done = false;
        Throwable trouble = null;

        while (!done && System.currentTimeMillis() < end) {
            try {
                synchronized (this) {
                    if (isConnected()) {
                        ServiceFactory.getHarvestService().harvestNow();
                        done = true;
                    }
                }
                Thread.sleep(200);
            } catch (InterruptedException iex) {
                // sleep returned early - ignore it - the process is ending anyway
            } catch (Exception ex) {
                trouble = ex;
            }
        }

        if (trouble != null) {
            Agent.LOG.log(Level.INFO, "Unable to send data to New Relic during JVM shutdown: {0}: {1}",
                    trouble.getClass().getSimpleName(), trouble.getLocalizedMessage());
        } else if (!done) {
            Agent.LOG.log(Level.INFO, "Unable to send data to New Relic during JVM shutdown: "
                    + "the Agent was unable to connect within the {0} second time limit.", MAX_WAIT_SECONDS);
        }
    }

    @Override
    public List<List<?>> getAgentCommands() throws Exception {
        try {
            return getAgentCommandsSyncRestart();
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private List<List<?>> getAgentCommandsSyncRestart() throws Exception {
        try {
            return dataSender.getAgentCommands();
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            return dataSender.getAgentCommands();
        }
    }

    @Override
    public void sendCommandResults(Map<Long, Object> commandResults) throws Exception {
        try {
            sendCommandResultsSyncRestart(commandResults);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendCommandResultsSyncRestart(Map<Long, Object> commandResults) throws Exception {
        try {
            dataSender.sendCommandResults(commandResults);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendCommandResults(commandResults);
        }
    }

    /**
     * This thing ultimately calls {@link #launch()} after about 50 billion points of indirection.
     */
    public void connect() {
        ServiceFactory.getRPMConnectionService().connect(this);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean hasEverConnected() {
        return hasEverConnected;
    }

    @Override
    public void harvest(StatsEngine statsEngine) {
        if (!isConnected()) {
            // we had a connection/run id once and reconnecting failed.
            // Assumption: failed re-connect should be a result of a temporary condition, and we need to retry
            try {
                if (serverlessMode) {
                    Agent.LOG.log(Level.FINE, "Trying to re-establish connection for serverless mode.");
                }
                else {
                    Agent.LOG.fine("Trying to re-establish connection to New Relic.");
                }
                this.launch();
            } catch (Exception e) {
                Agent.LOG.fine("Problem trying to re-establish connection to New Relic: " + e.getMessage());
            }
        }

        if (isConnected()) {
            boolean retry = false;

            Normalizer metricNormalizer = ServiceFactory.getNormalizationService().getMetricNormalizer(appName);
            List<MetricData> data = statsEngine.getMetricData(metricNormalizer);

            long startTime = System.nanoTime();
            long reportInterval = 0;
            try {
                long now = System.currentTimeMillis();
                sendMetricDataSyncRestart(lastReportTime, now, data);
                dataSender.commitAndFlush();
                reportInterval = now - lastReportTime;
                lastReportTime = now;
                last503Error.set(0);

                // log at info level whenever we successfully send metric data following a failure
                if (retryCount.get() > 0) {
                    Agent.LOG.log(Level.INFO, "Successfully reconnected to the New Relic data service.");
                }
                Agent.LOG.log(Level.FINE, "Reported {0} timeslices for {1}", data.size(), getApplicationName());
            } catch (InternalLimitExceeded e) {
                Agent.LOG.log(Level.SEVERE, "The metric data post was too large. {0} timeslices will not be resent", data.size());
            } catch (MetricDataException e) {
                Agent.LOG.log(Level.SEVERE, "An invalid response was received while sending metric data. This data will not be resent.");
                Agent.LOG.log(Level.FINEST, e, e.toString());
            } catch (HttpError e) {
                // HttpError handled here
                retry = e.isRetryableError();

                if (HttpResponseCode.SERVICE_UNAVAILABLE == e.getStatusCode()) {
                    // 503s are unfortunately common for the collector, so we log these at a lower level until we see
                    // many consecutive failures
                    handle503Error(e);
                } else if (retry) {
                    // otherwise if we're going to retry later things aren't so bad
                    Agent.LOG.log(Level.INFO, "An error occurred posting metric data - {0}. This data will be resent later.", e.getMessage());
                } else {
                    // but let's call out when we're dropping data. Check out HttpError.isRetryableError()
                    Agent.LOG.log(Level.SEVERE, "An error occurred posting metric data - {0}. {1} timeslices will not be resent.", e.getMessage(), data.size());
                }
            } catch (ForceRestartException e) {
                logForceRestartException(e);
                reconnectAsync();
                retry = true;
            } catch (ForceDisconnectException e) {
                logForceDisconnectException(e);
                shutdownAsync();
            } catch (HostConnectException e) {
                retry = true;
                Agent.LOG.log(Level.INFO, "A connection error occurred contacting {0}. Please check your network / proxy settings.", e.getHostName());
                Agent.LOG.log(Level.FINEST, e, e.toString());
            } catch (Exception e) {
                // LicenseException handled here
                logMetricDataError(e);
                retry = true;
                if (e.getMessage() != null) {
                    String message = e.getMessage().toLowerCase();
                    // if our data can't be parsed, we probably have a bad metric
                    // (web transaction maybe?). clear out the metrics
                    if (message.contains("json") && message.contains("parse")) {
                        retry = false;
                    }
                }
            }
            long duration = System.nanoTime() - startTime;
            if (retry) {
                retryCount.getAndIncrement();
            } else {
                retryCount.set(0);
                statsEngine.clear();
                recordSupportabilityMetrics(statsEngine, reportInterval, duration, data.size());
            }
        }
    }

    private void recordSupportabilityMetrics(StatsEngine statsEngine, long reportInterval, long duration, int dataSize) {
        if (reportInterval > 0) {
            statsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_METRIC_HARVEST_INTERVAL)
                    .recordResponseTime(reportInterval, TimeUnit.MILLISECONDS);
        }
        statsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_METRIC_HARVEST_TRANSMIT)
                .recordResponseTime(duration, TimeUnit.NANOSECONDS);
        statsEngine.getStats(MetricNames.SUPPORTABILITY_METRIC_HARVEST_COUNT).incrementCallCount(dataSize);
    }

    private void sendMetricDataSyncRestart(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception {
        try {
            dataSender.sendMetricData(beginTimeMillis, endTimeMillis, metricData);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendMetricData(beginTimeMillis, endTimeMillis, metricData);
        }
    }

    private void logMetricDataError(Exception e) {
        Agent.LOG.log(Level.INFO, "An unexpected error occurred sending metric data to New Relic."
                + " Please file a support ticket once you have seen several of these messages in a short period of time: {0}", e.toString());
        Agent.LOG.log(Level.FINEST, e, e.toString());
    }

    /**
     * Support has requested that this message only be printed out after it has occurred a few times as intermittent
     * failures with the collector can occur.
     *
     * @param e The 503 exception.
     */
    private void handle503Error(Exception e) {
        String msg = "A 503 (Unavailable) response was received while sending metric data to New Relic."
                + " The agent will continue to aggregate data and report it in the next time period.";

        if (last503Error.getAndIncrement() == LOG_MESSAGE_COUNT) {
            Agent.LOG.info(msg);
            Agent.LOG.log(Level.FINEST, e, e.toString());
            // at this point, we've logged the message once, we don't need to log it again.
            // we'll reset the last503Error after the next successful data post.
        } else {
            // print at finest until we reach a certain count
            Agent.LOG.log(Level.FINER, msg, e);
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return ((Double)Double.parseDouble((String)o)).intValue();
    }

    @Override
    protected void doStop() {
        removeHarvestablesFromServices(appName);
        try {
            errorService.stop();
            shutdown();
        } catch (Exception e) {
            Level level = e instanceof ConnectException ? Level.FINER : Level.SEVERE;
            Agent.LOG.log(level, "An error occurred in the NewRelic agent shutdown", e);
        }
        ServiceFactory.getEnvironmentService().getEnvironment().removeEnvironmentChangeListener(this);
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
        ServiceFactory.getServiceManager().getCircuitBreakerService().removeRPMService(this);
    }

    private void removeHarvestablesFromServices(String appName) {
        ServiceFactory.getHarvestService().removeHarvestablesByAppName(appName);
    }

    @Override
    public HealthDataProducer getHttpDataSenderAsHealthDataProducer() {
        return (HealthDataProducer) dataSender;
    }

    @Override
    public long getConnectionTimestamp() {
        return connectionTimestamp;
    }

    @Override
    public void agentIdentityChanged(AgentIdentity agentIdentity) {
        if (connected) {
            logger.log(Level.FINE, "Reconnecting after an environment change");
            reconnect();
        }
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        // reset our error logging so that something will show up at info level if data failures persist
        last503Error.set(0);
    }
}
