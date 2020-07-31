/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms11;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.jms.JMSException;
import javax.jms.Message;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.NewRelic;

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
        String header = getHeader(name);
        return header != null ? Collections.singletonList(header) : null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }
}
