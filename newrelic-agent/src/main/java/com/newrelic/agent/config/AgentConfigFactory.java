/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils;
import com.newrelic.agent.browser.BrowserConfig;
import com.newrelic.agent.config.internal.DeepMapClone;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.reinstrument.RemoteInstrumentationServiceImpl;
import com.newrelic.agent.transport.CollectorMethods;
import com.newrelic.agent.transport.ConnectionResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.newrelic.agent.config.SpanEventsConfig.SERVER_SPAN_HARVEST_CONFIG;

public class AgentConfigFactory {

    public static final String AGENT_CONFIG = "agent_config";
    public static final String PERIOD_REGEX = "\\.";
    public static final String DOT_SEPARATOR = ".";
    public static final String SLOW_SQL_PREFIX = AgentConfigImpl.SLOW_SQL + DOT_SEPARATOR;
    public static final String TRANSACTION_TRACER_PREFIX = AgentConfigImpl.TRANSACTION_TRACER + DOT_SEPARATOR;
    public static final String TRANSACTION_TRACER_CATEGORY_BACKGROUND_PREFIX = AgentConfigImpl.TRANSACTION_TRACER + ".category.background.";
    public static final String TRANSACTION_TRACER_CATEGORY_REQUEST_PREFIX = AgentConfigImpl.TRANSACTION_TRACER + ".category.request.";
    public static final String ERROR_COLLECTOR_PREFIX = AgentConfigImpl.ERROR_COLLECTOR + DOT_SEPARATOR;
    public static final String THREAD_PROFILER_PREFIX = AgentConfigImpl.THREAD_PROFILER + DOT_SEPARATOR;
    public static final String TRANSACTION_EVENTS_PREFIX = AgentConfigImpl.TRANSACTION_EVENTS + DOT_SEPARATOR;
    public static final String CUSTOM_INSIGHT_EVENTS_PREFIX = AgentConfigImpl.CUSTOM_INSIGHT_EVENTS + DOT_SEPARATOR;
    public static final String SPAN_EVENTS_PREFIX = AgentConfigImpl.SPAN_EVENTS + DOT_SEPARATOR;
    public static final String BROWSER_MONITORING_PREFIX = AgentConfigImpl.BROWSER_MONITORING + DOT_SEPARATOR;
    public static final String HIGH_SECURITY = "high_security";
    public static final String SECURITY_POLICIES_TOKEN = "security_policies_token";
    public static final String COLLECT_ERRORS = ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.COLLECT_ERRORS;
    public static final String EXPECTED_CLASSES = ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.EXPECTED_CLASSES;
    public static final String EXPECTED_STATUS_CODES = ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES;
    public static final String COLLECT_ERROR_EVENTS = ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.COLLECT_EVENTS;
    public static final String CAPTURE_ERROR_EVENTS = ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.CAPTURE_EVENTS;
    public static final String CUSTOM_INSIGHTS_ENABLED = AgentConfigImpl.CUSTOM_INSIGHT_EVENTS + DOT_SEPARATOR + InsightsConfigImpl.ENABLED_PROP;
    public static final String MAX_ERROR_EVENT_SAMPLES_STORED = ERROR_COLLECTOR_PREFIX + ErrorCollectorConfigImpl.MAX_EVENT_SAMPLES_STORED;
    public static final String COLLECT_TRACES = TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.COLLECT_TRACES;
    public static final String COLLECT_TRANSACTION_EVENTS = TRANSACTION_EVENTS_PREFIX + "collect_analytics_events";
    public static final String TRANSACTION_TARGET_SAMPLES_STORED = TRANSACTION_EVENTS_PREFIX + "target_samples_stored";
    public static final String COLLECT_SPAN_EVENTS = SPAN_EVENTS_PREFIX + SpanEventsConfig.COLLECT_SPAN_EVENTS;
    public static final String COLLECT_CUSTOM_INSIGHTS_EVENTS = CUSTOM_INSIGHT_EVENTS_PREFIX + InsightsConfigImpl.COLLECT_CUSTOM_EVENTS;
    public static final String RECORD_SQL = TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.RECORD_SQL;
    public static final String APPLICATION_LOGGING_ENABLED =  AgentConfigImpl.APPLICATION_LOGGING + DOT_SEPARATOR + ApplicationLoggingConfigImpl.ENABLED;
    public static final String APPLICATION_LOGGING_FORWARDING_ENABLED = AgentConfigImpl.APPLICATION_LOGGING + DOT_SEPARATOR +
            ApplicationLoggingConfigImpl.FORWARDING + DOT_SEPARATOR + ApplicationLoggingForwardingConfig.ENABLED;
    public static final String APPLICATION_LOGGING_FORWARDING_MAX_SAMPLES_STORED = AgentConfigImpl.APPLICATION_LOGGING + DOT_SEPARATOR +
            ApplicationLoggingConfigImpl.FORWARDING + DOT_SEPARATOR + ApplicationLoggingForwardingConfig.MAX_SAMPLES_STORED;
    public static final String APPLICATION_LOGGING_LOCAL_DECORATING_ENABLED = AgentConfigImpl.APPLICATION_LOGGING + DOT_SEPARATOR +
            ApplicationLoggingConfigImpl.LOCAL_DECORATING + DOT_SEPARATOR + ApplicationLoggingForwardingConfig.ENABLED;
    public static final String APPLICATION_LOGGING_METRICS_ENABLED = AgentConfigImpl.APPLICATION_LOGGING + DOT_SEPARATOR +
            ApplicationLoggingConfigImpl.METRICS + DOT_SEPARATOR + ApplicationLoggingForwardingConfig.ENABLED;
    @Deprecated
    public static final String SLOW_QUERY_WHITELIST = TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.SLOW_QUERY_WHITELIST;
    public static final String COLLECT_SLOW_QUERIES_FROM = TRANSACTION_TRACER_PREFIX + TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM;
    public static final String CROSS_APPLICATION_TRACER_PREFIX = AgentConfigImpl.CROSS_APPLICATION_TRACER + DOT_SEPARATOR;
    public static final String DISTRIBUTED_TRACING_PREFIX = AgentConfigImpl.DISTRIBUTED_TRACING + DOT_SEPARATOR;
    public static final String ENCODING_KEY = CROSS_APPLICATION_TRACER_PREFIX + CrossProcessConfigImpl.ENCODING_KEY;
    public static final String CROSS_PROCESS_ID = CROSS_APPLICATION_TRACER_PREFIX + CrossProcessConfigImpl.CROSS_PROCESS_ID;
    public static final String TRUSTED_ACCOUNT_IDS = CROSS_APPLICATION_TRACER_PREFIX + CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS;
    public static final String TRUSTED_ACCOUNT_KEY = DISTRIBUTED_TRACING_PREFIX + DistributedTracingConfig.TRUSTED_ACCOUNT_KEY;
    public static final String ACCOUNT_ID = DISTRIBUTED_TRACING_PREFIX + DistributedTracingConfig.ACCOUNT_ID;
    public static final String PRIMARY_APPLICATION_ID = DISTRIBUTED_TRACING_PREFIX + DistributedTracingConfig.PRIMARY_APPLICATION_ID;
    public static final String DISTRIBUTED_TRACING_ENABLED = DISTRIBUTED_TRACING_PREFIX + DistributedTracingConfig.ENABLED;
    public static final String STRIP_EXCEPTION = AgentConfigImpl.STRIP_EXCEPTION_MESSAGES;
    public static final String STRIP_EXCEPTION_ENABLED = STRIP_EXCEPTION + DOT_SEPARATOR + StripExceptionConfigImpl.ENABLED;
    @Deprecated
    public static final String STRIP_EXCEPTION_WHITELIST = STRIP_EXCEPTION + DOT_SEPARATOR + StripExceptionConfigImpl.WHITELIST;
    public static final String STRIP_EXCEPTION_ALLOWED_CLASSES = STRIP_EXCEPTION + DOT_SEPARATOR + StripExceptionConfigImpl.ALLOWED_CLASSES;
    public static final String EVENT_HARVEST_CONFIG = "event_harvest_config";

