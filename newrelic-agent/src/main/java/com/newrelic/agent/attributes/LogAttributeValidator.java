/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.config.ConfigConstant;

/**
 * Attribute validator with truncation rules specific to log events.
 */
public class LogAttributeValidator extends AttributeValidator{
    public LogAttributeValidator(String attributeType) {
        super(attributeType);
    }

    @Override
    protected String truncateValue(String key, String value, String methodCalled) {
        String truncatedVal = truncateString(value, ConfigConstant.MAX_LOG_EVENT_ATTRIBUTE_SIZE);
        logTruncatedValue(key, value, truncatedVal, methodCalled, ConfigConstant.MAX_LOG_EVENT_ATTRIBUTE_SIZE);
        return truncatedVal;
    }
}
