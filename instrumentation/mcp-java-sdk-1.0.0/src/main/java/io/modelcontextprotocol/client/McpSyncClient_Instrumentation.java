/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.modelcontextprotocol.client;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.mcp.McpUtils;
import io.modelcontextprotocol.spec.McpSchema;

import static com.nr.instrumentation.mcp.McpUtils.extractScheme;
import static com.nr.instrumentation.mcp.McpUtils.startMcpSegment;

@Weave(type = MatchType.ExactClass, originalName = "io.modelcontextprotocol.client.McpSyncClient")
public abstract class McpSyncClient_Instrumentation {

    public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
        String segmentName = callToolRequest.name() != null ? callToolRequest.name() : "tool";
        Segment segment = startMcpSegment("Llm/tool/MCP/call_tool", segmentName);
        McpUtils.IN_SYNC_MCP_CALL.set(true);
        try {
            return Weaver.callOriginal();
        } finally {
            McpUtils.IN_SYNC_MCP_CALL.set(false);
            if (segment != null) {
                segment.end();
            }
        }
    }

    public McpSchema.ReadResourceResult readResource(McpSchema.ReadResourceRequest readResourceRequest) {
        Segment segment = startMcpSegment("Llm/resource/MCP/read_resource", extractScheme(readResourceRequest.uri()));
        McpUtils.IN_SYNC_MCP_CALL.set(true);
        try {
            return Weaver.callOriginal();
        } finally {
            McpUtils.IN_SYNC_MCP_CALL.set(false);
            if (segment != null) {
                segment.end();
            }
        }
    }

    public McpSchema.GetPromptResult getPrompt(McpSchema.GetPromptRequest getPromptRequest) {
        String segmentName = getPromptRequest.name() != null ? getPromptRequest.name() : "prompt";
        Segment segment = startMcpSegment("Llm/prompt/MCP/get_prompt", segmentName);
        McpUtils.IN_SYNC_MCP_CALL.set(true);
        try {
            return Weaver.callOriginal();
        } finally {
            McpUtils.IN_SYNC_MCP_CALL.set(false);
            if (segment != null) {
                segment.end();
            }
        }
    }
}