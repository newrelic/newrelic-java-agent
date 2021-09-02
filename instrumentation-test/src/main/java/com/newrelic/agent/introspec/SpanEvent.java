/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.util.Map;

public interface SpanEvent {
    String getName();

    float duration();

    String traceId();

    String parentId();

    String category();

    String getHttpUrl();

    String getHttpMethod();

    String getHttpComponent();

    String getTransactionId();

    Map<String, Object> getAgentAttributes();
}
