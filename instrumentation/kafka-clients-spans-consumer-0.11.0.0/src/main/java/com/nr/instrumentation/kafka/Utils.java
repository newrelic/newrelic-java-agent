/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;

public class Utils {
    public static final boolean DT_CONSUMER_ENABLED = NewRelic.getAgent().getConfig()
            .getValue("kafka.spans.distributed_trace.consume_many.enabled", false);
}
