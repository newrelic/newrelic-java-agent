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
    public static final String SLASH = "/";
    public static final String ROUTING_KEY = "RoutingKey";

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
                .destinationName(exchangeName.isEmpty() ? DEFAULT : exchangeName)
                .outboundHeaders(new OutboundWrapper(headers))
                .build());
        AgentBridge.privateApi.reportMessageBrokerInstance(host, port, DestinationType.EXCHANGE,
                buildInstDestination(exchangeName, null, routingKey));

        addAttributes(routingKey, props);
    }

    public static void processGetMessage(String queueName, String routingKey, String exchangeName,
            AMQP.BasicProperties properties, TracedMethod tracedMethod, Connection connection) {
        String host = getHost(connection);
        Integer port = getPort(connection);
        tracedMethod.reportAsExternal(MessageConsumeParameters
                .library(RABBITMQ)
                .destinationType(DestinationType.EXCHANGE)
                .destinationName(exchangeName.isEmpty() ? DEFAULT : exchangeName)
                .inboundHeaders(new InboundWrapper(properties.getHeaders()))
                .build());
        AgentBridge.privateApi.reportMessageBrokerInstance(host, port, DestinationType.EXCHANGE,
                buildInstDestination(exchangeName, null, routingKey));

        addConsumeAttributes(queueName, routingKey, properties);
    }

    public static void addConsumeAttributes(String queueName, String routingKey, AMQP.BasicProperties properties) {
        if (queueName != null && captureSegmentParameters) {
            AgentBridge.privateApi.addTracerParameter("message.queueName", queueName);
        }
        addAttributes(routingKey, properties);
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

        AgentBridge.privateApi.addTracerParameter("message.routingKey", routingKey);
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

    public static String buildInstDestination(String exchangeName, String queueName, String routingKey) {
        String amqpSuffix = buildAmqpDestinationSuffix(queueName, routingKey);
        if (amqpSuffix == null) {
            return exchangeName;
        }
        return exchangeName + SLASH + amqpSuffix;
    }

    public static String buildAmqpDestinationSuffix(String queueName, String routingKey) {
        if (isParamKnown(queueName)) {
            return DestinationType.NAMED_QUEUE.getTypeName() + SLASH + queueName;
        } else if (isParamKnown(routingKey)) {
            return ROUTING_KEY + SLASH + routingKey;
        }
        return null;
    }

    private static boolean isParamKnown(String str) {
        return str != null && !str.isEmpty();
    }

}
