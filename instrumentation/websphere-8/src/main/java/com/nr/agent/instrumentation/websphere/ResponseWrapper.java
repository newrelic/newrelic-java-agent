/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.websphere;

import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class ResponseWrapper implements Response {

    private final HttpResponseMessage httpResponse;

    public ResponseWrapper(HttpResponseMessage httpResponse) {
        this.httpResponse = httpResponse;
    }

    @Override
    public int getStatus() throws Exception {
        return httpResponse.getStatusCodeAsInt();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return httpResponse.getReasonPhrase();
    }

    @Override
    public void setHeader(String name, String value) {
        httpResponse.setHeader(name, value);
    }

    @Override
    public String getContentType() {
        return httpResponse.getHeaderAsString(com.ibm.wsspi.http.channel.HttpConstants.HDR_CONTENT_TYPE);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
