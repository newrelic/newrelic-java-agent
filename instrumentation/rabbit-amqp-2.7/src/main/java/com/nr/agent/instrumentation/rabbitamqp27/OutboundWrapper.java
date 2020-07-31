/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.rabbitamqp27;

import java.util.Map;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

public class OutboundWrapper implements OutboundHeaders {

    private final Map<String, Object> delegate;

    public OutboundWrapper(Map<String, Object> headers) {
        this.delegate = headers;
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.put(name, value);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }
}
