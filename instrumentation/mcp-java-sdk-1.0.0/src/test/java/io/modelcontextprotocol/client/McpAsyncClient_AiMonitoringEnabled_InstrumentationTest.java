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
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"io.modelcontextprotocol.client"}, configName = "llm_enabled.yml")
public class McpAsyncClient_AiMonitoringEnabled_InstrumentationTest {

    private static final McpAsyncClientMock mockMcpAsyncClient = new McpAsyncClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testAiMonitoringEnabledCallToolSegmentCreated() {
        callToolInTransaction();

        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(1)));
        String txnName = introspector.getTransactionNames().iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        assertTrue(metrics.containsKey("Llm/tool/MCP/call_tool/testTool"));
    }

    @Test
    public void testAiMonitoringEnabledReadResourceSegmentCreated() {
        readResourceInTransaction();

        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(1)));
        String txnName = introspector.getTransactionNames().iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        assertTrue(metrics.containsKey("Llm/resource/MCP/read_resource/https"));
    }

    @Test
    public void testAiMonitoringEnabledGetPromptSegmentCreated() {
        getPromptInTransaction();

        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(1)));
        String txnName = introspector.getTransactionNames().iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        assertTrue(metrics.containsKey("Llm/prompt/MCP/get_prompt/testPrompt"));
    }

    @Test
    public void testAiMonitoringEnabledNoActiveTxnSegmentNotCreated() {
        try { mockMcpAsyncClient.callTool(new McpSchema.CallToolRequest("testTool", null)).block(); }
        catch (Exception ignored) {}

        assertEquals(0, introspector.getFinishedTransactionCount(100));
    }

    @Trace(dispatcher = true)
    private void callToolInTransaction() {
        try { mockMcpAsyncClient.callTool(new McpSchema.CallToolRequest("testTool", null)).block(); }
        catch (Exception ignored) {}
    }

    @Trace(dispatcher = true)
    private void readResourceInTransaction() {
        try { mockMcpAsyncClient.readResource(new McpSchema.ReadResourceRequest("https://example.com/resource")).block(); }
        catch (Exception ignored) {}
    }

    @Trace(dispatcher = true)
    private void getPromptInTransaction() {
        try { mockMcpAsyncClient.getPrompt(new McpSchema.GetPromptRequest("testPrompt", null)).block(); }
        catch (Exception ignored) {}
    }
}
