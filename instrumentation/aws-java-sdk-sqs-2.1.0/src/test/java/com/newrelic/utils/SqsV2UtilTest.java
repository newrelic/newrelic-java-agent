/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.utils.SqsV2Util.DT_MAX_MESSAGE_BYTES_SIZE;

public class SqsV2UtilTest {

    @Test
    public void testGenerateProduceMetricsGoodQueueName() {
        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics("path/myQueue");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("myQueue", messageProduceParameters.getDestinationName());
        Assert.assertNull(messageProduceParameters.getCloudAccountId());
        Assert.assertNull(messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateProduceMetricsBadQueueName() {
        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics("path");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("unknown", messageProduceParameters.getDestinationName());
        Assert.assertNull(messageProduceParameters.getCloudAccountId());
        Assert.assertNull(messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeMetricsGoodQueueName() {
        MessageConsumeParameters messageConsumeParameters = SqsV2Util.generateExternalConsumeMetrics("path/myQueue");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("myQueue", messageConsumeParameters.getDestinationName());
        Assert.assertNull(messageConsumeParameters.getCloudAccountId());
        Assert.assertNull(messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeMetricsBadQueueName() {
        MessageConsumeParameters messageConsumeParameters = SqsV2Util.generateExternalConsumeMetrics("path");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("unknown", messageConsumeParameters.getDestinationName());
        Assert.assertNull(messageConsumeParameters.getCloudAccountId());
        Assert.assertNull(messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeAwsUrl() {
        MessageConsumeParameters messageConsumeParameters = SqsV2Util.generateExternalConsumeMetrics("https://sqs.us-east-2.amazonaws.com/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageConsumeParameters.getDestinationName());
        Assert.assertEquals("123456789012", messageConsumeParameters.getCloudAccountId());
        Assert.assertEquals("us-east-2", messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeOtherUrl() {
        MessageConsumeParameters messageConsumeParameters = SqsV2Util.generateExternalConsumeMetrics("https://localhost/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageConsumeParameters.getDestinationName());
        Assert.assertNull(messageConsumeParameters.getCloudAccountId());
        Assert.assertNull(messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateProduceAwsUrl() {
        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics("https://sqs.us-east-2.amazonaws.com/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageProduceParameters.getDestinationName());
        Assert.assertEquals("123456789012", messageProduceParameters.getCloudAccountId());
        Assert.assertEquals("us-east-2", messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateProduceOtherUrl() {
        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics("https://localhost/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageProduceParameters.getDestinationName());
        Assert.assertNull(messageProduceParameters.getCloudAccountId());
        Assert.assertNull(messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testCanAddDtHeaders_SendMessageRequest_EmptyRequest_ReturnTrue() {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder().build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertTrue(result);
    }

    @Test
    public void testCanAddDtHeaders_SendMessageRequest_8AttributesWithBody_ReturnTrue() {
        Map<String, MessageAttributeValue> attributeValues = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            attributeValues.put(String.valueOf(i), MessageAttributeValue.builder().stringValue(String.valueOf(i)).build());
        }
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .messageBody("Some body")
                .messageAttributes(attributeValues)
                .build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertTrue(result);
    }

    @Test
    public void testCanAddDtHeaders_SendMessageRequest_9Attributes_ReturnFalse() {
        Map<String, MessageAttributeValue> attributeValues = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            attributeValues.put(String.valueOf(i), MessageAttributeValue.builder().stringValue(String.valueOf(i)).build());
        }
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder().messageAttributes(attributeValues).build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertFalse(result);
    }

    @Test
    public void testCanAddDtHeaders_SendMessageRequest_8AttributesBigBody_ReturnFalse() {
        Map<String, MessageAttributeValue> attributeValues = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            attributeValues.put(String.valueOf(i), MessageAttributeValue.builder().stringValue(String.valueOf(i)).build());
        }
        int MESSAGE_BODY_SIZE = DT_MAX_MESSAGE_BYTES_SIZE - 17;

        char[] charArray = new char[MESSAGE_BODY_SIZE];
        Arrays.fill(charArray, 'a');
        String messageBody = new String(charArray);
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .messageBody(messageBody)
                .messageAttributes(attributeValues)
                .build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertFalse(result);
    }

    @Test
    public void testCanAddDtHeaders_SendMessageBatchRequestEntry_Empty_ReturnTrue() {
        SendMessageBatchRequestEntry sendMessageRequest = SendMessageBatchRequestEntry.builder().build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertTrue(result);
    }

    @Test
    public void testCanAddDtHeaders_SendMessageBatchRequestEntry_8AttributesWithBody_ReturnTrue() {
        Map<String, MessageAttributeValue> attributeValues = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            attributeValues.put(String.valueOf(i), MessageAttributeValue.builder().stringValue(String.valueOf(i)).build());
        }
        SendMessageBatchRequestEntry sendMessageRequest = SendMessageBatchRequestEntry.builder()
                .messageBody("Some body")
                .messageAttributes(attributeValues)
                .build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertTrue(result);
    }

    @Test
    public void testCanAddDtHeaders_SendMessageBatchRequestEntry_9Attributes_ReturnFalse() {
        Map<String, MessageAttributeValue> attributeValues = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            attributeValues.put(String.valueOf(i), MessageAttributeValue.builder().stringValue(String.valueOf(i)).build());
        }
        SendMessageBatchRequestEntry sendMessageRequest = SendMessageBatchRequestEntry.builder()
                .messageAttributes(attributeValues)
                .build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertFalse(result);
    }

    @Test
    public void testCanAddDtHeaders_SendMessageBatchRequestEntry_8AttributesBigBody_ReturnFalse() {
        Map<String, MessageAttributeValue> attributeValues = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            attributeValues.put(String.valueOf(i), MessageAttributeValue.builder().stringValue(String.valueOf(i)).build());
        }
        int MESSAGE_BODY_SIZE = DT_MAX_MESSAGE_BYTES_SIZE - 17;

        char[] charArray = new char[MESSAGE_BODY_SIZE];
        Arrays.fill(charArray, 'a');
        String messageBody = new String(charArray);
        SendMessageBatchRequestEntry sendMessageRequest = SendMessageBatchRequestEntry.builder()
                .messageBody(messageBody)
                .messageAttributes(attributeValues)
                .build();
        boolean result = SqsV2Util.canAddDtHeaders(sendMessageRequest);
        Assert.assertFalse(result);
    }
}
