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
            // If a connection has already been established then we cannot modify the HttpURLConnection header map as it becomes immutable.
            // Trying to do so will cause the HttpURLConnection to internally throw/catch "java.lang.IllegalStateException: Already connected"
            connection.setRequestProperty(name, value);
        } catch (IllegalStateException e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, "Failed to set request property: key={0}, value={1}. Cause: {2}. "
                    + "(an IllegalStateException can be expected for certain usages of HttpURLConnection).", name, value, e.getMessage());
        }
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}