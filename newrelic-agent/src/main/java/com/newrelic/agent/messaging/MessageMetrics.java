/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.messaging;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.config.MessageBrokerConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.TracedMethod;

public class MessageMetrics {
    public static final String METRIC_NAMESPACE = "MessageBroker";
    public static final String SLASH = "/";
    public static final String MESSAGE_BROKER_INSTANCE = METRIC_NAMESPACE + "/instance/";

    public static final String ROUTING_KEY = "RoutingKey";

    public static final String UNKNOWN = "unknown";
    public static String HOSTNAME = Hostname.getHostname(ServiceFactory.getConfigService().getDefaultAgentConfig());

    public static void reportInstanceMetric(TracedMethod tracedMethod, String host, Integer port,
            DestinationType destinationType, String destination) {
        String instanceMetric = buildInstanceMetricIfEnabled(host, port, destinationType, destination);
        if (instanceMetric != null && !instanceMetric.trim().isEmpty()) {
            tracedMethod.addRollupMetricName(instanceMetric);
        }
    }

    public static String buildInstanceMetricIfEnabled(String host, Integer port,
            DestinationType destinationType, String destination) {
        MessageBrokerConfig messageBrokerConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getMessageBrokerConfig();
        if (messageBrokerConfig.isInstanceReportingEnabled()) {
            return buildInstanceMetric(host, port, destinationType, destination);
        }
        return null;
    }

    public static String buildInstanceMetric(String host, Integer port,
            DestinationType destinationType, String destination) {
        String instance = buildInstanceIdentifier(host, port, destinationType, destination);
        return MESSAGE_BROKER_INSTANCE + instance;
    }

    public static String buildInstanceIdentifier(String host, Integer port,
            DestinationType destinationType, String destination) {
        String hostname = replaceLocalhost(host);
        String portName = replacePort(port);
        String parsedDestination = replaceDestination(destinationType, destination);

        return hostname + SLASH + portName + SLASH + destinationType.getTypeName() + SLASH + parsedDestination;
    }

    public static String buildAmqpDestination(String exchangeName, String queueName, String routingKey) {
        String amqpSuffix = buildAmqpDestinationSuffix(queueName, routingKey);
        if (amqpSuffix != null) {
            return null;
        }
        return DestinationType.EXCHANGE.getTypeName() + SLASH + exchangeName + SLASH + amqpSuffix;
    }

    public static String buildAmqpDestinationSuffix(String queueName, String routingKey) {
        if (!isParamUnknown(queueName)) {
            return DestinationType.NAMED_QUEUE.getTypeName() + SLASH + queueName;
        } else if (!isParamUnknown(routingKey)) {
            return ROUTING_KEY + SLASH + routingKey;
        }
        return null;
    }

    public static String replaceDestination(DestinationType destinationType, String destination) {
        if (destinationType == DestinationType.TEMP_QUEUE || destinationType == DestinationType.TEMP_TOPIC) {
            return "Temp";
        }
        if (isParamUnknown(destination)) {
            return UNKNOWN;
        }
        return "Named" + SLASH + destination;
    }

    public static String replaceLocalhost(String host) {
        if (isParamUnknown(host)) {
            return UNKNOWN;
        }

        if ("localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host) || "::1".equals(host) || "0:0:0:0:0:0:0:0".equals(host)
                || "::".equals(host)) {
            return HOSTNAME;
        }

        return host;
    }

    public static String replacePort(Integer port) {
        if (isParamUnknown(port)) {
            return UNKNOWN;
        }
        return String.valueOf(port);
    }

    private static boolean isParamUnknown(String str) {
        return str == null || str.isEmpty();
    }

    private static boolean isParamUnknown(Integer integer) {
        return integer == null || integer == -1;
    }
}
