/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jettyhttpclient120;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.http.HttpFields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps a Jetty 12 {@link Response} so that New Relic can read distributed-tracing
 * response headers for cross-application tracing.
 */
public class InboundWrapper extends ExtendedInboundHeaders {

    private final HttpFields headers;

    public InboundWrapper(Response response) {
        this.headers = response == null ? null : response.getHeaders();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        if (headers == null || name == null) {
            return null;
        }
        return headers.get(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        if (headers == null || name == null) {
            return Collections.emptyList();
        }
        List<String> values = headers.getValuesList(name);
        return values == null ? Collections.<String>emptyList() : values;
    }
}
