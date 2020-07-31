/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;

public class Util {

    public static final String LIBRARY = "SQS";
    public static final String UNKOWN = "unknown";

    public static MessageProduceParameters generateExternalProduceMetrics(String queueUrl) {
        String queueName = UNKOWN;
        int index = queueUrl.lastIndexOf('/');
        if (index > 0) {
            queueName = queueUrl.substring(index + 1);
        }
        MessageProduceParameters params = MessageProduceParameters
                .library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(queueName)
                .outboundHeaders(null)
                .build();
        return params;
    }

    public static MessageConsumeParameters generateExternalConsumeMetrics(String queueUrl) {
        String queueName = UNKOWN;
        int index = queueUrl.lastIndexOf('/');
        if (index > 0) {
            queueName = queueUrl.substring(index + 1);
        }
        MessageConsumeParameters params = MessageConsumeParameters
                .library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(queueName)
                .inboundHeaders(null)
                .build();
        return params;
    }

    public static void finishSegment(Segment segment) {
        try {
            segment.end();
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
    }
}
