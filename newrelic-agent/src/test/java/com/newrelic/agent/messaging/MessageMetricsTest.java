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
    public void testBuildInstanceMetrics() {
        assertEquals(
                MessageFormat.format("MessageBroker/instance/{0}/8080/Topic/Named/topic", hostName),
                MessageMetrics.buildInstanceMetricIfEnabled("localhost", 8080, DestinationType.NAMED_TOPIC, "topic"));

        assertEquals(
                "MessageBroker/instance/broker.com/8080/Topic/Named/topic2",
                MessageMetrics.buildInstanceMetricIfEnabled("broker.com", 8080, DestinationType.NAMED_TOPIC, "topic2"));

        assertEquals(
                "MessageBroker/instance/broker.com/8080/Queue/Named/queue2",
                MessageMetrics.buildInstanceMetricIfEnabled("broker.com", 8080, DestinationType.NAMED_QUEUE, "queue2"));

        assertEquals(
                "MessageBroker/instance/broker.com/8080/Topic/Temp",
                MessageMetrics.buildInstanceMetricIfEnabled( "broker.com", 8080, DestinationType.TEMP_TOPIC, null));

        assertEquals(
                "MessageBroker/instance/broker.com/8081/Queue/Temp",
                MessageMetrics.buildInstanceMetricIfEnabled( "broker.com", 8081, DestinationType.TEMP_QUEUE, null));

        assertEquals(
                "MessageBroker/instance/example.com/8080/Exchange/Named/exchange/Queue/Named/someQueue",
                MessageMetrics.buildInstanceMetricIfEnabled("example.com", 8080,
                        DestinationType.EXCHANGE, "exchange/Queue/Named/someQueue"));

        assertEquals("MessageBroker/instance/unknown/8080/Exchange/Named/exchange",
                MessageMetrics.buildInstanceMetricIfEnabled(null, 8080, DestinationType.EXCHANGE, "exchange"));

        assertEquals("MessageBroker/instance/example.com/unknown/Exchange/Named/exchange",
                MessageMetrics.buildInstanceMetricIfEnabled("example.com", null, DestinationType.EXCHANGE, "exchange"));

        assertEquals("MessageBroker/instance/example.com/8080/Exchange/unknown",
                MessageMetrics.buildInstanceMetricIfEnabled("example.com", 8080, DestinationType.EXCHANGE, null));

        assertEquals("MessageBroker/instance/unknown/unknown/Exchange/unknown",
                MessageMetrics.buildInstanceMetricIfEnabled(null, null, DestinationType.EXCHANGE, null));

    }
}
