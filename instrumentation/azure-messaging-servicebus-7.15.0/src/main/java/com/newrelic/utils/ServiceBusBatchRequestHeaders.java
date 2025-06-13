/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ServiceBusBatchRequestHeaders implements Headers {

    public final static int DEFAULT_MAX_MESSAGE_SIZE = 256*1024; // can be adjusted on a per-batch basis

    private ServiceBusMessage message = null;
    private final Map<String, String> additionalAttributes = new Hashtable<String, String>();

    public ServiceBusBatchRequestHeaders(ServiceBusMessage message) {
        this.message = message;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        Map<String, Object> attributes = message.getApplicationProperties();
        if (attributes != null) {
            Object value = attributes.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> list = new ArrayList<String>();
        String value = getHeader(name);
        if (value != null && !value.isEmpty()) {
            list.add(value);
        }
        return list;
    }

    @Override
    public void setHeader(String name, String value) {
        if (message != null) {
            if (!message.getApplicationProperties().containsKey(name)) {
                additionalAttributes.put(name, value);
            }
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (message != null) {
            if (!message.getApplicationProperties().containsKey(name)) {
                additionalAttributes.put(name, value);
            }
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        if (message != null) {
            Map<String, Object> attributes = message.getApplicationProperties();
            if (attributes != null) {
                return attributes.keySet();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean containsHeader(String name) {
        return getHeaderNames().contains(name);
    }

    public ServiceBusMessage tryToUpdateHeaders() {
        if (message.getBody().getLength() + ServiceBusUtil.NR_DT_HEADER_SIZE > ServiceBusBatchRequestHeaders.DEFAULT_MAX_MESSAGE_SIZE) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "Unable to add DT headers, not enough space");
            return message;
        }

        addDTHeaders();

        return message;
    }

    public void addDTHeaders() {
        if (additionalAttributes == null) return;
        // only add them if the key doesn't already exist
        for (String key : additionalAttributes.keySet()) {
            if (!message.getApplicationProperties().containsKey(key)) {
                message.getApplicationProperties().put(key, additionalAttributes.get(key));
            }
        }
    }
}
