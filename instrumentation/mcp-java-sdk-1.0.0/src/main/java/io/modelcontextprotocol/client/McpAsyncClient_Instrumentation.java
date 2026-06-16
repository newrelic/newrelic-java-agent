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
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import static com.nr.instrumentation.mcp.McpUtils.endSegmentOnFinally;
import static com.nr.instrumentation.mcp.McpUtils.extractScheme;
import static com.nr.instrumentation.mcp.McpUtils.startMcpSegment;

@Weave(type = MatchType.ExactClass, originalName = "io.modelcontextprotocol.client.McpAsyncClient")
public abstract class McpAsyncClient_Instrumentation {

    public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
        String segmentName = callToolRequest.name() != null ? callToolRequest.name() : "tool";
        final Segment segment = startMcpSegment("Llm/tool/MCP/call_tool", segmentName);
        return endSegmentOnFinally(Weaver.callOriginal(), segment);
    }

    public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.ReadResourceRequest readResourceRequest) {
        final Segment segment = startMcpSegment("Llm/resource/MCP/read_resource", extractScheme(readResourceRequest.uri()));
        return endSegmentOnFinally(Weaver.callOriginal(), segment);
    }

    public Mono<McpSchema.GetPromptResult> getPrompt(McpSchema.GetPromptRequest getPromptRequest) {
        String segmentName = getPromptRequest.name() != null ? getPromptRequest.name() : "prompt";
        final Segment segment = startMcpSegment("Llm/prompt/MCP/get_prompt", segmentName);
        return endSegmentOnFinally(Weaver.callOriginal(), segment);
    }
}