/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.servlet;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Request;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ServletUtils {

    private ServletUtils() {
    }

    public static void recordParameters(Transaction tx, Request request) {
        if (tx.isIgnore()) {
            return;
        }

        if (!ServiceFactory.getAttributesService().captureRequestParams(tx.getApplicationName())) {
            return;
        }
        Map<String, String> requestParameters = getRequestParameterMap(request);
        if (requestParameters.isEmpty()) {
            return;
        }
        tx.getPrefixedAgentAttributes().put(AttributeNames.HTTP_REQUEST_PREFIX, requestParameters);
    }

    static Map<String, String> getRequestParameterMap(Request request) {
        Enumeration<?> nameEnumeration = request.getParameterNames();
        if (nameEnumeration == null || !nameEnumeration.hasMoreElements()) {
            return Collections.emptyMap();
        }
        Map<String, String> requestParameters = new HashMap<>();

        while (nameEnumeration.hasMoreElements()) {
            String name = nameEnumeration.nextElement().toString();
            if (name.length() > ConfigConstant.MAX_USER_ATTRIBUTE_SIZE) {
                Agent.LOG.log(Level.FINER,
                        "Rejecting request parameter with key \"{0}\" because the key is over the size limit of {1}",
                        name, ConfigConstant.MAX_USER_ATTRIBUTE_SIZE);
            } else {
                String[] values = request.getParameterValues(name);
                String value = getValue(values);
                if (value != null) {
                    requestParameters.put(name, value);
                }
            }
        }
        return requestParameters;
    }

    private static String getValue(String[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        String value = values.length == 1 ? values[0] : Arrays.asList(values).toString();
        if (value != null && value.length() > ConfigConstant.MAX_USER_ATTRIBUTE_SIZE) {
            if (values.length == 1) {
                value = value.substring(0, ConfigConstant.MAX_USER_ATTRIBUTE_SIZE);
            } else {
                value = value.substring(0, ConfigConstant.MAX_USER_ATTRIBUTE_SIZE - 1) + ']';
            }
        }
        return value;
    }
}
