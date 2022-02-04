/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;

import java.util.concurrent.atomic.AtomicReference;

public final class DistributedTraceUtil {

    private DistributedTraceUtil() {
    }

    // Payload constants
    public static final String VERSION = "v";
    public static final String DATA = "d";
    public static final String PARENT_TYPE = "ty";
    public static final String APP_PARENT_TYPE = "App";
    public static final String ACCOUNT_ID = "ac";
    public static final String TRUSTED_ACCOUNT_KEY = "tk";
    public static final String APPLICATION_ID = "ap";
    public static final String TIMESTAMP = "ti";
    public static final String GUID = "id";
    public static final String TRACE_ID = "tr";
    public static final String TX = "tx";
    public static final String PRIORITY = "pr";
    public static final String SAMPLED = "sa";

    public static boolean isSampledPriority(float priority) {
        return priority >= 1.0f;
    }
}
