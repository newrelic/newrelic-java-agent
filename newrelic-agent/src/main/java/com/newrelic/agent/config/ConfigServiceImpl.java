/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.ConnectionConfigListener;
import com.newrelic.agent.DebugFlag;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.agent.config.internal.DeepMapClone;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.api.agent.NewRelic;
import org.json.simple.JSONObject;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class ConfigServiceImpl extends AbstractService implements ConfigService, ConnectionConfigListener, HarvestListener {
    private static final String SANITIZED_SETTING = "****";
    private static final Map<String, String> policiesToConfigs;

    private final List<AgentConfigListener> listeners = new CopyOnWriteArrayList<>();
    private final File configFile;
    private final String defaultAppName;
    private final ConcurrentMap<String, AgentConfig> agentConfigs = new ConcurrentHashMap<>();
    private final boolean checkConfig;
    private volatile long lastModified;
    private volatile AgentConfig defaultAgentConfig;
    private volatile AgentConfig localAgentConfig;
    private volatile Map<String, Object> savedServerData;
    private volatile Map<String, Boolean> laspPolicies;
    private volatile Map<String, Object> fileSettings;

    static {
        policiesToConfigs = ImmutableMap.<String, String>builder()
                .put(LaspPolicies.LASP_RECORD_SQL, "transaction_tracer.record_sql")
                .put(LaspPolicies.LASP_ATTRIBUTES_INCLUDE, "attributes.include")
                .put(LaspPolicies.LASP_ALLOW_RAW_EXCEPTION_MESSAGES, "strip_exception_messages.enabled")
                .put(LaspPolicies.LASP_CUSTOM_EVENTS, "custom_insights_events.enabled")
                .put(LaspPolicies.LASP_CUSTOM_PARAMETERS, LaspPolicies.LASP_CUSTOM_PARAMETERS)
                .put(LaspPolicies.LASP_CUSTOM_INSTRUMENTATION_EDITOR, LaspPolicies.LASP_CUSTOM_INSTRUMENTATION_EDITOR)
                .put(LaspPolicies.LASP_MESSAGE_PARAMETERS, LaspPolicies.LASP_MESSAGE_PARAMETERS)
                .build();
    }

    protected ConfigServiceImpl(AgentConfig agentConfig, File configFile, Map<String, Object> fileSettings, boolean checkConfig) {
        super(ConfigService.class.getSimpleName());
        this.configFile = configFile;
        this.fileSettings = fileSettings == null ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(fileSettings);
        localAgentConfig = agentConfig;
        this.checkConfig = checkConfig;
        defaultAgentConfig = localAgentConfig;
        defaultAppName = defaultAgentConfig.getApplicationName();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() {
        ServiceFactory.getRPMServiceManager().setConnectionConfigListener(this);

        if (configFile != null) {
            lastModified = configFile.lastModified();
            String msg = MessageFormat.format("Configuration file is {0}", configFile.getAbsolutePath());
            getLogger().info(msg);
        }

        Object apdex_t = localAgentConfig.getProperty("apdex_t", null);
        if (apdex_t != null) {
            String msg = "The apdex_t setting is obsolete and is ignored! Set the apdex_t value for an application in New Relic UI";
            getLogger().warning(msg);
        }

        Object wait_for_customer_ssl = localAgentConfig.getProperty("wait_for_customer_ssl", null);
        if (wait_for_customer_ssl != null) {
            String msg = "The wait_for_customer_ssl setting is obsolete and is ignored!";
            getLogger().warning(msg);
        }

        boolean isCrossApplicationTracing = localAgentConfig.getCrossProcessConfig().isCrossApplicationTracing();
        if (isCrossApplicationTracing) {
            String msg = "Cross application tracing (CAT) is deprecated and will be removed in a future agent release. Distributed tracing has replaced CAT " +
                    "as the default means of tracing between services. To continue using CAT, re-enable it with " +
                    "cross_application_tracer.enabled=true and distributed_tracing.enabled=false. ";
            getLogger().info(msg);
        }

        localAgentConfig.logDeprecatedProperties(fileSettings);

        ServiceFactory.getHarvestService().addHarvestListener(this);

        updateDebugFlag(localAgentConfig);
    }

    @Override
    protected void doStop() {
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public void addIAgentConfigListener(AgentConfigListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeIAgentConfigListener(AgentConfigListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Map<String, Object> getSanitizedLocalSettings() {
        Map<String, Object> settings = DeepMapClone.deepCopy(fileSettings);
        if (settings.containsKey(AgentConfigImpl.PROXY_HOST)) {
            settings.put(AgentConfigImpl.PROXY_HOST, SANITIZED_SETTING);
        }
        if (settings.containsKey(AgentConfigImpl.PROXY_USER)) {
            settings.put(AgentConfigImpl.PROXY_USER, SANITIZED_SETTING);
        }
        if (settings.containsKey(AgentConfigImpl.PROXY_PASS)) {
            settings.put(AgentConfigImpl.PROXY_PASS, SANITIZED_SETTING);
        }
        //if there is no adaptive_sampling_target set, set it here.
        addAdaptiveSamplerDefaultIfNotSet(settings);
        return settings;
    }

    @Override
    public AgentConfig getDefaultAgentConfig() {
        return defaultAgentConfig;
    }

    @Override
    public AgentConfig getLocalAgentConfig() {
        return localAgentConfig;
    }

    @Override
    public AgentConfig getAgentConfig(String appName) {
        return getOrCreateAgentConfig(appName);
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig(String appName) {
        return getOrCreateAgentConfig(appName).getTransactionTracerConfig();
    }

    @Override
    public ErrorCollectorConfig getErrorCollectorConfig(String appName) {
        return getOrCreateAgentConfig(appName).getErrorCollectorConfig();
    }

    @Override
    public StripExceptionConfig getStripExceptionConfig(String appName) {
        return getOrCreateAgentConfig(appName).getStripExceptionConfig();
    }

    @Override
    public void setLaspPolicies(Map<String, Boolean> policiesJson) {
        laspPolicies = convertToAgentConfig(policiesJson);
    }

    @Override
    public DistributedTracingConfig getDistributedTracingConfig(String appName) {
        return getOrCreateAgentConfig(appName).getDistributedTracingConfig();
    }

    @Override
    public ExtensionsConfig getExtensionsConfig(String appName) {
        return getOrCreateAgentConfig(appName).getExtensionsConfig();
    }

    private void checkConfigFile() throws Exception {
        if (configFile == null || configFile.lastModified() == lastModified) {
            return;
        }

        Agent.LOG.info("Re-reading New Relic configuration file");
        lastModified = configFile.lastModified();

        // Read updated configuration file into memory
        Map<String, Object> fileSettings = new HashMap<>(AgentConfigHelper.getConfigurationFileSettings(configFile));
        if (checkConfig) {
            Agent.LOG.log(Level.INFO, "YAML parse results: {0}", JSONObject.toJSONString(fileSettings));
        }

        this.fileSettings = Collections.unmodifiableMap(DeepMapClone.deepCopy(fileSettings));

        // Update local agent configuration and modify audit_mode + log_level here. This uses the last known serverData
        // that we received in order to ensure that no serverData specific config is lost on config reload.
        localAgentConfig = AgentConfigFactory.createAgentConfig(fileSettings, savedServerData, laspPolicies);

        updateDynamicAuditAndLogConfig(localAgentConfig, fileSettings);

        // This ensures that all listeners are notified and that configuration is updated across the app
        replaceServerConfig(defaultAppName, fileSettings, savedServerData, laspPolicies);
    }

    private void addAdaptiveSamplerDefaultIfNotSet(Map<String, Object> settings) {
        try {
            settings.putIfAbsent("distributed_tracing", new HashMap<>());
            Map<String, Object> dtSettings = (Map<String, Object>) settings.get("distributed_tracing");
            dtSettings.putIfAbsent("sampler", new HashMap<>());
            Map<String, Object> samplerSettings = (Map<String, Object>) dtSettings.get("sampler");
            samplerSettings.putIfAbsent(SamplerConfig.ADAPTIVE_SAMPLING_TARGET, SamplerConfig.DEFAULT_ADAPTIVE_SAMPLING_TARGET);
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.WARNING, "Error adding default adaptive sampling target settings to agent config.");
        }
    }

    private void updateDynamicAuditAndLogConfig(AgentConfig localAgentConfig, Map<String, Object> settings) {
        AuditModeConfig auditModeConfig = localAgentConfig.getAuditModeConfig();
        if (auditModeConfig != null) {
            Set<String> auditModeEndpoints = auditModeConfig.getEndpoints();
            if (auditModeEndpoints == null) {
                // Legacy "audit_mode: true/false"
                settings.put(AuditModeConfig.PROPERTY_NAME, localAgentConfig.isAuditMode());
            } else {
                // New, nested "audit_mode:"
                Map<String, Object> auditModeData = new HashMap<>();
                auditModeData.put(AuditModeConfig.ENABLED, localAgentConfig.getAuditModeConfig().isEnabled());
                auditModeData.put(AuditModeConfig.ENDPOINTS, localAgentConfig.getAuditModeConfig().getEndpoints());
                settings.put(AuditModeConfig.PROPERTY_NAME, auditModeData);
            }
        }

        settings.put(AgentConfigImpl.LOG_LEVEL, localAgentConfig.getLogLevel());

        updateDebugFlag(localAgentConfig);
        AgentLogManager.setLogLevel(localAgentConfig.getLogLevel());
    }

    private void updateDebugFlag(AgentConfig localAgentConfig) {
        DebugFlag.tokenEnabled.set(localAgentConfig.isDebugEnabled("token"));
    }

    private void notifyListeners(String appName, AgentConfig agentConfig) {
        for (AgentConfigListener listener : listeners) {
            listener.configChanged(appName, agentConfig);
        }
    }

    private AgentConfig getOrCreateAgentConfig(String appName) {
        AgentConfig agentConfig = findAgentConfig(appName);
        if (agentConfig != null) {
            return agentConfig;
        }
        agentConfig = AgentConfigFactory.createAgentConfig(fileSettings, null, laspPolicies);
        AgentConfig oldAgentConfig = agentConfigs.putIfAbsent(appName, agentConfig);
        return oldAgentConfig == null ? agentConfig : oldAgentConfig;
    }

    private AgentConfig findAgentConfig(String appName) {
        if (appName == null || appName.equals(defaultAppName)) {
            return defaultAgentConfig;
        }
        return agentConfigs.get(appName);
    }

    private AgentConfig createAgentConfig(String appName, Map<String, Object> localSettings,
            Map<String, Object> serverData, Map<String, Boolean> laspData) {
        try {
            return AgentConfigFactory.createAgentConfig(localSettings, serverData, laspData);
        } catch (Exception e) {
            String msg = MessageFormat.format("Error configuring application \"{0}\" with server data \"{1}\": {2}", appName, serverData, e);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, e);
            } else {
                Agent.LOG.warning(msg);
            }
        }
        return null;
    }

    private AgentConfig replaceServerConfig(String appName, Map<String, Object> localSettings, Map<String, Object> serverData, Map<String, Boolean> laspData) {
        if (Agent.LOG.isLoggable(Level.FINER)) {
            if (laspData == null || laspData.isEmpty()) {
                Agent.LOG.finer(MessageFormat.format("Received New Relic data for {0}:  server data {1}, lasp data {2}", appName, serverData, laspData));
            } else {
                Agent.LOG.finer(MessageFormat.format("Received New Relic data for {0}:  server data {1}", appName, serverData));
            }
        }

        AgentConfig agentConfig = createAgentConfig(appName, localSettings, serverData, laspData);
        if (agentConfig == null) {
            return null;
        }

        if (appName == null || appName.equals(defaultAppName)) {
            defaultAgentConfig = agentConfig;
        } else {
            agentConfigs.put(appName, agentConfig);
        }

        logIfHighSecurityServerAndLocal(appName, agentConfig, serverData);
        notifyListeners(appName, agentConfig);

        return agentConfig;
    }

    private void logIfHighSecurityServerAndLocal(String appName, AgentConfig agentConfig, Map<String, Object> serverData) {
        if (agentConfig.isHighSecurity() && serverData.containsKey(AgentConfigFactory.HIGH_SECURITY)) {
            String msg = MessageFormat.format("The agent is in high security mode for {0}: {1} setting is \"{2}\"."
                            + " Disabling the collection of request parameters, message queue parameters, and user attributes.", appName,
                    AgentConfigFactory.RECORD_SQL, agentConfig.getTransactionTracerConfig().getRecordSql());
            Agent.LOG.info(msg);
        }
    }

    @Override
    public AgentConfig connected(IRPMService rpmService, Map<String, Object> serverData) {
        String appName = rpmService.getApplicationName();

        // Store off server data for later use if configuration is updated dynamically
        savedServerData = new HashMap<>(serverData);

        return replaceServerConfig(appName, fileSettings, serverData, laspPolicies);
    }

    public static Map<String, Boolean> convertToAgentConfig(Map<String, Boolean> laspPolicies) {
        if (laspPolicies == null) {
            return null;
        }

        Map<String, Boolean> agentFormat = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : laspPolicies.entrySet()) {
            String configName = policiesToConfigs.get(entry.getKey());
            if (configName != null) {
                if (entry.getKey().equals("allow_raw_exception_messages")) {
                    agentFormat.put(configName, !entry.getValue());
                } else {
                    agentFormat.put(configName, entry.getValue());
                }
            }
        }

        return agentFormat;
    }

    @Override
    public void afterHarvest(String appName) {
        if (!appName.equals(defaultAppName)) {
            return;
        }
        try {
            checkConfigFile();
        } catch (Throwable t) {
            String msg = MessageFormat.format("Unexpected exception checking for config file changes: {0}", t.toString());
            getLogger().warning(msg);
        }
        ServiceFactory.getClassTransformerService().checkShutdown();
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }

}
