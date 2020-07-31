/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageProduceParameters;
import org.junit.Test;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import static org.junit.Assert.assertEquals;

public class SnsClientInstrumentationHelperTest {

    @Test
    public void testTopicArnBasedMessage() throws Exception {
        PublishRequest publishRequest = PublishRequest.builder()
                .message("message").topicArn("userSpecifiedTopicArn").subject("VERY IMPORTANT").build();
        MessageProduceParameters result = SnsClientInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        assertEquals("SNS", result.getLibrary());
        assertEquals("userSpecifiedTopicArn", result.getDestinationName());
        assertEquals(DestinationType.NAMED_TOPIC, result.getDestinationType());
    }

    @Test
    public void testTargetArnBasedMessage() throws Exception {
        PublishRequest publishRequest = PublishRequest.builder()
                .message("message").targetArn("userSpecifiedTargetArn").subject("VERY IMPORTANT").build();
        MessageProduceParameters result = SnsClientInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        assertEquals("SNS", result.getLibrary());
        assertEquals("userSpecifiedTargetArn", result.getDestinationName());
        assertEquals(DestinationType.NAMED_TOPIC, result.getDestinationType());
    }

    @Test
    public void testPhoneNumberBasedMessage() throws Exception {
        PublishRequest publishRequest = PublishRequest.builder()
                .message("message").phoneNumber("8675309er").subject("VERY IMPORTANT").build();
        MessageProduceParameters result = SnsClientInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        assertEquals("SNS", result.getLibrary());
        assertEquals("PhoneNumber", result.getDestinationName());
        assertEquals(DestinationType.NAMED_TOPIC, result.getDestinationType());
    }
}