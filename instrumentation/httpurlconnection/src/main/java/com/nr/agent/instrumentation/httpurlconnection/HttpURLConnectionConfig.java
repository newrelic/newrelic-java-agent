/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

/**
 * Provides some config options for tuning the instrumentation's use of a SegmentCleanupTask runnable.
 * Specifically, thread_pool_size configures the number of threads used by the executor to execute
 * the runnables and delay_ms configures the delay before a scheduled runnable task is executed.
 * <p>
 * See README for config examples.
 */
public class HttpURLConnectionConfig {
    private static final boolean DEFAULT_DISTRIBUTED_TRACING_ENABLED = true;

    private HttpURLConnectionConfig() {
    }

    public static boolean distributedTracingEnabled() {
        boolean dtEnabled = DEFAULT_DISTRIBUTED_TRACING_ENABLED;
        try {
            dtEnabled = NewRelic.getAgent().getConfig().getValue("distributed_tracing.enabled", DEFAULT_DISTRIBUTED_TRACING_ENABLED);
        } catch (Exception ignored) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "HttpURLConnection - using default distributed tracing enabled: " + dtEnabled);
        }
        return dtEnabled;
    }
}
