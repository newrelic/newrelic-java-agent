/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.json;

import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Map;

public class AttributeFilters {

    public static final AttributeFilter SPAN_EVENTS_ATTRIBUTE_FILTER = new BasicAttributeFilter(new BasicAttributeFilter.GetMapStrategy() {
        @Override
        public Map<String, ?> getFilteredMap(String appName, Map<String, ?> input) {
            return ServiceFactory.getAttributesService().filterSpanEventAttributes(appName, input);
        }

        @Override
        public boolean shouldIncludeAttribute(String appName, String attributeName) {
            return ServiceFactory.getAttributesService().shouldIncludeSpanAttribute(appName, attributeName);
        }
    });

    public static final AttributeFilter ERROR_EVENTS_ATTRIBUTE_FILTER = new BasicAttributeFilter(new BasicAttributeFilter.GetMapStrategy() {
        @Override
        public Map<String, ?> getFilteredMap(String appName, Map<String, ?> input) {
            return ServiceFactory.getAttributesService().filterErrorEventAttributes(appName, input);
        }

        @Override
        public boolean shouldIncludeAttribute(String appName, String attributeName) {
            return ServiceFactory.getAttributesService().shouldIncludeErrorAttribute(appName, attributeName);
        }
    });


}
