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
         *
         * 'reasoning_content' is the model's chain-of-thought/thinking text and is exempted for the same reason
         * as 'content'. 'reasoning_content_signature' is an opaque provider continuation token (not semantic
         * content); truncating it would silently corrupt it, making it useless for the audit-replay/cross-turn
         * echo purposes it exists for, so it's exempted as well.
         */
        if (key.equals("content") || key.equals("input") || key.equals("reasoning_content") || key.equals("reasoning_content_signature")) {
            return value;
        }
        String truncatedVal = truncateString(value, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        logTruncatedValue(key, value, truncatedVal, methodCalled, MAX_CUSTOM_EVENT_ATTRIBUTE_SIZE);
        return truncatedVal;
    }
}
