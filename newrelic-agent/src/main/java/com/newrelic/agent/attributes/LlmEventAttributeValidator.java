/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.service.ServiceFactory;

/**
 * Attribute validator with truncation rules specific to LLM events.
 */
public class LlmEventAttributeValidator extends AttributeValidator {
    // FIXME different size attribute limits for LLM events InsightsConfigImpl.MAX_MAX_ATTRIBUTE_VALUE ?
    private static final int MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE = ServiceFactory.getConfigService()
            .getDefaultAgentConfig()
            .getInsightsConfig()
            .getMaxAttributeValue();

    public LlmEventAttributeValidator(String attributeType) {
        super(attributeType);
    }

    @Override
    protected String truncateValue(String key, String value, String methodCalled) {
        // TODO make sure that this behavior is accepted into the agent spec
        if (key.equals("content")) {
            return value;
        }
        String truncatedVal = truncateString(value, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        logTruncatedValue(key, value, truncatedVal, methodCalled, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        return truncatedVal;
    }
}
