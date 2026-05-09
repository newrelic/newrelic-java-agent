/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.modelcontextprotocol.client;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"io.modelcontextprotocol.client"}, configName = "llm_disabled.yml")
public class McpSyncClient_AiMonitoringDisabled_InstrumentationTest {

    private static final McpSyncClientMock mockMcpSyncClient = new McpSyncClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testAiMonitoringDisabledCallToolSegmentNotCreated() {
        callToolInTransaction();

        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(1)));
        String txnName = introspector.getTransactionNames().iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        assertFalse(metrics.containsKey("Llm/tool/MCP/call_tool/testTool"));
    }

    @Test
    public void testAiMonitoringDisabledReadResourceSegmentNotCreated() {
        readResourceInTransaction();

        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(1)));
        String txnName = introspector.getTransactionNames().iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        assertFalse(metrics.containsKey("Llm/resource/MCP/read_resource/https"));
    }

    @Test
    public void testAiMonitoringDisabledGetPromptSegmentNotCreated() {
        getPromptInTransaction();

        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(1)));
        String txnName = introspector.getTransactionNames().iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        assertFalse(metrics.containsKey("Llm/prompt/MCP/get_prompt/testPrompt"));
    }

    @Trace(dispatcher = true)
    private void callToolInTransaction() {
        try { mockMcpSyncClient.callTool(new McpSchema.CallToolRequest("testTool", null)); }
        catch (Exception ignored) {}
    }

    @Trace(dispatcher = true)
    private void readResourceInTransaction() {
        try { mockMcpSyncClient.readResource(new McpSchema.ReadResourceRequest("https://example.com/resource")); }
        catch (Exception ignored) {}
    }

    @Trace(dispatcher = true)
    private void getPromptInTransaction() {
        try { mockMcpSyncClient.getPrompt(new McpSchema.GetPromptRequest("testPrompt", null)); }
        catch (Exception ignored) {}
    }
}