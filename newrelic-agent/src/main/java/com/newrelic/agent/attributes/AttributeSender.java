/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;

import java.util.Map;
import java.util.logging.Level;

public abstract class AttributeSender {
    private final AttributeValidator attributeValidator;

    public AttributeSender(AttributeValidator attributeValidator) {
        this.attributeValidator = attributeValidator;
    }

    /**
     * This is used only for logging.
     *
     * @return The type of attribute (e.g. "agent", "custom")
     */
    protected abstract String getAttributeType();

    /**
     * @return The current map of attributes from a given source.
     */
    protected abstract Map<String, Object> getAttributeMap();

    protected void addCustomAttributeImpl(String key, Object value, String methodName) {
        // perform general checks
        Object filteredValue = attributeValidator.verifyParameterAndReturnValue(key, value, methodName);
        // null will be returned if the key/value failed validation
        if (filteredValue == null) {
            return;
        }
        try {
            Map<String, Object> attributeMap = getAttributeMap();
            if (attributeMap != null) {
                attributeMap.put(key, filteredValue);
                Agent.LOG.log(Level.FINER, "Added {0} attribute \"{1}\": {2}", getAttributeType(), key, filteredValue);
                MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_ADD_CUSTOM_PARAMETER);
            }
        } catch (Throwable t) {
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, "Exception adding attribute for key: \"{0}\": {1}", key, t);
            } else if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, "Exception adding attribute for key: \"{0}\": {1}", key);
            }
        }
    }

    protected void addCustomAttributesImpl(Map<String, Object> params, String methodName) {
        // an empty map will be returned if all keys/values fail verification
        Map<String, Object> filteredValues = attributeValidator.verifyParametersAndReturnValues(params, methodName);
        if (filteredValues == null || filteredValues.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> attributeMap = getAttributeMap();
            if (attributeMap != null) {
                attributeMap.putAll(filteredValues);
                Agent.LOG.log(Level.FINER, "Added {0} attributes \"{1}\"", getAttributeType(), filteredValues);
                MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_ADD_CUSTOM_PARAMETER);
            }
        } catch (Throwable t) {
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, "Exception adding attributes for keys: \"{0}\": {1}", filteredValues.keySet(), t);
            } else if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, "Exception adding attributes for keys: \"{0}\": {1}", filteredValues.keySet());
            }
        }
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     */
    public void addAttribute(String key, String value, String methodName) {
        addCustomAttributeImpl(key, value, methodName);
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     */
    public void addAttribute(String key, Number value, String methodName) {
        addCustomAttributeImpl(key, value, methodName);
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     */
    public void addAttribute(String key, Boolean value, String methodName) {
        addCustomAttributeImpl(key, value, methodName);
    }

    /**
     * Add a key with a map of values to the current transaction. These are reported in errors and transaction traces.
     */
    public void addAttribute(String key, Map<String, String> values, String methodName) {
        addCustomAttributeImpl(key, values, methodName);
    }

    /**
     * Add a map of attributes to the current transaction. These are reported in Spans.
     */
    public void addAttributes(Map<String, Object> params, String methodName) {
        addCustomAttributesImpl(params, methodName);
    }

    public <T> T verifyParameterAndReturnValue(String key, T value, String methodCalled) {
        return this.attributeValidator.verifyParameterAndReturnValue(key, value, methodCalled);
    }

    protected Map<String, Object> verifyParametersAndReturnValues(Map<String, Object> params, String methodCalled) {
        return this.attributeValidator.verifyParametersAndReturnValues(params, methodCalled);
    }

    protected void setTransactional(boolean newSetting) {
        this.attributeValidator.setTransactional(newSetting);
    }
}
