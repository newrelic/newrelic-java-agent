/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.amazonaws.services.sns.model.PublishRequest;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageProduceParameters;

public class SNSInstrumentationHelper {

    public static MessageProduceParameters makeMessageProducerParameters(PublishRequest publishRequest) {
        String destinationName = extractDestination(publishRequest);
        return MessageProduceParameters.library("SNS")
                .destinationType(DestinationType.NAMED_TOPIC)
                .destinationName(destinationName)
                .outboundHeaders(null)
                .build();
    }

    private static String extractDestination(PublishRequest publishRequest) {
        if (publishRequest.getTopicArn() != null) {
            return publishRequest.getTopicArn();
        }
        if (publishRequest.getTargetArn() != null) {
            return publishRequest.getTargetArn();
        }
        if (publishRequest.getPhoneNumber() != null) {
            return "PhoneNumber";
        }
        return "Unknown";
    }
}
