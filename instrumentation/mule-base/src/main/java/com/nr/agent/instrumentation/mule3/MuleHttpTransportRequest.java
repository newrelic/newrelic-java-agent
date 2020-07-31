/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import org.mule.api.MuleMessage;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wrapper providing the request properties for a Mule HTTP Transport request.
 */
public class MuleHttpTransportRequest extends ExtendedRequest {

    private final MuleMessage message;

    public MuleHttpTransportRequest(MuleMessage message) {
        this.message = message;
    }

    @Override
    public String getMethod() {
        return message.getInboundProperty("http.method");
    }

    @Override
    public String getRequestURI() {
        return message.getInboundProperty("http.context.uri");
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        final Map paramMap = message.getInboundProperty("http.query.params");
        return new Enumeration() {
            Iterator names = paramMap.keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return names.hasNext();
            }

            @Override
            public Object nextElement() {
                return names.next();
            }
        };
    }

    @Override
    public String[] getParameterValues(String name) {
        final Map paramMap = message.getInboundProperty("http.query.params");

        Object value = paramMap.get(name);
        if (value instanceof List) {
            List inList = (List) value;
            int size = inList.size();
            String[] valArray = new String[size];

            for (int i = 0; i < size; i++) {
                valArray[i] = inList.get(i).toString();
            }

            return valArray;
        } else {
            return new String[] { value.toString() };
        }
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        Map headerMap = message.getInboundProperty("http.headers");
        if (headerMap == null || headerMap.get(name) == null) {
            return null;
        }

        return headerMap.get(name).toString();
    }

    @Override
    public List<String> getHeaders(String name) {
        return Collections.singletonList(getHeader(name));
    }
}
