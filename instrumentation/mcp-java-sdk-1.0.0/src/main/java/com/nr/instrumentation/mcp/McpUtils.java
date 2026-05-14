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
