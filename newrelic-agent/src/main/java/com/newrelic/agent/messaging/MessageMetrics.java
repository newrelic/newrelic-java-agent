/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.messaging;

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

    public static boolean isAnyEndpointParamsKnown(String host, Integer port) {
        return !(isParamUnknown(host) && isParamUnknown(port));
    }
    public static void collectMessageProducerRollupMetrics(TracedMethod method, String host, Integer port,
            DestinationType destinationType, String destinationName, String amqpRoutingKey) {
        reportInstanceIfEnabled(method, host, port, destinationType, destinationName, null, amqpRoutingKey);
    }

    public static void collectMessageConsumerRollupMetrics(TracedMethod method, String host, Integer port,
            DestinationType destinationType, String destinationName, String amqpQueueName, String amqpRoutingKey) {
        reportInstanceIfEnabled(method, host, port, destinationType, destinationName, amqpQueueName, amqpRoutingKey);
    }

    public static void reportInstanceIfEnabled(TracedMethod method, String host, Integer port,
            DestinationType destinationType, String destinationName, String amqpQueueName, String amqpRoutingKey) {
        MessageBrokerConfig messageBrokerConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getMessageBrokerConfig();
        if (messageBrokerConfig.isInstanceReportingEnabled()) {
            String instanceMetric = buildInstanceMetric(host, port, destinationType, destinationName);
            method.addRollupMetricName(instanceMetric);
            if (isAmqpCall(destinationType, amqpQueueName, amqpRoutingKey)) {
                String amqpInstance = buildAmqpInstanceMetric(instanceMetric, destinationType, amqpQueueName, amqpRoutingKey);
                method.addRollupMetricName(amqpInstance);
            }
        }
    }

    public static String buildInstanceMetric(String host, Integer port,
            DestinationType destinationType, String destinationName) {
        String instance = buildInstanceIdentifier(host, port, destinationType, destinationName);
        return MESSAGE_BROKER_INSTANCE + instance;
    }

    public static String buildInstanceIdentifier(String host, Integer port,
            DestinationType destinationType, String destinationName) {
        String hostname = replaceLocalhost(host);
        String portName = replacePort(port);
        String parsedDestinationName = replaceDestinationName(destinationType, destinationName);

        return hostname + SLASH + portName + SLASH + destinationType.getTypeName() + SLASH + parsedDestinationName;
    }

    public static String buildAmqpInstanceMetric(String instanceMetric, DestinationType destinationType, String amqpQueueName, String amqpRoutingKey) {
        String amqpSuffix = buildAmqpInstance(destinationType, amqpQueueName, amqpRoutingKey);
        return instanceMetric + SLASH + amqpSuffix;
    }

    public static String buildAmqpInstance(DestinationType destinationType, String amqpQueueName, String amqpRoutingKey) {
        if (DestinationType.EXCHANGE.equals(destinationType)) {
            if (!isParamUnknown(amqpQueueName)) {
                return DestinationType.NAMED_QUEUE.getTypeName() + SLASH + amqpQueueName;
            } else if (!isParamUnknown(amqpRoutingKey)) {
                return ROUTING_KEY + SLASH + amqpRoutingKey;
            }
        }
        return "";
    }

    public static String replaceDestinationName(DestinationType destinationType, String destinationName) {
        if (destinationType == DestinationType.TEMP_QUEUE || destinationType == DestinationType.TEMP_TOPIC) {
            return "Temp";
        }
        if (isParamUnknown(destinationName)) {
            return UNKNOWN;
        }
        return "Named" + SLASH + destinationName;
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

    private static boolean isAmqpCall(DestinationType destinationType, String amqpQueueName, String amqpRoutingKey) {
        return DestinationType.EXCHANGE.equals(destinationType) && !(isParamUnknown(amqpQueueName) && isParamUnknown(amqpRoutingKey));
    }

    private static boolean isParamUnknown(String str) {
        return str == null || str.isEmpty();
    }

    private static boolean isParamUnknown(Integer integer) {
        return integer == null || integer == -1;
    }
}
