/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.ForceDisconnectException;
import com.newrelic.agent.transaction.TransactionNamingScheme;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MergeServerDataTest {

    private Map<String, Object> loadLocalConfig() {
        Map<String, Object> localConfig = new HashMap<>();
        localConfig.put("security_policies_token", "SOME_TOKEN");
        return localConfig;
    }

    private Map<String, Object> createLocalConfig() throws Exception {
        InputStream configFile = MergeServerDataTest.class.getResourceAsStream("/localConfigServerOverride.yml");
        return AgentConfigHelper.parseConfiguration(configFile);
    }

    private Map<String, Object> createServerSideConfig() throws ParseException {
        String serverSideConfig = "{\n" +
                "    \"transaction_naming_scheme\": \"legacy\",\n" +
                "    \"collect_traces\": false,\n" +
                "    \"collect_error_events\": true,\n" +
                "    \"browser_key\": \"browser-key\",\n" +
                "    \"browser_monitoring.debug\": null,\n" +
                "    \"js_agent_loader\": \"\",\n" +
                "    \"trusted_account_ids\": [\"1tak234\"],\n" +
                "    \"collect_errors\": true,\n" +
                "    \"error_beacon\": \"staging-bam.nr-data.net\",\n" +
                "    \"browser_monitoring.loader_version\": \"1071\",\n" +
                "    \"encoding_key\": \"deadbeefcafebabe8675309babecafe1beefdead\",\n" +
                "    \"application_id\": \"1app456\",\n" +
                "    \"browser_monitoring.loader\": null,\n" +
                "    \"cross_process_id\": \"1tak234#1app456\",\n" +
                "    \"apdex_t\": 0.5,\n" +
                "    \"beacon\": \"staging-bam.nr-data.net\",\n" +
                "    \"collect_analytics_events\": true,\n" +
                "    \"agent_config\": {\n" +
                "      \"application_logging.enabled\": false, \n" +
                "      \"application_logging.forwarding.enabled\": false, \n" +
                "      \"application_logging.forwarding.max_samples_stored\": 10001, \n" +
                "      \"application_logging.local_decorating.enabled\": true, \n" +
                "      \"application_logging.metrics.enabled\": false, \n" +
                "      \"transaction_tracer.explain_enabled\": true,\n" +
                "      \"transaction_tracer.transaction_threshold\": 0.005,\n" +
                "      \"transaction_tracer.enabled\": true,\n" +
                "      \"ignored_params\": [],\n" +
                "      \"audit_mode\": false,\n" +
                "      \"log_level\": \"info\",\n" +
                "      \"transaction_tracer.explain_threshold\": 0.5,\n" +
                "      \"thread_profiler.enabled\": true,\n" +
                "      \"transaction_tracer.record_sql\": \"raw\",\n" +
                "      \"slow_sql.enabled\": true,\n" +
                "      \"error_collector.ignore_errors\": [],\n" +
                "      \"capture_params\": true,\n" +
                "      \"transaction_tracer.stack_trace_threshold\": 0.5,\n" +
                "      \"rum.load_episodes_file\": true,\n" +
                "      \"error_collector.ignore_status_codes\": [404],\n" +
                "      \"transaction_tracer.log_sql\": false,\n" +
                "      \"agent_enabled\": true,\n" +
                "      \"error_collector.enabled\": true,\n" +
                "      \"transaction_tracer.obfuscated_sql_fields\": []\n" +
                "    }\n" +
                "  }";

        JSONParser parser = new JSONParser();
        Object response = parser.parse(serverSideConfig);
        return Map.class.cast(response);
    }

    @Test
    public void serverOverridesLocal() throws Exception {
        Map<String, Object> localConfig = createLocalConfig();
        Map<String, Object> serverData = createServerSideConfig();

        AgentConfigFactory.mergeServerData(localConfig, serverData);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localConfig);

        assertEquals(TransactionNamingScheme.LEGACY, config.getTransactionNamingScheme());
        assertEquals(false, config.getTransactionTracerConfig().isEnabled());
        assertEquals(true, config.getTransactionTracerConfig().isExplainEnabled());
        assertEquals(500, config.getTransactionTracerConfig().getExplainThresholdInMillis(), 0.0f);
        assertEquals(5, config.getTransactionTracerConfig().getTransactionThresholdInMillis(), 0.0f);
        assertEquals("raw", config.getTransactionTracerConfig().getRecordSql());
        assertEquals(500, config.getTransactionTracerConfig().getStackTraceThresholdInMillis(), 0.0f);
        assertEquals(false, config.getTransactionTracerConfig().isLogSql());

        assertEquals(true, config.getErrorCollectorConfig().isEnabled());
        assertEquals(true, config.getErrorCollectorConfig().isEventsEnabled());
        assertTrue(config.getErrorCollectorConfig().getIgnoreStatusCodes().contains(404));

        assertTrue(config.getCrossProcessConfig().isTrustedAccountId("1tak234"));

        assertEquals(500, config.getApdexTInMillis(), 0.0f);

        assertEquals(true, config.getTransactionEventsConfig().isEnabled());

        assertEquals(true, config.getThreadProfilerConfig().isEnabled());

        assertEquals(true, config.getSqlTraceConfig().isEnabled());
        assertEquals(true, config.getAttributesConfig().isEnabledRoot());
        assertEquals(4, config.getAttributesConfig().attributesRootInclude().size());

        assertEquals(true, config.isAgentEnabled());
    }
}
