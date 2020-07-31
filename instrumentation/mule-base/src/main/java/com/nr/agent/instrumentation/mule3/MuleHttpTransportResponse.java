/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;
import org.apache.commons.httpclient.Header;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

/**
 * Wrapper providing the response properties for a Mule HTTP request.
 */
public class MuleHttpTransportResponse implements Response {

    private final HttpResponse httpResponse;

    public MuleHttpTransportResponse(HttpResponse response) {
        this.httpResponse = response;
    }

    @Override
    public int getStatus() throws Exception {
        return httpResponse.getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return httpResponse.getStatusLine();
    }

    @Override
    public String getContentType() {
        Header contentType = httpResponse.getFirstHeader(HttpConstants.HEADER_CONTENT_TYPE);
        if (contentType == null) {
            return null;
        }

        return contentType.getValue();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        httpResponse.setHeader(new Header(name, value));
    }

    public Header getHeader(String name) {
        return httpResponse.getFirstHeader(name);
    }

}
