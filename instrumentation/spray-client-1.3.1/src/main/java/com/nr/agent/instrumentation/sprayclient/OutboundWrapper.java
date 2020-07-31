/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sprayclient;

import java.util.List;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

import spray.http.HttpHeader;
import spray.http.HttpHeaders;

public class OutboundWrapper implements OutboundHeaders {

    private final List<HttpHeader> headers;

    public OutboundWrapper(List<HttpHeader> headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.add(new HttpHeaders.RawHeader(name, value));
    }

    public String getHeader(String name) {
        for (HttpHeader header : headers) {
            if (header.is(name.toLowerCase())) {
                return header.value();
            }
        }
        return null;
    }

}
