/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.messaging;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.service.ServiceFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * This class is the contact point between Messaging-specific calls from instrumentation and the generic Transaction
 * object where statistics are recorded.
 */
public final class MessagingUtil {

    /**
     * Record the parameters of the message if allowed by the configuration settings.<br>
     * 
     * @param tx The transaction
     * @param requestParameters the request parameters from the message
     */
    public static void recordParameters(Transaction tx, Map<String, String> requestParameters) {

        // This method is analogous to ServletService.recordParameters, but for messaging.

        if (requestParameters.isEmpty()) {
            return;
        }

        if (tx.isIgnore()) {
            return;
        }
        if (!ServiceFactory.getAttributesService().captureMessageParams(tx.getApplicationName())) {
            return;
        }

        tx.getPrefixedAgentAttributes().put(AttributeNames.MESSAGE_REQUEST_PREFIX,
                filterMessageParameters(requestParameters, ConfigConstant.MAX_USER_ATTRIBUTE_SIZE));
    }

    static Map<String, String> filterMessageParameters(Map<String, String> messageParams, int maxSizeLimit) {
        Map<String, String> atts = new LinkedHashMap<>();
        String value;
        for (Entry<String, String> current : messageParams.entrySet()) {
            if (current.getKey().length() > maxSizeLimit) {
                Agent.LOG.log(Level.FINER,
                        "Rejecting request parameter with key \"{0}\" because the key is over the size limit of {1}",
                        current.getKey(), maxSizeLimit);
            } else {
                value = getValue(current.getValue(), maxSizeLimit);
                if (value != null) {
                    atts.put(current.getKey(), value);
                }
            }
        }

        return atts;
    }

    private static String getValue(String value, int maxSizeLimit) {
        if (value == null || value.length() == 0) {
            return null;
        }
        if (value.length() > maxSizeLimit) {
            value = value.substring(0, maxSizeLimit);
        }
        return value;
    }

}
