/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;

import com.newrelic.api.agent.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.ConfigServiceImpl;

/* (non-javadoc)
 * Note: the "beacon" was a predecessor technology for correlated transaction traces with the browser. 
 * Some appearances of the term could be changed to "browser" now.
 */

/**
 * There is no configuration specifically for synthetics; the spec states that synthetics should always be enabled. But
 * synthetics and CAT share the same decoding key (and algorithm) for the obfuscated headers, so there is a danger that
 * disabling CAT (or maybe Browser) would screw up synthetics. This test is supposed to check for that.
 */
public class CrossProcessAndSyntheticsConfigTest {

    // appName must match .yml files in newrelic-agent/src/test/resources/com/newrelic/agent/config
    String appName = "Unit Test";

    /*-
    This is the Python definition of what the AIT Mock Collector was sending in 2014-10, at least.
    
    CONNECT_OPTIONS = {
        'agent_config' : {
            'agent_enabled'                            : True,
            'log_level'                                : 'info',
            'audit_mode'                               : False,
            'capture_params'                           : True,
            'error_collector.enabled'                  : True,
            'error_collector.ignore_errors'            : [],
            'error_collector.ignore_status_codes'      : [404],
            'ignored_params'                           : [],
            'rum.load_episodes_file'                   : True,
            'slow_sql.enabled'                         : True,
            'thread_profiler.enabled'                  : True,
            'transaction_tracer.enabled'               : True,
            'transaction_tracer.explain_enabled'       : True,
            'transaction_tracer.explain_threshold'     : 0.5,
            'transaction_tracer.log_sql'               : False,
            'transaction_tracer.obfuscated_sql_fields' : [],
            'transaction_tracer.record_sql'            : 'raw',
            'transaction_tracer.stack_trace_threshold' : 0.5,
            'transaction_tracer.transaction_threshold' : 0.0050,
        },
        'apdex_t'           : 0.5,
        'agent_run_id'      : 1,
        'url_rules'         : [],
        'collect_errors'    : True,
        'cross_process_id'  : AGENT_CROSS_PROCESS_ID,
        'trusted_account_ids'  : [TestCase.ACCOUNT_ID, AGENT_ACCOUNT_ID],
        'encoding_key'      : obfuscator.ENCODING_KEY,
        'messages' : [{
                'message' : 'mock HTTP collector',
                'level'   : 'INFO'
        }],
        'sampling_rate'      : 0,
        'collect_traces'     : True,
        'data_report_period' : 60,
        'rum.enabled'        : True,
        'browser_key'        : '12345',
        'browser_monitoring.loader_version' : '248',
        'beacon'             : 'staging-beacon-2.newrelic.com',
        'error_beacon'       : 'staging-jserror.newrelic.com',
        'application_id'     : '45047',
        'js_agent_loader'    : 'window.NREUM||(NREUM={}),__nr_require=function a(b,c,d)',
        'js_agent_file'      : 'js-agent.newrelic.com\nr-248.min.js'
    }
     */

    String encodingKey = "deadbeefcafebabe8675309babecafe1beefdead";

