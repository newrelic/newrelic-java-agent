/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.HashMap;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

@SuppressWarnings("serial")
public class OutboundHeadersMap extends HashMap<String, String> implements OutboundHeaders {
    private final HeaderType type;

    public OutboundHeadersMap(HeaderType type) {
        super();
        this.type = type;
    }

    @Override
    public HeaderType getHeaderType() {
        return type;
    }

    @Override
    public void setHeader(String name, String value) {
        put(name, value);
    }
}
