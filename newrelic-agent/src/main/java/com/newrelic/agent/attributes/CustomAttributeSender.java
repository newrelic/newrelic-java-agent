/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Map;
import java.util.logging.Level;

public class CustomAttributeSender extends AttributeSender {
    protected static final String ATTRIBUTE_TYPE = "custom";

    public CustomAttributeSender() {
        super(new AttributeValidator(ATTRIBUTE_TYPE));
    }

    @Override
    protected String getAttributeType() {
        return ATTRIBUTE_TYPE;
    }

    @Override
    protected Map<String, Object> getAttributeMap() {
        Transaction currentTxn = Transaction.getTransaction(false);
        if (currentTxn != null &&
                ServiceFactory.getConfigService().getDefaultAgentConfig().isCustomParametersAllowed()) {
            return currentTxn.getUserAttributes();
        } else {
            return null;
        }
    }

    public <T> T verifyParameterAndReturnValue(String key, T value, String methodCalled) {
        try {
            Transaction currentTxn = Transaction.getTransaction(false);
            if (currentTxn != null) {
                if (currentTxn.getAgentConfig().isHighSecurity()) {
                    Agent.LOG.log(Level.FINER,
                            "Unable to add {0} attribute when {1} was invoked with key \"{2}\" while in high security mode.",
                            getAttributeType(), methodCalled, key);
                    return null;
                }
            }
            if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isCustomParametersAllowed()) {
                Agent.LOG.log(Level.FINER,
                        "Unable to add {0} attribute when {1} was invoked with key \"{2}\" while lasp custom_parameters disabled.",
                        getAttributeType(), methodCalled, key);
                return null;
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST,
                    "Unable to verify attribute. Exception thrown while verifying security policies", t);
            return null;
        }
        return super.verifyParameterAndReturnValue(key, value, methodCalled);
    }

    protected void addCustomAttributeImpl(String key, Object value, String methodName) {
        try {
            Transaction tx = Transaction.getTransaction(false);
            if (tx == null) {
                Agent.LOG.log(Level.FINER,
                        "Unable to add {0} attribute for key \"{1}\" because there is no transaction.",
                        getAttributeType(), key);
                return;
            }

            // check the max size for user attributes
            Map<String, Object> attributeMap = getAttributeMap();
            if (attributeMap != null && attributeMap.size() >= ConfigConstant.MAX_USER_ATTRIBUTES) {
                Agent.LOG.log(Level.FINER, "Unable to add {0} attribute for key \"{1}\" because the limit is {2}.",
                        getAttributeType(), key, ConfigConstant.MAX_USER_ATTRIBUTES);
                return;
            }
            super.addCustomAttributeImpl(key, value, methodName);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, "Exception adding {0} parameter for key: \"{1}\": {2}", getAttributeType(), key,
                    t);
        }
    }
}