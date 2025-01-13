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
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;

public class SqsSpansV2Util {

    public static final String LIBRARY = "SQS";
    public static final String OTEL_LIBRARY = "aws_sqs";
    public static final String[] DT_HEADERS = new String[] {"newrelic","NEWRELIC","NewRelic","tracestate","TraceState","TRACESTATE"};

    public static final boolean RECEIVE_MSG_DT_ENABLED =  NewRelic.getAgent().getConfig().getValue("sqs.receiver_distributed_trace.enabled", true);

    public static MessageProduceParameters generateExternalProduceMetrics(String queueUrl) {
        DestinationData destinationData = DestinationData.parse(queueUrl);

        MessageProduceParameters params = MessageProduceParameters
                .library(LIBRARY, OTEL_LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(destinationData.getQueueName())
                .outboundHeaders(null)
                .cloudRegion(destinationData.getRegion())
                .cloudAccountId(destinationData.getAccountId())
                .build();
        return params;
    }

    public static MessageConsumeParameters generateExternalConsumeMetrics(String queueUrl) {
        DestinationData destinationData = DestinationData.parse(queueUrl);

        MessageConsumeParameters params = MessageConsumeParameters
                .library(LIBRARY, OTEL_LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(destinationData.getQueueName())
                .inboundHeaders(null)
                .cloudRegion(destinationData.getRegion())
                .cloudAccountId(destinationData.getAccountId())
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
