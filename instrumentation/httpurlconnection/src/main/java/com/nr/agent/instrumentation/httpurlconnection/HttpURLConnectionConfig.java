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
    private static final String configPrefix = "class_transformer.com.newrelic.instrumentation.httpurlconnection.segment_cleanup_task.";
    /*
     * The following tests do a Thread.sleep to account for this delay. If this value is changed then the tests will also need to be updated.
     * functional_test/src/test/java/com/newrelic/agent/instrumentation/pointcuts/net/HttpURLConnectionTest
     * instrumentation/httpurlconnection/src/test/java/com/nr/agent/instrumentation/httpurlconnection/MetricStateConnectTest
     */
    private static final int DEFAULT_TASK_DELAY_MS = 5_000;
    private static final int DEFAULT_THREAD_POOL_SIZE = 5;
    private static final boolean DEFAULT_DISTRIBUTED_TRACING_ENABLED = true;

    private HttpURLConnectionConfig() {
    }

    public static int getThreadPoolSize() {
        int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        try {
            threadPoolSize = NewRelic.getAgent().getConfig().getValue(configPrefix + "thread_pool_size", DEFAULT_THREAD_POOL_SIZE);
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "HttpURLConnection - using default thread_pool_size: " + threadPoolSize);
        }
        return threadPoolSize;
    }

    public static int getDelayMs() {
        int delayMs = DEFAULT_TASK_DELAY_MS;
        try {
            delayMs = NewRelic.getAgent().getConfig().getValue(configPrefix + "delay_ms", DEFAULT_TASK_DELAY_MS);
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "HttpURLConnection - using default task delay_ms: " + delayMs);
        }
        return delayMs;
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
