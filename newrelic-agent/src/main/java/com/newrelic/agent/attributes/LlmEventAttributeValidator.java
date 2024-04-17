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
    private static final int MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE = ServiceFactory.getConfigService()
            .getDefaultAgentConfig()
            .getInsightsConfig()
            .getMaxAttributeValue();

    public LlmEventAttributeValidator(String attributeType) {
        super(attributeType);
    }

    @Override
    protected String truncateValue(String key, String value, String methodCalled) {
        /*
         * The 'input' and output 'content' attributes should be added to LLM events
         * without being truncated as per the LLMs agent spec. This is because the
         * backend will use these attributes to calculate LLM token usage in cases
         * where token counts aren't available on LLM events.
         */
        if (key.equals("content") || key.equals("input")) {
            return value;
        }
        String truncatedVal = truncateString(value, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        logTruncatedValue(key, value, truncatedVal, methodCalled, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        return truncatedVal;
    }
}
