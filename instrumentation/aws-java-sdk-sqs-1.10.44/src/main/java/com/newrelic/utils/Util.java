/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;

public class Util {

    public static final String LIBRARY = "SQS";
    public static final String UNKOWN = "unknown";

    public static MessageProduceParameters generateExternalProduceMetrics(String queueUrl) {
        return MessageProduceParameters
                .library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(extractQueueName(queueUrl))
                .outboundHeaders(null)
                .build();
    }

    public static MessageConsumeParameters generateExternalConsumeMetrics(String queueUrl) {
        return MessageConsumeParameters
                .library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(extractQueueName(queueUrl))
                .inboundHeaders(null)
                .build();
    }

    private static String extractQueueName(String queueUrl) {
        int index = queueUrl.lastIndexOf('/');
        if (index > 0) {
            return queueUrl.substring(index + 1);
        }
        return UNKOWN;
    }
}
