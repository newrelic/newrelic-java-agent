/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3.integration;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * Hold a Connection and a Session together (along with some other stuff). Similar to a JmsContext in the 2.0 version
 * of the API (which we are not instrumenting here so don't want to pull in).
 */
public class JmsTestSession {

    private final Session session;
    private final Connection connection;
    private final Destination adminQueue;

    public JmsTestSession(ConnectionFactory factory, boolean transacted, int ackMode, String queueName) throws Exception {
        this.connection = factory.createConnection();
        this.session = connection.createSession(transacted, ackMode);
        this.adminQueue = session.createQueue(queueName);
    }

    public void start() throws Exception {
        this.connection.start();
    }

    public void shutdown() throws Exception {
        this.session.close();
        this.connection.close();
    }

    public MessageProducer getProducer(int deliveryModeFlags) throws Exception {
        return getProducer(adminQueue, deliveryModeFlags);
    }

    public MessageProducer getProducer(Destination dest, int deliveryModeFlags) throws Exception {
        MessageProducer result = this.session.createProducer(dest);
        result.setDeliveryMode(deliveryModeFlags);
        return result;
    }

    public MessageConsumer getConsumer() throws Exception {
        return this.session.createConsumer(adminQueue);
    }

    public MessageConsumer getConsumer(Destination dest) throws Exception {
        return session.createConsumer(dest);
    }

    public TextMessage createTextMessage() throws Exception {
        return session.createTextMessage();
    }

    public Destination getTemporaryQueue() throws Exception {
        return session.createTemporaryQueue();
    }
}
