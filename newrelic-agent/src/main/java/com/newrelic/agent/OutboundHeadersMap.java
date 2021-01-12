/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.HashMap;
import java.util.Map;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

/**
 * An {@link OutboundHeaders} that is backed by a map.
 */
public class OutboundHeadersMap implements OutboundHeaders {

    private final HeaderType type;
    private final Map<String, String> map;

    public OutboundHeadersMap(HeaderType type) {
        this.type = type;
        this.map = new HashMap<>();
    }

    @Override
    public HeaderType getHeaderType() {
        return type;
    }


    @Override
    public void setHeader(String name, String value) {
        map.put(name, value);
    }

    /**
     * Obtain a copy of the headers backing map.
     *
     * @return the headers map
     */
    public Map<String, String> asMap() {
        return new HashMap<>(map);
    }

}
