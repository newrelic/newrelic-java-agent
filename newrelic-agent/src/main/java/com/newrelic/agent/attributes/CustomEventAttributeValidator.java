/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.service.ServiceFactory;

/**
 * Attribute validator with truncation rules specific to custom events.
 */
public class CustomEventAttributeValidator extends AttributeValidator{
    private static final int MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE = ServiceFactory.getConfigService().getDefaultAgentConfig().getInsightsConfig().getMaxAttributeValue();

    public CustomEventAttributeValidator(String attributeType) {
        super(attributeType);
    }

    @Override
    protected String truncateValue(String key, String value, String methodCalled) {
        String truncatedVal = truncateString(value, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        logTruncatedValue(key, value, truncatedVal, methodCalled, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        return truncatedVal;
    }
}
