/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.messaging;

import com.newrelic.agent.AgentHelper;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.TracedMethod;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MessageMetricsTest {

    private final String hostName;

    TracedMethod mockTracedMethod;

    public MessageMetricsTest() throws UnknownHostException {
        this.hostName = InetAddress.getLocalHost().getHostName();
    }

    @Before
    public void before() throws Exception {
        AgentHelper.bootstrap(AgentHelper.createAgentConfig(true));
        mockTracedMethod = mock(TracedMethod.class);
    }

    @Test
    public void testReplaceEmptyPort() {
        assertEquals(MessageMetrics.UNKNOWN, MessageMetrics.replacePort(null));
        assertEquals(MessageMetrics.UNKNOWN, MessageMetrics.replacePort(-1));
        assertEquals("1234", MessageMetrics.replacePort(1234));
    }

    @Test
    public void testReplaceLocalhost() throws Exception {
        assertEquals(MessageMetrics.UNKNOWN, MessageMetrics.replaceLocalhost(null));

        final String hostname = InetAddress.getLocalHost().getHostName();

        assertEquals(hostname, MessageMetrics.replaceLocalhost("localhost"));
        assertEquals(hostname, MessageMetrics.replaceLocalhost("127.0.0.1"));
        assertEquals(hostname, MessageMetrics.replaceLocalhost("0.0.0.0"));
        assertEquals(hostname, MessageMetrics.replaceLocalhost("0:0:0:0:0:0:0:1"));
        assertEquals(hostname, MessageMetrics.replaceLocalhost("::1"));
        assertEquals(hostname, MessageMetrics.replaceLocalhost("0:0:0:0:0:0:0:0"));
        assertEquals(hostname, MessageMetrics.replaceLocalhost("::"));

        assertEquals("example.com", MessageMetrics.replaceLocalhost("example.com"));
    }

    @Test
    public void testIsAnyEndpointParamsKnown() {
        assertTrue(MessageMetrics.isAnyEndpointParamsKnown("example.com", 1000));
        assertTrue(MessageMetrics.isAnyEndpointParamsKnown("example.com", null));
        assertTrue(MessageMetrics.isAnyEndpointParamsKnown(null, 1000));
        assertFalse(MessageMetrics.isAnyEndpointParamsKnown(null, null));
        assertTrue(MessageMetrics.isAnyEndpointParamsKnown("example.com", -1));
        assertTrue(MessageMetrics.isAnyEndpointParamsKnown("", 1000));
        assertFalse(MessageMetrics.isAnyEndpointParamsKnown("", -1));
    }

    @Test
    public void testCollectMessageProducerRollupMetrics() {
        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.NAMED_TOPIC, "topic");
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Topic/Named/topic", hostName));

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.NAMED_QUEUE, "queue");
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Queue/Named/queue", hostName));

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.TEMP_TOPIC, null);
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Topic/Temp", hostName));

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.TEMP_QUEUE, null);
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Queue/Temp", hostName));

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, "example.com",
                8080, DestinationType.NAMED_QUEUE, "queue");
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/example.com/8080/Queue/Named/queue");

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, null,
                8080, DestinationType.EXCHANGE, "queue");
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/unknown/8080/Exchange/Named/queue");

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, "example.com",
                null, DestinationType.NAMED_QUEUE, "queue");
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/example.com/unknown/Queue/Named/queue");

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, null,
                null, DestinationType.EXCHANGE, null);
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/unknown/unknown/Exchange/unknown");

        MessageMetrics.collectMessageProducerRollupMetrics(mockTracedMethod, "example.com",
                8080, DestinationType.EXCHANGE, null);
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/example.com/8080/Exchange/unknown");
    }

    @Test
    public void testCollectMessageConsumerRollupMetrics() {
        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.NAMED_TOPIC, "topic");
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Topic/Named/topic", hostName));

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.NAMED_QUEUE, "queue");
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Queue/Named/queue", hostName));

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.TEMP_TOPIC, null);
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Topic/Temp", hostName));

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, "localhost",
                8080, DestinationType.TEMP_QUEUE, null);
        verify(mockTracedMethod).addRollupMetricName(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Queue/Temp", hostName));

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, "example.com",
                8080, DestinationType.NAMED_QUEUE, "queue");
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/example.com/8080/Queue/Named/queue");

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, null,
                8080, DestinationType.EXCHANGE, "queue");
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/unknown/8080/Exchange/Named/queue");

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, "example.com",
                null, DestinationType.NAMED_QUEUE, "queue");
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/example.com/unknown/Queue/Named/queue");

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, null,
                null, DestinationType.EXCHANGE, null);
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/unknown/unknown/Exchange/unknown");

        MessageMetrics.collectMessageConsumerRollupMetrics(mockTracedMethod, "example.com",
                8080, DestinationType.EXCHANGE, null);
        verify(mockTracedMethod).addRollupMetricName("MessageBroker/instance/example.com/8080/Exchange/unknown");
    }
}
