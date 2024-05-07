/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms11;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.nr.agent.instrumentation.jms11.integration.JmsProviderTest;
import com.nr.agent.instrumentation.jms11.integration.JmsTestFixture;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.ConnectionFactory;

/**
 * Run the standard JMS instrumentation test suite against the ApacheMQ JMS provider.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.jms11", "com.nr.agent.instrumentation.activemqclient580", "org.apache.activemq" })
public class ApacheMQTest implements JmsProviderTest {
    private static final String MESSAGE_BROKER_URL = "vm://localhost?create=false";
    private static final String QUEUE_NAME = "InstrumentationTestQueue";

    private ConnectionFactory connectionFactory;
    private BrokerService broker;

    @Before
    public void startEmbeddedApacheMQ() throws Exception {
        broker = new BrokerService();
        broker.setBrokerName("localhost");
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.addConnector(MESSAGE_BROKER_URL);
        broker.start();
        connectionFactory = new ActiveMQConnectionFactory(MESSAGE_BROKER_URL);
    }

    @After
    public void stopEmbeddedApacheMQ() throws Exception {
        broker.stop();
    }

    @Test
    @Override
    public void testEchoServer() throws Exception {
        JmsTestFixture.Params params = new JmsTestFixture.Params.Builder()
            .addQueueName(QUEUE_NAME)
            .addSegmentName("MessageBroker/JMS/Queue/Produce/Temp")
            .addSegmentClassName("org.apache.activemq.ActiveMQMessageProducerSupport")
            .addSegmentMethodName("send")
            .build();

        new JmsTestFixture(connectionFactory, params).testEchoServer();
    }
}
