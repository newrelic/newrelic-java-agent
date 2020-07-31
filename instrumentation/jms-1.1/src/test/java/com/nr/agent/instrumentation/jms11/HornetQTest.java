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
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.ConnectionFactory;

/**
 * This test will produce the following error if you're looking at stderr:
 * org.hornetq.core.deployers.impl.XmlDeployer deploy
 * ERROR: HQ224005: Unable to deply node [connection-factory: null]
 * javax.naming.NamingException: /nr-factory already has an object bound
 *
 * HornetQ attempts to find resource files. Oddly, the classloader returns
 * an enumeration with 3 of the same URL. The deployer tries to deploy all 3.
 * Then, 1 succeeds and 2 fail with the duplicate name. It seems safe to ignore
 * because everything _is_ actually set up.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.jms11" })
public class HornetQTest implements JmsProviderTest {
    private EmbeddedJMS jmsServer;
    ConnectionFactory factory;
    private static final String QUEUE_NAME = "InstrumentationTestQueue";

    @Before
    public void startEmbeddedHornetQ() throws Exception {
        jmsServer = new EmbeddedJMS();
        jmsServer.start();

        factory = (ConnectionFactory)jmsServer.lookup("/nr-factory");
    }

    @After
    public void stopEmbeddedHornetQ() throws Exception {
        jmsServer.stop();
    }

    @Test
    @Override
    public void testEchoServer() throws Exception {
        JmsTestFixture.Params params = new JmsTestFixture.Params.Builder()
                .addQueueName(QUEUE_NAME)
                .addSegmentName("MessageBroker/JMS/Queue/Produce/Temp")
                .addSegmentClassName("org.hornetq.jms.client.HornetQMessageProducer")
                .addSegmentMethodName("send")
                .build();
        new JmsTestFixture(factory, params).testEchoServer();
    }
}
