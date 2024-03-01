/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.springjms3;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InboundWrapper extends ExtendedInboundHeaders {
    private final Map<String, String> properties;

    public InboundWrapper(Message message) {
        super();

        properties = new HashMap<>();
        if (message != null) {
            Enumeration<String> names = null;
            try {
                names = message.getPropertyNames();
            } catch (JMSException e) {
                NewRelic.getAgent().getLogger().log(Level.FINE, e, "Error getting property names from JMS message.");
            }
            String name = null;
            try {
                while (names != null && names.hasMoreElements()) {
                    name = names.nextElement();
                    properties.put(name, message.getStringProperty(name));
                }
            } catch (JMSException e) {
                NewRelic.getAgent().getLogger().log(Level.FINE, e, "Error getting property ({0}) from JMS message.", name);
            }
        }
    }

    @Override
    public String getHeader(String name) {
        if (properties == null || name == null) return null;

        return properties.get(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        String header = getHeader(name);
        return header != null ? Collections.singletonList(header) : null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }
}
