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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SNSInstrumentationHelperTest {

    @Test
    public void testTopicArnBasedMessage() throws Exception {
        PublishRequest publishRequest = new PublishRequest("userSpecifiedTopicArn", "message", "VERY IMPORTANT");
        MessageProduceParameters result = SNSInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        assertEquals("SNS", result.getLibrary());
        assertEquals("userSpecifiedTopicArn", result.getDestinationName());
        assertEquals(DestinationType.NAMED_TOPIC, result.getDestinationType());
    }

    @Test
    public void testTargetArnBasedMessage() throws Exception {
        PublishRequest publishRequest = new PublishRequest(null, "message", "VERY IMPORTANT");
        publishRequest.setTargetArn("userSpecifiedTargetArn");
        MessageProduceParameters result = SNSInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        assertEquals("SNS", result.getLibrary());
        assertEquals("userSpecifiedTargetArn", result.getDestinationName());
        assertEquals(DestinationType.NAMED_TOPIC, result.getDestinationType());
    }

    @Test
    public void testPhoneNumberBasedMessage() throws Exception {
        PublishRequest publishRequest = new PublishRequest(null, "message", "VERY IMPORTANT");
        publishRequest.setPhoneNumber("8675309er");
        MessageProduceParameters result = SNSInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        assertEquals("SNS", result.getLibrary());
        assertEquals("PhoneNumber", result.getDestinationName());
        assertEquals(DestinationType.NAMED_TOPIC, result.getDestinationType());
    }
}