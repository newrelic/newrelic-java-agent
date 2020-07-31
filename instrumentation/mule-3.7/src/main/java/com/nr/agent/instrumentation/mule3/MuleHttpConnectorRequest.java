/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import org.mule.api.MuleEvent;
import org.mule.module.http.internal.domain.request.HttpRequestContext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The HTTP Connector was added in Mule 3.6. Verify this implementation is consistent with MuleHttpTransportRequest in
 * the mule-base module, and is identical to this class in the mule-3.6 module.
 */
public class MuleHttpConnectorRequest extends ExtendedRequest {

    private MuleEvent muleEvent;
    private HttpRequestContext requestContext;

    public MuleHttpConnectorRequest(MuleEvent muleEvent, HttpRequestContext requestContext) {
        this.muleEvent = muleEvent;
        this.requestContext = requestContext;
    }

    @Override
    public String getMethod() {
        return requestContext.getRequest().getMethod();
    }

    @Override
    public String getRequestURI() {
        return requestContext.getRequest().getUri();
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        final Map paramMap = muleEvent.getMessage().getInboundProperty("http.query.params");
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
        final Map paramMap = muleEvent.getMessage().getInboundProperty("http.query.params");

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
        Map headerMap = muleEvent.getMessage().getInboundProperty("http.headers");
        if (headerMap == null || headerMap.get(name) == null) {
            Object inboundProperty = muleEvent.getMessage().getInboundProperty(name.toLowerCase());
            if (inboundProperty != null) {
                return inboundProperty.toString();
            } else {
                return null;
            }
        }

        return headerMap.get(name).toString();
    }

    @Override
    public List<String> getHeaders(String name) {
        return Collections.singletonList(getHeader(name));
    }
}
