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
import org.junit.Assert;
import org.junit.Test;

public class MetricUtilTest {

    @Test
    public void testGenerateProduceMetricsGoodQueueName() {
        MessageProduceParameters messageProduceParameters = MetricUtil.generateExternalProduceMetrics("path/myQueue");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("myQueue", messageProduceParameters.getDestinationName());
        Assert.assertNull(messageProduceParameters.getCloudAccountId());
        Assert.assertNull(messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateProduceMetricsBadQueueName() {
        MessageProduceParameters messageProduceParameters = MetricUtil.generateExternalProduceMetrics("path");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("unknown", messageProduceParameters.getDestinationName());
        Assert.assertNull(messageProduceParameters.getCloudAccountId());
        Assert.assertNull(messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeMetricsGoodQueueName() {
        MessageConsumeParameters messageConsumeParameters = MetricUtil.generateExternalConsumeMetrics("path/myQueue");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("myQueue", messageConsumeParameters.getDestinationName());
        Assert.assertNull(messageConsumeParameters.getCloudAccountId());
        Assert.assertNull(messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeMetricsBadQueueName() {
        MessageConsumeParameters messageConsumeParameters = MetricUtil.generateExternalConsumeMetrics("path");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("unknown", messageConsumeParameters.getDestinationName());
        Assert.assertNull(messageConsumeParameters.getCloudAccountId());
        Assert.assertNull(messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeAwsUrl() {
        MessageConsumeParameters messageConsumeParameters = MetricUtil.generateExternalConsumeMetrics("https://sqs.us-east-2.amazonaws.com/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageConsumeParameters.getDestinationName());
        Assert.assertEquals("123456789012", messageConsumeParameters.getCloudAccountId());
        Assert.assertEquals("us-east-2", messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeOtherUrl() {
        MessageConsumeParameters messageConsumeParameters = MetricUtil.generateExternalConsumeMetrics("https://localhost/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageConsumeParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageConsumeParameters.getDestinationName());
        Assert.assertNull(messageConsumeParameters.getCloudAccountId());
        Assert.assertNull(messageConsumeParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateProduceAwsUrl() {
        MessageProduceParameters messageProduceParameters = MetricUtil.generateExternalProduceMetrics("https://sqs.us-east-2.amazonaws.com/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageProduceParameters.getDestinationName());
        Assert.assertEquals("123456789012", messageProduceParameters.getCloudAccountId());
        Assert.assertEquals("us-east-2", messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateProduceOtherUrl() {
        MessageProduceParameters messageProduceParameters = MetricUtil.generateExternalProduceMetrics("https://localhost/123456789012/MyQueue");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("aws_sqs", messageProduceParameters.getOtelLibrary());
        Assert.assertEquals("MyQueue", messageProduceParameters.getDestinationName());
        Assert.assertNull(messageProduceParameters.getCloudAccountId());
        Assert.assertNull(messageProduceParameters.getCloudRegion());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }
}
