/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.jetty.jakarta;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import org.eclipse.jetty.http.HttpFields;
import java.util.Collection;
import java.util.Collections;

/**
 * Headers implementation for Jetty 12+ API using HttpFields
 * This class adapts the new Jetty HttpFields API to New Relic's Headers interface
 * for distributed tracing support.
 */
public class JettyRequestHeaders implements Headers {
    private final HttpFields headers;
    
    public JettyRequestHeaders(HttpFields headers) {
        this.headers = headers;
    }
    
    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
    
    @Override
    public String getHeader(String name) {
        return headers != null ? headers.get(name) : null;
    }
    
    @Override
    public Collection<String> getHeaders(String name) {
        if (headers == null) {
            return Collections.emptyList();
        }
        return headers.getValuesList(name);
    }
    
    @Override
    public void setHeader(String name, String value) {
        // Not applicable for request headers
    }
    
    @Override
    public void addHeader(String name, String value) {
        // Not applicable for request headers
    }
    
    @Override
    public Collection<String> getHeaderNames() {
        if (headers == null) {
            return Collections.emptyList();
        }
        return headers.getFieldNamesCollection();
    }
    
    @Override
    public boolean containsHeader(String name) {
        return headers != null && headers.contains(name);
    }
}