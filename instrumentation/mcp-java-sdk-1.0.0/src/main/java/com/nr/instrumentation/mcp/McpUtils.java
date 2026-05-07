/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.mcp;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Segment;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.function.Consumer;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;

public class McpUtils {

    /**
     * Boolean set by the McpSyncClient_Instrumentation before calling Weaver.callOriginal() so the
     * async delegate (McpAsyncClient_Instrumentation) skips startSegment() and avoids double reporting.
     */
    public static final ThreadLocal<Boolean> IN_SYNC_MCP_CALL = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    /**
     * Helper method to start a Segment for an MCP client call with the provided category and name. If
     * AI Monitoring is disabled or there is no active transaction then it will return null.
     *
     * @param category is the category of the segment, e.g. "Llm/tool/MCP/call_tool"
     * @param name is the specific tool being called, resource scheme, or prompt name
     * @return the started Segment or null
     */
    public static Segment startMcpSegment(String category, String name) {
        if (IN_SYNC_MCP_CALL.get()) {
            return null;
        }

        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (!isAiMonitoringEnabled() || txn == null) {
            return null;
        }

        return txn.startSegment(category, name);
    }

    /**
     * Helper method to end a Segment when a Mono terminates, regardless of whether it completed successfully, errored out or was canceled.
     * If the segment is null then the Mono will be returned unmodified.
     * If the received Mono is null the segment will immediately be ended and null is returned.
     *
     * @param mono is the Mono to attach the doFinally callback to.
     * @param segment is the Segment to end when the Mono completes.
     * @return the original Mono with a doFinally callback to end the segment or null
     * @param <T> is the type of Mono
     */
    public static <T> Mono<T> endSegmentOnFinally(Mono<T> mono, Segment segment) {
        if (segment == null) {
            return mono;
        }

        if (mono == null) {
            segment.endAsync();
            return null;
        }

        return mono.doFinally(new Consumer<SignalType>() {
            @Override
            public void accept(SignalType signalType) {
                segment.endAsync();
            }
        });
    }

    /**
     * Helper method to extract the scheme from an MCP resource URI. If the URI is null, empty or doesn't contain a colon
     * then "resource" will be returned.
     *
     * @param uri is the URI to extract the scheme from
     * @return the scheme of the URI or "resource"
     */
    public static String extractScheme(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "resource";
        }
        int colonIndex = uri.indexOf(':');
        return colonIndex > 0
                ? uri.substring(0, colonIndex)
                : "resource";
    }
}
