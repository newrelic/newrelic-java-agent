/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.spray;

import java.util.List;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;
import spray.http.HttpHeader;
import spray.http.HttpHeaders;
import spray.http.StatusCode;

public class ResponseWrapper implements Response {

    private final StatusCode status;
    private final List<HttpHeader> headers;

    public ResponseWrapper(StatusCode status, List<HttpHeader> headers) {
        this.status = status;
        this.headers = headers;
    }

    @Override
    public int getStatus() throws Exception {
        return status.intValue();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return status.reason();
    }

    @Override
    public String getContentType() {
        for (HttpHeader header : headers) {
            if (header.is("content-type")) {
                return header.value();
            }
        }
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.add(new HttpHeaders.RawHeader(name, value));
    }

}
