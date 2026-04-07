/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.test.marker.Java25IncompatibleTest;
import com.newrelic.test.marker.Java26IncompatibleTest;
import com.nr.agent.instrumentation.jms3.integration.JmsProviderTest;
import com.nr.agent.instrumentation.jms3.integration.JmsTestFixture;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Run the standard JMS instrumentation test suite against the ApacheMQ JMS provider.
 *
 * This test is disabled for Java 24+ due to ActiveMQ making an API call to the Java Security Manager,
 * which was removed in Java 24.
 *
 * Higher versions of ActiveMQ might remove this call, so this test may be re-enabled in the future
 * (once the agent project is migrated off Java 8).
 */
@RunWith(InstrumentationTestRunner.class)
@Category({ Java25IncompatibleTest.class, Java26IncompatibleTest.class })
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.jms3" })
public class ApacheMQTest implements JmsProviderTest {
    private static final String MESSAGE_BROKER_URL = "vm://localhost?create=false";
    private static final String QUEUE_NAME = "InstrumentationTestQueue";
    private ConnectionFactory connectionFactory;
    private ActiveMQServer server;

    @Before
    public void startEmbeddedApacheMQ() throws Exception {
        server = ActiveMQServers.newActiveMQServer("broker.xml", null, null);
        server.start();
        connectionFactory = new ActiveMQConnectionFactory(MESSAGE_BROKER_URL);
    }

    @After
    public void stopEmbeddedApacheMQ() throws Exception {
        server.stop();
    }

    @Test
    @Override
    public void testEchoServer() throws Exception {
        JmsTestFixture.Params params = new JmsTestFixture.Params.Builder()
                .addQueueName(QUEUE_NAME)
                .addSegmentName("MessageBroker/JMS/Queue/Produce/Temp")
                .addSegmentClassName("org.apache.activemq.artemis.jms.client.ActiveMQMessageProducer")
                .addSegmentMethodName("send")
                .build();

        new JmsTestFixture(connectionFactory, params).testEchoServer();
    }
}
