/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import java.util.logging.Level;

public class OutboundWrapper implements OutboundHeaders {

    private final Message delegate;

    public OutboundWrapper(Message message) {
        this.delegate = message;
    }

    /**
     * JMS property names should be valid java identifiers, and thus not allowed to contain '-'. Ensure name is valid
     * before setting.
     */
    @Override
    public void setHeader(String name, String value) {
        try {
            delegate.setStringProperty(name, value);
        } catch (JMSException e) {
            NewRelic.getAgent().getLogger().log(Level.FINE, e, "Error setting property ({0}) on JMS message.", name);
        }
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }
}
