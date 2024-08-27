/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.rabbitamqp270;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public abstract class RabbitAMQPMetricUtil {
    private static final String RABBITMQ = "RabbitMQ";

    private static final String MESSAGE_BROKER_TRANSACTION_EXCHANGE_NAMED = "RabbitMQ/Exchange/Named/{0}";

    private static final String MESSAGE = "Message";
    private static final String DEFAULT = "Default";

    private static final boolean captureSegmentParameters = AgentBridge.getAgent()
            .getConfig()
            .getValue("message_tracer.segment_parameters.enabled", Boolean.TRUE);

    public static void nameTransaction(String exchangeName) {
        String transactionName = MessageFormat.format(MESSAGE_BROKER_TRANSACTION_EXCHANGE_NAMED,
                exchangeName.isEmpty() ? DEFAULT : exchangeName);
        AgentBridge.getAgent()
                .getTransaction()
                .setTransactionName(TransactionNamePriority.FRAMEWORK, false, MESSAGE, transactionName);
    }

    public static void processSendMessage(String exchangeName, String routingKey,
            HashMap<String, Object> headers,
            AMQP.BasicProperties props, TracedMethod tracedMethod, Connection connection) {
        String host = getHost(connection);
        Integer port = getPort(connection);
        tracedMethod.reportAsExternal(MessageProduceParameters
                .library(RABBITMQ)
                .destinationType(DestinationType.EXCHANGE)
                .destinationName(wrapExchange(exchangeName))
                .outboundHeaders(new OutboundWrapper(headers))
                .instance(host, port)
                .build());

        addProduceAttributes(exchangeName, routingKey, props);
    }

    public static void processGetMessage(String queueName, String routingKey, String exchangeName,
            AMQP.BasicProperties properties, TracedMethod tracedMethod, Connection connection) {
        String host = getHost(connection);
        Integer port = getPort(connection);
        tracedMethod.reportAsExternal(MessageConsumeParameters
                .library(RABBITMQ)
                .destinationType(DestinationType.EXCHANGE)
                .destinationName(wrapExchange(exchangeName))
                .inboundHeaders(new InboundWrapper(properties.getHeaders()))
                .instance(host, port)
                .build());

        addConsumeAttributes(exchangeName, queueName, routingKey, properties);
    }

    public static void addConsumeAttributes(String exchangeName, String queueName, String routingKey, AMQP.BasicProperties properties) {
        if (queueName != null && captureSegmentParameters) {
            AgentBridge.privateApi.addTracerParameter("message.queueName", queueName, true);
            // OTel attributes
            AgentBridge.privateApi.addTracerParameter("messaging.destination.name", queueName, true);
            if (exchangeName != null) {
                AgentBridge.privateApi.addTracerParameter("messaging.destination_publish.name", exchangeName, true);
            }
        }
        addAttributes(routingKey, properties);
    }

    public static void addProduceAttributes(String exchangeName, String routingKey, AMQP.BasicProperties properties) {
        if (exchangeName != null && captureSegmentParameters) {
            // OTel attributes
            AgentBridge.privateApi.addTracerParameter("messaging.destination.name", wrapExchange(exchangeName), true);
        }
        addAttributes(routingKey, properties);
    }

    public static String wrapExchange(String exchangeName) {
        return exchangeName.isEmpty() ? DEFAULT : exchangeName;
    }

    public static void queuePurge(String queue, TracedMethod tracedMethod) {
        tracedMethod.setMetricName(MessageFormat.format("MessageBroker/{0}/Queue/Purge/Named/{1}",
                RABBITMQ, queue.isEmpty() ? DEFAULT : queue));
    }

    private static String getHost(Connection connection) {
        String host = null;
        if (connection != null) {
            InetAddress address = connection.getAddress();
            if (address != null) {
                host = address.getHostName();
            }
        }
        return host;
    }

    private static Integer getPort(Connection connection) {
        return (connection != null) ? connection.getPort() : null;
    }

    private static void addAttributes(String routingKey, AMQP.BasicProperties properties) {
        if (!captureSegmentParameters) {
            return;
        }

        AgentBridge.privateApi.addTracerParameter("message.routingKey", routingKey, true);
        // Add Open Telemetry attribute for routing key to be added to spans
        AgentBridge.privateApi.addTracerParameter("messaging.rabbitmq.destination.routing_key", routingKey, true);
        if (properties.getReplyTo() != null) {
            AgentBridge.privateApi.addTracerParameter("message.replyTo", properties.getReplyTo());
        }
        if (properties.getCorrelationId() != null) {
            AgentBridge.privateApi.addTracerParameter("message.correlationId", properties.getCorrelationId());
        }
        if (properties.getHeaders() != null) {
            for (Map.Entry<String, Object> entry : properties.getHeaders().entrySet()) {
                if (entry.getKey().equals("NewRelicTransaction") || entry.getKey().equals("NewRelicID")) {
                    continue;
                }

                AgentBridge.privateApi.addTracerParameter("message.headers." + entry.getKey(), entry.toString());
            }
        }
    }

}
