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
 * Provides sconfig options for tuning the HttpUrlConnection instrumentation.
 * By default, the instrumentation is verbose and captures every response handler method as a segment,
 * with only the first segment reporting the associated external call.
 *
 * To turn off verbose reporting and suppress HTTPUrlConnection segments that don't make external calls, set
 * -Dnewrelic.config.class_transformer.com.newrelic.instrumentation.htturlconnection.verbose=false
 */
public class HttpURLConnectionConfig {
    private static final boolean DEFAULT_DISTRIBUTED_TRACING_ENABLED = true;

    private static final String VERBOSE_PREFIX = "class_transformer.com.newrelic.instrumentation.httpurlconnection.verbose";

    private static final boolean DEFAULT_VERBOSE_SETTING = true;

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

    public static boolean getVerbose() {
        return NewRelic.getAgent().getConfig().getValue(VERBOSE_PREFIX, DEFAULT_VERBOSE_SETTING);
    }
}
