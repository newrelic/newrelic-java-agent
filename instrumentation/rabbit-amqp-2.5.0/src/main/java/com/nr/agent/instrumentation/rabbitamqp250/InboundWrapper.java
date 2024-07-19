/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.rabbitamqp250;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;

public class InboundWrapper extends ExtendedInboundHeaders {
    private final Map<String, Object> delegate;

    public InboundWrapper(Map<String, Object> arguments) {
        super();
        this.delegate = arguments;
    }

    @Override
    public String getHeader(String name) {
        Object property = delegate.get(name);
        if (property == null) {
            return null;
        }
        return property.toString();
    }

    @Override
    public List<String> getHeaders(String name) {
        String result = getHeader(name);
        if (result == null) {
            return null;
        }
        return Collections.singletonList(result);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

}
