/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.json;

import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Collections;
import java.util.Map;

public class BasicAttributeFilter implements AttributeFilter {

    private final GetMapStrategy strategy;

    public BasicAttributeFilter(GetMapStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public boolean shouldIncludeAgentAttribute(String appName, String attributeName) {
        return strategy.shouldIncludeAttribute(appName, attributeName);
    }

    @Override
    public Map<String, ?> filterAgentAttributes(String appName, Map<String, ?> agentAttributes) {
        return strategy.getFilteredMap(appName, agentAttributes);
    }

    @Override
    public Map<String, ?> filterUserAttributes(String appName, Map<String, ?> userAttributes) {
        // user attributes should have already been filtered for high security - this is just extra protection
        // high security is per an account - meaning it can not be different for various application names within a
        // JVM - so we can just check the default agent config
        if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
            return strategy.getFilteredMap(appName, userAttributes);
        } else {
            return Collections.emptyMap();
        }
    }

    interface GetMapStrategy {
        Map<String, ?> getFilteredMap(String appName, Map<String, ?> input);

        boolean shouldIncludeAttribute(String appName, String attributeName);
    }
}
