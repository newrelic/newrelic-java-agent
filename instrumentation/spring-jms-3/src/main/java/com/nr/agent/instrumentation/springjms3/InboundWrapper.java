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
import java.util.List;
import java.util.logging.Level;

public class InboundWrapper extends ExtendedInboundHeaders {
    private final Message delegate;

    public InboundWrapper(Message message) {
        super();
        this.delegate = message;
    }

    @Override
    public String getHeader(String name) {
        try {
            return delegate.getStringProperty(name);
        } catch (JMSException e) {
            NewRelic.getAgent().getLogger().log(Level.FINE, e, "Error getting property ({0}) from JMS message.", name);
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        String result = getHeader(name);
        if (result == null) {
            return null;
        }
        return Collections.singletonList(result);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }
}