    @VisibleForTesting
    public static AgentConfig createAgentConfig(Map<String, Object> localSettings, Map<String, Object> serverData, Map<String, Boolean> laspData) {
        Map<String, Object> mergedSettings = DeepMapClone.deepCopy(localSettings);
        mergeServerData(mergedSettings, serverData, laspData);
        return AgentConfigImpl.createAgentConfig(mergedSettings);
    }

    /**
     * The server-side config (that is, values the user is allowed to manipulate in the APM UI)
     * is under the {@literal agent_config} key of the collector's response to connect().
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAgentData(Map<String, Object> serverData) {
        if (serverData == null) {
            return new HashMap<>();
        }
        return DeepMapClone.deepCopy((Map<String, Object>) serverData.get(AGENT_CONFIG));
    }

    // be careful with high security here - ssl must stay at true, record_sql must be off or obfuscated
    public static void mergeServerData(Map<String, Object> settings, Map<String, Object> serverData, Map<String, Boolean> laspData) {
        if (serverData == null && laspData == null) {
            return;
        }

        if (serverData == null) {
            serverData = new HashMap<>();
        }
        if (laspData == null) {
            laspData = new HashMap<>();
        }

        AgentConfig settingsConfig = AgentConfigImpl.createAgentConfig(settings);
        Map<String, Object> agentData = getAgentData(serverData);

        String recordSqlSecure = getMostSecureSql(agentData, settingsConfig, laspData);

        // True means agent is allowed to record attribute keys found in the attributes.include list when attributes are enabled.
        boolean attributesIncludeEnabled = getLaspValue(laspData, AttributesConfigImpl.ATTS_INCLUDE, true);

        List<String> attributesInclude = attributesIncludeEnabled
                ? settingsConfig.getAttributesConfig().attributesRootInclude() : Collections.emptyList();
        String attributesIncludeSecure = Joiner.on(",").join(attributesInclude);

        Boolean captureMessageParameters = getLaspValue(laspData, LaspPolicies.LASP_MESSAGE_PARAMETERS, true);
        if (!captureMessageParameters) {
            List<String> filteredAttributes = new ArrayList<>();
            String[] attributes = attributesIncludeSecure.split(",");
            for (String attribute : attributes) {
                // If we don't want to capture message parameters we need to remove them from the includes list
                if (attribute.startsWith("message.parameters.")) {
                    continue;
                }
                filteredAttributes.add(attribute);
            }
            attributesIncludeSecure = Joiner.on(",").join(filteredAttributes);
        }

        // we OR this comparison instead of ANDing it because the most secure state is if strip_exception_messages is true
        Boolean stripExceptionMessagesSecure = settingsConfig.getStripExceptionConfig().isEnabled() || getLaspValue(laspData, STRIP_EXCEPTION_ENABLED, false);

        Boolean customEventsSecure;
        Boolean serverSideCustomEvents = (Boolean) serverData.get(InsightsConfigImpl.COLLECT_CUSTOM_EVENTS);
        if (serverSideCustomEvents == null) {
            customEventsSecure = settingsConfig.getInsightsConfig().isEnabled()
                    && getLaspValue(laspData, CUSTOM_INSIGHTS_ENABLED, true);
        } else {
            customEventsSecure = settingsConfig.getInsightsConfig().isEnabled()
                    && getLaspValue(laspData, CUSTOM_INSIGHTS_ENABLED, true) && serverSideCustomEvents;
        }

        Boolean customParametersSecure = getLaspValue(laspData, LaspPolicies.LASP_CUSTOM_PARAMETERS, true);
        Boolean customInstrumentationEditor = getLaspValue(laspData, LaspPolicies.LASP_CUSTOM_INSTRUMENTATION_EDITOR, true);

        // calling remove here prevents mergeAgentData from always overriding local config
        // remove deprecated cross_application_tracing property
        agentData.remove(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING);
        agentData.remove(HIGH_SECURITY);
        agentData.remove("reinstrument");
        agentData.remove("reinstrument.attributes_enabled");
        agentData.remove(STRIP_EXCEPTION);
        agentData.remove(STRIP_EXCEPTION_ENABLED);
        agentData.remove(STRIP_EXCEPTION_ALLOWED_CLASSES);
        agentData.remove(STRIP_EXCEPTION_WHITELIST);
        agentData.remove(SLOW_QUERY_WHITELIST);
        agentData.remove(COLLECT_SLOW_QUERIES_FROM);
        Object serverRecordSql = agentData.remove(RECORD_SQL);

        // handle high security - record_sql must stay as obfuscated or off
        if (isHighSecurity(settingsConfig.getProperty(AgentConfigImpl.HIGH_SECURITY))) {
            // check record sql
            if (isValidRecordSqlValue(serverRecordSql)) {
                addServerProp(RECORD_SQL, serverRecordSql, settings);
            }
        } else {
            // we are not in high security - set whatever property you want
            addServerProp(RECORD_SQL, serverRecordSql, settings);
        }

        addServerProp(AgentConfigImpl.TRANSACTION_NAMING_SCHEME, serverData.get(AgentConfigImpl.TRANSACTION_NAMING_SCHEME), settings);

        mergeAgentData(settings, agentData);
        // server properties
        addServerProp(AgentConfigImpl.APDEX_T, serverData.get(AgentConfigImpl.APDEX_T), settings);

        addServerProp(COLLECT_ERRORS, serverData.get(ErrorCollectorConfigImpl.COLLECT_ERRORS), settings);
        addServerProp(COLLECT_ERROR_EVENTS, serverData.get(ErrorCollectorConfigImpl.COLLECT_EVENTS), settings);
        addServerProp(CAPTURE_ERROR_EVENTS, serverData.get(ErrorCollectorConfigImpl.CAPTURE_EVENTS), settings);
        addServerProp(MAX_ERROR_EVENT_SAMPLES_STORED, serverData.get(ErrorCollectorConfigImpl.MAX_EVENT_SAMPLES_STORED), settings);
        addServerProp(AiMonitoringUtils.COLLECT_AI, serverData.get(AiMonitoringUtils.COLLECT_AI), settings);
        addServerProp(COLLECT_TRACES, serverData.get(TransactionTracerConfigImpl.COLLECT_TRACES), settings);
        addServerProp(COLLECT_TRANSACTION_EVENTS, serverData.get("collect_analytics_events"), settings);
        addServerProp(COLLECT_CUSTOM_INSIGHTS_EVENTS, serverData.get(InsightsConfigImpl.COLLECT_CUSTOM_EVENTS), settings);
        addServerProp(COLLECT_SPAN_EVENTS, serverData.get(SpanEventsConfig.COLLECT_SPAN_EVENTS), settings);
        addServerProp(SERVER_SPAN_HARVEST_CONFIG, serverData.get(SERVER_SPAN_HARVEST_CONFIG), settings);
        // key transaction server properties
        addServerProp(AgentConfigImpl.KEY_TRANSACTIONS, serverData.get(AgentConfigImpl.KEY_TRANSACTIONS), settings);
        // cross application tracing server properties
        addServerProp(CROSS_PROCESS_ID, serverData.get(CrossProcessConfigImpl.CROSS_PROCESS_ID), settings);
        addServerProp(ENCODING_KEY, serverData.get(CrossProcessConfigImpl.ENCODING_KEY), settings);
        addServerProp(TRUSTED_ACCOUNT_IDS, serverData.get(CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS), settings);
        // distributed tracing server properties received on connect
        addServerProp(TRUSTED_ACCOUNT_KEY, serverData.get(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY), settings);
        addServerProp(ACCOUNT_ID, serverData.get(DistributedTracingConfig.ACCOUNT_ID), settings);
        addServerProp(PRIMARY_APPLICATION_ID, serverData.get(DistributedTracingConfig.PRIMARY_APPLICATION_ID), settings);
        // Expected errors server properties
        addServerProp(EXPECTED_CLASSES, serverData.get(ErrorCollectorConfigImpl.EXPECTED_CLASSES), settings);
        addServerProp(EXPECTED_STATUS_CODES, serverData.get(ErrorCollectorConfigImpl.EXPECTED_STATUS_CODES), settings);

        // Transaction event properties
        addServerProp(TRANSACTION_TARGET_SAMPLES_STORED, serverData.get("sampling_target"), settings);

        // Adding agent_run_id & account_id to config as required by Security agent
        addServerProp(ConnectionResponse.AGENT_RUN_ID_KEY, serverData.get(ConnectionResponse.AGENT_RUN_ID_KEY), settings);
        addServerProp(DistributedTracingConfig.ACCOUNT_ID, serverData.get(DistributedTracingConfig.ACCOUNT_ID), settings);
        addServerProp("agent_home", ConfigFileHelper.getNewRelicDirectory().getAbsolutePath(), settings);

        // Application logging
        addServerProp(APPLICATION_LOGGING_ENABLED, agentData.get(APPLICATION_LOGGING_ENABLED), settings);
        addServerProp(APPLICATION_LOGGING_FORWARDING_ENABLED, agentData.get(APPLICATION_LOGGING_FORWARDING_ENABLED), settings);
        addServerProp(APPLICATION_LOGGING_FORWARDING_MAX_SAMPLES_STORED, agentData.get(APPLICATION_LOGGING_FORWARDING_MAX_SAMPLES_STORED), settings);
        addServerProp(APPLICATION_LOGGING_LOCAL_DECORATING_ENABLED, agentData.get(APPLICATION_LOGGING_LOCAL_DECORATING_ENABLED), settings);
        addServerProp(APPLICATION_LOGGING_METRICS_ENABLED, agentData.get(APPLICATION_LOGGING_METRICS_ENABLED), settings);

        if (AgentJarHelper.getAgentJarDirectory() != null) {
            addServerProp("agent_jar_location", AgentJarHelper.getAgentJarDirectory().getAbsolutePath(), settings);
        }
        if (settingsConfig.getProperty(SECURITY_POLICIES_TOKEN) != null) {
            addServerProp(RECORD_SQL, recordSqlSecure, settings);
            // Root
            addServerProp(AttributesConfigImpl.ATTS_INCLUDE, attributesIncludeSecure, settings);

            if (!attributesIncludeEnabled) {
                // Use empty list if attribute_include is disabled by LASP
                addServerProp(AgentConfigImpl.ERROR_COLLECTOR + "." + AttributesConfigImpl.ATTS_INCLUDE, Collections.emptyList(), settings);
                addServerProp(AgentConfigImpl.TRANSACTION_EVENTS + "." + AttributesConfigImpl.ATTS_INCLUDE, Collections.emptyList(), settings);
                addServerProp(AgentConfigImpl.TRANSACTION_TRACER + "." + AttributesConfigImpl.ATTS_INCLUDE, Collections.emptyList(), settings);
                addServerProp(AgentConfigImpl.BROWSER_MONITORING + "." + AttributesConfigImpl.ATTS_INCLUDE, Collections.emptyList(), settings);
                addServerProp(AgentConfigImpl.SPAN_EVENTS + "." + AttributesConfigImpl.ATTS_INCLUDE, Collections.emptyList(), settings);
                addServerProp(AgentConfigImpl.TRANSACTION_SEGMENTS + "." + AttributesConfigImpl.ATTS_INCLUDE, Collections.emptyList(), settings);
            }

            addServerProp(STRIP_EXCEPTION_ENABLED, stripExceptionMessagesSecure, settings);
            addServerProp(CUSTOM_INSIGHTS_ENABLED, customEventsSecure, settings);
            addServerProp(LaspPolicies.LASP_CUSTOM_PARAMETERS, customParametersSecure, settings);
            addServerProp(LaspPolicies.LASP_CUSTOM_INSTRUMENTATION_EDITOR, customInstrumentationEditor, settings);
        }

        // Copy "event_harvest_config" over from the serverData since it doesn't live in the "agent_config" subsection (it's a top level property from the collector)
        Object eventHarvestConfig = serverData.get(EVENT_HARVEST_CONFIG);
        addServerProp(AgentConfigFactory.EVENT_HARVEST_CONFIG, eventHarvestConfig, settings);

        // when log forwarding is disabled account wide, the backend will inform the agent by setting the harvest limit to 0
        if (eventHarvestConfig instanceof Map) {
            Object harvestLimits = ((Map<?,?>) eventHarvestConfig).get(HarvestServiceImpl.HARVEST_LIMITS);
            if (harvestLimits instanceof Map) {
                Object logLimit = ((Map<?,?>) harvestLimits).get(CollectorMethods.LOG_EVENT_DATA);
                if (logLimit instanceof Number && ((Number) logLimit).intValue() == 0) {
                    String loggingForwardingEnabled = AgentConfigImpl.APPLICATION_LOGGING + "." + ApplicationLoggingConfigImpl.FORWARDING + "." +
                            ApplicationLoggingForwardingConfig.ENABLED;
                    addServerProp(loggingForwardingEnabled, false, settings);
                }
            }
        }


        // Browser settings
        addServerProp(BrowserConfig.BROWSER_KEY, serverData.get(BrowserConfig.BROWSER_KEY), settings);
        addServerProp(BrowserConfig.BROWSER_LOADER_VERSION, serverData.get(BrowserConfig.BROWSER_LOADER_VERSION), settings);
        addServerProp(BrowserConfig.JS_AGENT_LOADER, serverData.get(BrowserConfig.JS_AGENT_LOADER), settings);
        addServerProp(BrowserConfig.JS_AGENT_FILE, serverData.get(BrowserConfig.JS_AGENT_FILE), settings);
        addServerProp(BrowserConfig.BEACON, serverData.get(BrowserConfig.BEACON), settings);
        addServerProp(BrowserConfig.ERROR_BEACON, serverData.get(BrowserConfig.ERROR_BEACON), settings);
        addServerProp(BrowserConfig.APPLICATION_ID, serverData.get(BrowserConfig.APPLICATION_ID), settings);

        // Normalization settings
        addServerProp(NormalizationRuleConfig.URL_RULES_KEY, serverData.get(NormalizationRuleConfig.URL_RULES_KEY), settings);
        addServerProp(NormalizationRuleConfig.METRIC_NAME_RULES_KEY, serverData.get(NormalizationRuleConfig.METRIC_NAME_RULES_KEY), settings);
        addServerProp(NormalizationRuleConfig.TRANSACTION_NAME_RULES_KEY, serverData.get(NormalizationRuleConfig.TRANSACTION_NAME_RULES_KEY), settings);
        addServerProp(NormalizationRuleConfig.TRANSACTION_SEGMENT_TERMS_KEY, serverData.get(NormalizationRuleConfig.TRANSACTION_SEGMENT_TERMS_KEY), settings);

        // Remote instrumentation
        addServerProp(RemoteInstrumentationServiceImpl.INSTRUMENTATION_CONFIG, serverData.get(RemoteInstrumentationServiceImpl.INSTRUMENTATION_CONFIG),
                settings);
    }

    private static String getMostSecureSql(Map<String, Object> agentData, AgentConfig settings, Map<String, Boolean> laspData) {
        String server = (String) agentData.get(RECORD_SQL);
        String local = settings.getTransactionTracerConfig().getRecordSql();
        Boolean lasp = getLaspValue(laspData, RECORD_SQL, null);

        if (SqlObfuscator.OFF_SETTING.equals(server) || SqlObfuscator.OFF_SETTING.equals(local) || (lasp != null && !lasp)) {
            return SqlObfuscator.OFF_SETTING;
        }
        if (SqlObfuscator.OBFUSCATED_SETTING.equals(server) || SqlObfuscator.OBFUSCATED_SETTING.equals(local) || (lasp != null && lasp)) {
            return SqlObfuscator.OBFUSCATED_SETTING;
        }
        return SqlObfuscator.RAW_SETTING;
    }

    private static Boolean getLaspValue(Map<String, Boolean> policies, String key, Boolean defaultValue) {
        if (policies != null && policies.containsKey(key)) {
            return policies.get(key);
        }
        return defaultValue;
    }

    private static boolean isValidRecordSqlValue(Object recordSqlValue) {
        if ((recordSqlValue == null) || !(recordSqlValue instanceof String)) {
            return false;
        }
        String rSql = ((String) recordSqlValue).toLowerCase();
        if (!(rSql.equals(SqlObfuscator.OFF_SETTING) || rSql.equals(SqlObfuscator.OBFUSCATED_SETTING))) {
            return false;
        }
        return true;
    }

    private static boolean isHighSecurity(Object value) {
        return (value instanceof Boolean) && ((Boolean) value);
    }

    private static void mergeAgentData(Map<String, Object> settings, Map<String, Object> agentData) {
        Map.Entry<String, Object> entry;
        for (Iterator<Map.Entry<String, Object>> it = agentData.entrySet().iterator(); it.hasNext(); addServerProp(
                entry.getKey(), entry.getValue(), settings)) {
            entry = it.next();
        }
    }

    private static void addServerProp(String prop, Object val, Map<String, Object> settings) {
        if (val == null) {
            return;
        }

        addMappedProperty(prop, ServerProp.createPropObject(val), settings);
    }

    /**
     * <b>Warning: This method will mutate the provided settings map.</b>
     * <p>It takes the prop parameters, splits it on periods, and creates sub-maps for each level of periods, adding
     * them to the input settings map.
     */
    public static void addSimpleMappedProperty(String prop, Object val, Map<String, Object> settings) {
        addMappedProperty(prop, val, settings);
    }

