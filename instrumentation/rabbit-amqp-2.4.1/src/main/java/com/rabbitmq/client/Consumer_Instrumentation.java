/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.rabbitmq.client;

import java.io.IOException;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.rabbitamqp241.InboundWrapper;
import com.nr.agent.instrumentation.rabbitamqp241.RabbitAMQPMetricUtil;

@Weave(type = MatchType.Interface, originalName = "com.rabbitmq.client.Consumer")
public abstract class Consumer_Instrumentation {

    @Trace(dispatcher = true)
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        RabbitAMQPMetricUtil.nameTransaction(envelope.getExchange());
        AgentBridge.getAgent().getTransaction().provideHeaders(new InboundWrapper(properties.getHeaders()));
        AgentBridge.getAgent().getTransaction(false).setTransportType(TransportType.AMQP);
        RabbitAMQPMetricUtil.addConsumeAttributes(null, null, envelope.getRoutingKey(), properties);
        Weaver.callOriginal();
    }
}
