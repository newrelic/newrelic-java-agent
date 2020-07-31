/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import java.net.HttpURLConnection;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

public class OutboundWrapper implements OutboundHeaders {

    private final HttpURLConnection connection;

    public OutboundWrapper(HttpURLConnection connection) {
        this.connection = connection;
    }

    @Override
    public void setHeader(String name, String value) {
        try {
            connection.setRequestProperty(name, value);
        } catch (IllegalStateException e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, "Failed to set request property. Cause: {0}. " 
                    + "(an IllegalStateException can be expected for certain usages of HttpURLConnection).",
                    e.getMessage());
        }
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}