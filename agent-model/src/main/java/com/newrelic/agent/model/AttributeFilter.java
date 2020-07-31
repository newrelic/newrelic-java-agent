/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import java.util.Map;

/**
 * This is a stop-gap way for us to create separation between our model objects
 * and how they are marshaled.  Eventually that responsibility can be removed
 * from the models entirely and this class can disappear or go elsewhere.
 */
public interface AttributeFilter {

    boolean shouldIncludeAgentAttribute(String appName, String attributeName);
    Map<String, ?> filterAgentAttributes(String appName, Map<String, ?> agentAttributes);
    Map<String, ?> filterUserAttributes(String appName, Map<String, ?> userAttributes);

    class PassEverythingAttributeFilter implements AttributeFilter {
        @Override
        public boolean shouldIncludeAgentAttribute(String appName, String attributeName) {
            return true;
        }

        @Override
        public Map<String, ?> filterAgentAttributes(String appName, Map<String, ?> agentAttributes) {
            return agentAttributes;
        }

        @Override
        public Map<String, ?> filterUserAttributes(String appName, Map<String, ?> userAttributes) {
            return userAttributes;
        }
    }
}
