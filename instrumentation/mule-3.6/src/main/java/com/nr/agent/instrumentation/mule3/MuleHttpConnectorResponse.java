/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;
import org.mule.module.http.internal.domain.response.ResponseStatus;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * The HTTP Connector was added in Mule 3.6. Verify this implementation is consistent with MuleHttpTransportResponse in
 * the mule-base module, and is identical to this class in the mule-3.7 module.
 */
public class MuleHttpConnectorResponse implements Response {

    private Map headers;
    private ResponseStatus response;

    public MuleHttpConnectorResponse(Map headers, ResponseStatus response) {
        this.headers = headers;
        this.response = response;
    }

    /**
     * Get the first header that matches.
     */
    public String getHeader(String name) {
        if (headers == null) {
            return null;
        } else {
            final Object value = headers.get(name);
            if (value == null || !(value instanceof Collection)) {
                return null;
            }

            final Iterator headerValues = ((Collection) value).iterator();
            if (!headerValues.hasNext()) {
                return null;
            }

            return headerValues.next().toString();
        }
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return response.getReasonPhrase();
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

}
