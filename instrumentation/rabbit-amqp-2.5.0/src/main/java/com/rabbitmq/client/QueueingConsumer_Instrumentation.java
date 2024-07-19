/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.rabbitmq.client;

import com.nr.agent.instrumentation.rabbitamqp250.RabbitAMQPMetricUtil;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.rabbitmq.client.AMQP.BasicProperties;

@Weave(type = MatchType.BaseClass, originalName = "com.rabbitmq.client.QueueingConsumer")
public abstract class QueueingConsumer_Instrumentation {

    public abstract Channel getChannel();

    @Weave(originalName = "com.rabbitmq.client.QueueingConsumer$Delivery")
    public static class Delivery_Instrumentation {
        public BasicProperties getProperties() {
            return Weaver.callOriginal();
        }

        public Envelope getEnvelope() {
            return Weaver.callOriginal();
        }
    }

    @Trace
    public QueueingConsumer.Delivery nextDelivery() {
        QueueingConsumer.Delivery delivery = Weaver.callOriginal();
        Envelope envelope = delivery.getEnvelope();
        BasicProperties props = delivery.getProperties();
        Connection connection = null;
        Channel channel = getChannel();
        if (channel != null) {
            connection = channel.getConnection();
        }
        RabbitAMQPMetricUtil.processGetMessage(null, envelope.getRoutingKey(),
                envelope.getExchange(), props, AgentBridge.getAgent().getTracedMethod(), connection);
        RabbitAMQPMetricUtil.nameTransaction(envelope.getExchange());
        return delivery;
    }

}
