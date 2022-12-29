/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.logging;

public enum LogAttributeType {
    AGENT(null) {
        @Override
        public String applyPrefix(String key) {
            return key;
        }
    },
    CONTEXT(AppLoggingUtils.CONTEXT_DATA_ATTRIBUTE_PREFIX),
    TAG(AppLoggingUtils.TAGS_ATTRIBUTE_PREFIX);

    private final String prefix;

    LogAttributeType(String prefix) {
        this.prefix = prefix;
    }

    public String applyPrefix(String key) {
        return prefix.concat(key);
    }
}
