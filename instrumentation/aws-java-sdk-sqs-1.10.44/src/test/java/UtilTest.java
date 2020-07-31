/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.utils.Util;
import org.junit.Assert;
import org.junit.Test;

public class UtilTest {

    @Test
    public void testGenerateProduceMetricsGoodQueueName() {
        MessageProduceParameters messageProduceParameters = Util.generateExternalProduceMetrics("path/myQueue");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("myQueue", messageProduceParameters.getDestinationName());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateProduceMetricsBadQueueName() {
        MessageProduceParameters messageProduceParameters = Util.generateExternalProduceMetrics("path");
        Assert.assertEquals("SQS", messageProduceParameters.getLibrary());
        Assert.assertEquals("unknown", messageProduceParameters.getDestinationName());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageProduceParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeMetricsGoodQueueName() {
        MessageConsumeParameters messageConsumeParameters = Util.generateExternalConsumeMetrics("path/myQueue");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("myQueue", messageConsumeParameters.getDestinationName());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }

    @Test
    public void testGenerateConsumeMetricsBadQueueName() {
        MessageConsumeParameters messageConsumeParameters = Util.generateExternalConsumeMetrics("path");
        Assert.assertEquals("SQS", messageConsumeParameters.getLibrary());
        Assert.assertEquals("unknown", messageConsumeParameters.getDestinationName());
        Assert.assertEquals(DestinationType.NAMED_QUEUE, messageConsumeParameters.getDestinationType());
    }
}
