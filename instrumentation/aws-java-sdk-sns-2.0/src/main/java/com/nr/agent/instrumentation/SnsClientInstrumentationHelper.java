/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class SnsClientInstrumentationHelper {

    public static Segment startSegmentAndReportAsExternal(PublishRequest publishRequest) {
        Transaction transaction = NewRelic.getAgent().getTransaction();
        Segment segment = transaction.startSegment("SNS");
        MessageProduceParameters params = SnsClientInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        segment.reportAsExternal(params);
        return segment;
    }

    public static MessageProduceParameters makeMessageProducerParameters(PublishRequest publishRequest) {
        String destinationName = extractDestination(publishRequest);
        return MessageProduceParameters.library("SNS")
                .destinationType(DestinationType.NAMED_TOPIC)
                .destinationName(destinationName)
                .outboundHeaders(null)
                .build();
    }

    private static String extractDestination(PublishRequest publishRequest) {
        if (publishRequest.topicArn() != null) {
            return publishRequest.topicArn();
        }
        if (publishRequest.targetArn() != null) {
            return publishRequest.targetArn();
        }
        if (publishRequest.phoneNumber() != null) {
            return "PhoneNumber";
        }
        return "Unknown";
    }
}
