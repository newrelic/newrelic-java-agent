/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.Transaction;

import java.util.Map;

public class AgentAttributeSender extends AttributeSender {
    protected static final String ATTRIBUTE_TYPE = "agent";

    public AgentAttributeSender() {
        super(new AttributeValidator(ATTRIBUTE_TYPE));
    }

    @Override
    protected String getAttributeType() {
        return ATTRIBUTE_TYPE;
    }

    @Override
    protected Map<String, Object> getAttributeMap() {
        Transaction currentTxn = Transaction.getTransaction(false);
        if (currentTxn != null) {
            return currentTxn.getAgentAttributes();
        } else {
            return null;
        }
    }

    public void removeAttribute(String key) {
        Map<String, Object> attributeMap = getAttributeMap();
        if (attributeMap == null) {
            return;
        }
        attributeMap.remove(key);
    }

}