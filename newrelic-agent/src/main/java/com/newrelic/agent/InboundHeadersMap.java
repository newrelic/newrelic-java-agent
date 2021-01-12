/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link InboundHeaders} that is backed by a map.
 */
public class InboundHeadersMap implements InboundHeaders {

    private final HeaderType type;
    private final Map<String, String> map;

    public InboundHeadersMap(HeaderType type, Map<String, String> map) {
        this.type = type;
        this.map = new HashMap<>(map);
    }

    @Override
    public HeaderType getHeaderType() {
        return type;
    }

    @Override
    public String getHeader(String name) {
        return map.get(name);
    }

}