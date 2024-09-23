/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.rabbitmq.client.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.rabbitamqp241.RabbitAMQPMetricUtil;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.Connection;

import java.util.HashMap;

@Weave(type = MatchType.ExactClass, originalName = "com.rabbitmq.client.impl.ChannelN")
public abstract class ChannelN_Instrumentation {

    public abstract Connection getConnection();

    @Trace
    public void basicPublish(String exchange, String routingKey, boolean mandatory, boolean immediate,
                             BasicProperties props, byte[] body) {

        if (props == null) {
            props = MessageProperties.MINIMAL_BASIC;
        }

        // Property headers is an Unmodifiable map.
        // Create new map to hold new outbound and existing headers.
        HashMap<String, Object> headers = new HashMap<>();
        if (props.getHeaders() != null) {
            headers.putAll(props.getHeaders());
        }
        RabbitAMQPMetricUtil.processSendMessage(exchange, routingKey, headers, props,
                AgentBridge.getAgent().getTracedMethod(), getConnection());
        props.setHeaders(headers);
        Weaver.callOriginal();
    }

    /*
     * basicGet retrieves messages individually.
     */
    @Trace
    public GetResponse basicGet(String queue, boolean autoAck) {
        GetResponse response = Weaver.callOriginal();
        if (response != null) {
            RabbitAMQPMetricUtil.processGetMessage(queue, response.getEnvelope().getRoutingKey(),
                    response.getEnvelope().getExchange(), response.getProps(),
                    AgentBridge.getAgent().getTracedMethod(), getConnection());
        }
        return response;
    }

    @Trace
    public AMQImpl.Queue.PurgeOk queuePurge(String queue) {
        RabbitAMQPMetricUtil.queuePurge(queue, AgentBridge.getAgent().getTracedMethod());
        return Weaver.callOriginal();
    }
}