    /**
     * Take the dot-delimited key in `prop` and dive into `settings`, using the dot to
     * separate levels in the hierarchy in `settings`.
     *
     * @param prop     a dot-delimited key, like "slow_sql.enabled".
     * @param val      a non-null value to set, like {@literal true}. Null values will be ignored.
     * @param settings A map that may contain nested maps, like {slow_sql: {}}. Sublevels are created if they do not exist.
     */
    @SuppressWarnings("unchecked")
    private static void addMappedProperty(String prop, Object val, Map<String, Object> settings) {
        if (val == null) {
            return;
        }
        Map<String, Object> currentMap = settings;
        int count = 0;
        String[] propArray = prop.split(PERIOD_REGEX);
        for (String propPart : propArray) {
            count++;
            if (count < propArray.length) {
                Map<String, Object> propMap = null;

                Object propValue = currentMap.get(propPart);
                if (propValue instanceof Map) {
                    propMap = (Map<String, Object>) currentMap.get(propPart);
                } else if (propValue instanceof ServerProp) {
                    propMap = (Map<String, Object>) ((ServerProp) propValue).getValue();
                }

                if (propMap == null) {
                    propMap = new HashMap<>();
                    currentMap.put(propPart, propMap);
                }
                currentMap = propMap;
            } else {
                currentMap.put(propPart, val);
            }
        }
    }

}
