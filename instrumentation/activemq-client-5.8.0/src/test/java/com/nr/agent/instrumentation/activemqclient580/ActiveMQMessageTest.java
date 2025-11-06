/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.activemqclient580;

import com.newrelic.agent.bridge.messaging.BrokerInstance;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.transport.Transport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.JMSException;

import static com.newrelic.agent.bridge.messaging.JmsProperties.NR_JMS_BROKER_INSTANCE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache" })
public class ActiveMQMessageTest {

    private Transport transport;
    private ActiveMQMessage activeMQMessage;

    @Before
    public void setUp() {
        ActiveMQConnection connection = mock(ActiveMQConnection.class);
        transport = mock(Transport.class);

        when(connection.getTransport()).thenReturn(transport);

        ActiveMQMessage message = new ActiveMQMessage();
        message.setConnection(connection);
        activeMQMessage = spy(message);
    }

    @Test
    public void fromMessageGetUnsetProperty() throws Exception {
        Object object = activeMQMessage.getObjectProperty("unsetProperty");
        assertFalse("Object must not be an instance of BrokerInstance", object instanceof BrokerInstance);
        assertNull("object must be null", object);
    }

    @Test
    public void fromMessageGetHostAndPort() throws Exception {
        final String awsAddress = "ssl://b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com/174.65.25.235:61617";

        final String expectedHost = "b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com";
        final Integer expectedPort = 61617;

        setStubs(awsAddress);

        assertMessage(expectedHost, expectedPort, activeMQMessage, 1);
        // Verify Caching
        assertMessage(expectedHost, expectedPort, activeMQMessage, 2);
    }

    @Test
    public void fromMessageGetHostAndPortWithLocalPort() throws Exception {
        final String awsAddress = "ssl://b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com/174.65.25.235:61617@59925";

        final String expectedHost = "b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com";
        final Integer expectedPort = 61617;

        setStubs(awsAddress);

        assertMessage(expectedHost, expectedPort, activeMQMessage, 1);
        // Verify Caching
        assertMessage(expectedHost, expectedPort, activeMQMessage, 2);
    }

    @Test
    public void fromMessageGetHostAndPortWithLocalHost() throws Exception {
        final String localhostAddress = "tcp://localhost/127.0.0.1:61616@59925";
        final String expectedHost = "localhost";
        final Integer expectedPort = 61616;

        setStubs(localhostAddress);

        assertMessage(expectedHost, expectedPort, activeMQMessage, 1);
        // Verify Caching
        assertMessage(expectedHost, expectedPort, activeMQMessage, 2);
    }

    private void setStubs(String transportString) {
        when(transport.toString()).thenReturn(transportString);
    }

    private void assertMessage(String expectedHost, Integer expectedPort, ActiveMQMessage message, Integer timesGetConnectionCalled) throws JMSException {
        BrokerInstance brokerInstance = (BrokerInstance)message.getObjectProperty(NR_JMS_BROKER_INSTANCE_PROPERTY);
        verify(message, times(timesGetConnectionCalled)).getConnection();
        assertNotNull("Failed to retrieve brokerInstance from ActiveMQ message", brokerInstance);
        assertEquals("Expected host did not match", expectedHost, brokerInstance.getHostName());
        assertEquals("Expected port did not match", expectedPort, brokerInstance.getPort());
    }

}