    // To make this easier to read, the JSON includes single quotes where double quotes are needed.
    // There is a replaceAll() hiding at the end of this initializer that fixes all.
    private final String collectorJson = new String("{" + "'agent_config': {"
            + "    'transaction_tracer.transaction_threshold':0.0050,"
            + "    'transaction_tracer.stack_trace_threshold':0.5," + "    'agent_enabled':true,"
            + "    'transaction_tracer.log_sql':false," + "    'error_collector.ignore_status_codes':[404],"
            + "    'transaction_tracer.enabled':true," + "    'ignored_params':[],"
            + "    'error_collector.ignore_errors':[]," + "    'error_collector.enabled':true,"
            + "    'transaction_tracer.obfuscated_sql_fields':[]," + "    'slow_sql.enabled':true,"
            + "    'transaction_tracer.explain_threshold':0.5," + "    'audit_mode':false,"
            + "    'transaction_tracer.explain_enabled':true," + "    'transaction_tracer.record_sql':'raw',"
            + "    'log_level':'info'," + "    'rum.load_episodes_file':true," + "    'capture_params':true,"
            + "    'thread_profiler.enabled':true" + "}," + "'agent_run_id':1,"
            + "'js_agent_file':'js-agent.newrelic.com\nr-248.min.js'," + "'collect_errors':true," + "'url_rules':[],"
            + "'cross_process_id':'54321#9876'," + "'collect_traces':true,"
            + "'messages':[{'message':'mock HTTP collector','level':'INFO'}]," + "'data_report_period':60,"
            + "'sampling_rate':0," + "'js_agent_loader':'window.NREUM||(NREUM={}),__nr_require=function a(b,c,d)',"
            + "'browser_key':'12345'," + "'encoding_key':'" + encodingKey + "'," + "'apdex_t':0.5,"
            + "'rum.enabled':true," + "'browser_monitoring.loader_version':'248'," + "'trusted_account_key':'67890',"
            + "'trusted_account_ids':[12345,'54321']," + "'error_beacon':'staging-jserror.newrelic.com',"
            + "'beacon':'staging-beacon-2.newrelic.com'," + "'application_id':'45047'" + "}").replaceAll("'", "\"");

    private final String ymlResourceDir = "src/test/resources/com/newrelic/agent/config/";

    private String makeYmlFilePath(String fileName) {
        return ymlResourceDir + fileName;
    }

    @Test
    public void testConfig1() throws Exception {
        testConfig(makeYmlFilePath("newrelic.yml"), false, true);
    }

    @Test
    public void testConfig2() throws Exception {
        testConfig(makeYmlFilePath("newrelicWithBrowserMonitoringFalse.yml"), true, false);
    }

    @Test
    public void testConfig3() throws Exception {
        testConfig(makeYmlFilePath("newrelicWithBrowserMonitoringNotSpecified.yml"), true, true);
    }

    @Test
    public void testConfig4() throws Exception {
        testConfig(makeYmlFilePath("newrelicWithCrossAppTracingFalse.yml"), false, true);
    }

    @Test
    public void testConfig5() throws Exception {
        testConfig(makeYmlFilePath("newrelicWithCrossAppTracingNotSpecified.yml"), false, true);
    }

    // The "expected value" arguments are all about the newrelic.yml. We're not varying the collector JSON here.
    public void testConfig(String ymlFilePath, boolean expectedValueOfCatEnable, boolean expectedValueOfBrowserEnabled)
            throws Exception {
        System.setProperty("newrelic.config.file", ymlFilePath);
        ConfigService configService = ConfigServiceFactory.createConfigService(mock(Logger.class), false);
        AgentConfig agentConfig = configService.getAgentConfig(appName);
        assertEquals(appName, agentConfig.getApplicationName());
        assertTrue(agentConfig instanceof AgentConfigImpl);
        assertTrue(configService instanceof ConfigServiceImpl);

        IRPMService rpmService = mock(IRPMService.class);
        Mockito.when(rpmService.getApplicationName()).thenReturn(appName);

        org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
        @SuppressWarnings("unchecked")
        Map<String, Object> serverData = (Map<String, Object>) parser.parse(collectorJson);
        ((ConfigServiceImpl) configService).connected(rpmService, serverData);

        assertEquals(configService.getAgentConfig(appName).getBrowserMonitoringConfig().isAutoInstrumentEnabled(),
                expectedValueOfBrowserEnabled);
        assertEquals(configService.getAgentConfig(appName).getCrossProcessConfig().isCrossApplicationTracing(),
                expectedValueOfCatEnable);
        String s = configService.getAgentConfig(appName).getValue("cross_application_tracer.encoding_key");
        assertEquals(s, encodingKey);
    }
}
