/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms11;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.messaging.BrokerInstance;
import com.newrelic.agent.bridge.messaging.JmsProperties;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class JmsMetricUtil {

    private static final String CATEGORY = "Message";
    private static final String IBM_QUEUE_PREFIX = "queue:///";
    private static final String IBM_TOPIC_PREFIX = "topic:///";

    private static final Pattern NORMALIZE = Pattern.compile("((?<=[\\W_]|^)([0-9a-fA-F\\.\\-]){4,}(?=[\\W_]|$))");

    public static Message nameTransaction(Message msg) {
        if (msg != null) {
            try {
                Destination dest = msg.getJMSDestination();
                if (dest instanceof Queue) {
                    Queue queue = (Queue) dest;
                    if (queue instanceof TemporaryQueue) {
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW,
                                false, CATEGORY, "JMS/Queue/Temp");
                    } else {
                        String queueName = normalizeName(queue.getQueueName());
                        NewRelic.getAgent().getLogger().log(Level.FINE,
                            "Normalizing queue name: {0}, {1}. ", queue.getQueueName(), queueName);

                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                                false, CATEGORY, "JMS/Queue/Named", queueName);
                    }
                } else if (dest instanceof Topic) {
                    Topic topic = (Topic) dest;
                    if (topic instanceof TemporaryTopic) {
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW,
                                false, CATEGORY, "JMS/Topic/Temp");
                    } else {
                        String topicName = normalizeName(topic.getTopicName());
                        NewRelic.getAgent().getLogger().log(Level.FINEST,
                            "Normalizing topic name: {0}, {1}. ",topic.getTopicName(), topicName);

                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                                false, CATEGORY, "JMS/Topic/Named", topicName);
                    }
                } else {
                    NewRelic.getAgent().getLogger().log(Level.FINE,
                            "Error naming JMS transaction: Invalid Message Type.");
                }
            } catch (JMSException e) {
                NewRelic.getAgent().getLogger().log(Level.FINE, e, "Error naming JMS transaction");
            }
        } else {
            // Not a useful transaction.
            NewRelic.getAgent().getTransaction().ignore();
        }
        return msg;
    }

    public static void saveMessageParameters(Message msg) {
        if (msg != null) {
            AgentBridge.getAgent().getTransaction().saveMessageParameters(getMessageParameters(msg));
        }
    }

    /**
     * Get the message properties as a map of String to Object
     * 
     * @param msg the message holding 0 or more properties
     * @return the map, may be empty, never null
     */
    public static Map<String, String> getMessageParameters(Message msg) {

        Map<String, String> result = new LinkedHashMap<>(1);

        try {
            Enumeration<?> parameterEnum = msg.getPropertyNames();
            if (parameterEnum == null || !parameterEnum.hasMoreElements()) {
                return Collections.emptyMap();
            }

            while (parameterEnum.hasMoreElements()) {
                String key = (String) parameterEnum.nextElement();
                Object val = msg.getObjectProperty(key);
                result.put(key, ((val == null) ? null : val.toString()));
            }
        } catch (JMSException e) {
            NewRelic.getAgent().getLogger().log(Level.FINE, e, "Unable to capture JMS message property");
        }

        return result;
    }

    public static void processSendMessage(Message message, Destination dest, TracedMethod tracer) {
        if (message == null) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "JMS processSendMessage(): message is null");
            return;
        }

        try {
            DestinationType destinationType = getDestinationType(dest);
            String destinationName = getDestinationName(dest == null ? message.getJMSDestination() : dest);
            MessageProduceParameters.Build builder = MessageProduceParameters
                    .library("JMS")
                    .destinationType(destinationType)
                    .destinationName(destinationName)
                    .outboundHeaders(new OutboundWrapper(message));
            BrokerInstance brokerInstance = getHostAndPort(message);
            if (brokerInstance != null) {
                builder = builder.instance(brokerInstance.getHostName(), brokerInstance.getPort());
            }
            tracer.reportAsExternal(builder.build());
        } catch (JMSException exception) {
            NewRelic.getAgent().getLogger().log(Level.FINE, exception,
                    "Unable to record metrics for JMS message produce.");
        }
    }

    public static void processConsume(Message message, TracedMethod tracer) {
        if (message == null) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "JMS processConsume: message is null");
            return;
        }

        try {
            DestinationType destinationType = getDestinationType(message.getJMSDestination());
            String destinationName = getDestinationName(message.getJMSDestination());
            MessageConsumeParameters.Build builder = MessageConsumeParameters
                    .library("JMS")
                    .destinationType(destinationType)
                    .destinationName(destinationName)
                    .inboundHeaders(new InboundWrapper(message));
            BrokerInstance brokerInstance = getHostAndPort(message);
            if (brokerInstance != null) {
                builder = builder.instance(brokerInstance.getHostName(), brokerInstance.getPort());
            }
            tracer.reportAsExternal(builder.build());
        } catch (JMSException exception) {
            NewRelic.getAgent().getLogger().log(Level.FINE, exception,
                    "Unable to record metrics for JMS message consume.");
        }
    }

    private static BrokerInstance getHostAndPort(Message message) throws JMSException {
        Object obj = message.getObjectProperty(JmsProperties.NR_JMS_HOST_AND_PORT_PROPERTY);
        if (obj instanceof BrokerInstance) {
            return (BrokerInstance) obj;
        }
        return null;
    }

    private static String getDestinationName(Destination destination) throws JMSException {

        if (destination instanceof TemporaryQueue || destination instanceof TemporaryTopic) {
            return "Temp";
        }

        if (destination instanceof Queue) {
            Queue queue = (Queue) destination;
            String queueName = queue.getQueueName();
            // IBM MQ returns queue names in the format queue:///
            if (queueName != null && queueName.startsWith(IBM_QUEUE_PREFIX)) {
                    queueName = queueName.substring(IBM_QUEUE_PREFIX.length());
            }
            return queueName;
        }

        if (destination instanceof Topic) {
            Topic topic = (Topic) destination;
            String topicName = topic.getTopicName();
            // IBM MQ returns topic names in the format topic:///
            if (topicName != null && topicName.startsWith(IBM_TOPIC_PREFIX)) {
                    topicName = topicName.substring(IBM_TOPIC_PREFIX.length());
            }
            return topicName;
        }

        return "Unknown";
    }

    private static DestinationType getDestinationType(Destination destination) {
        if (destination instanceof TemporaryQueue) {
            return DestinationType.TEMP_QUEUE;
        } else if (destination instanceof TemporaryTopic) {
            return DestinationType.TEMP_TOPIC;
        } else if (destination instanceof Queue) {
            return DestinationType.NAMED_QUEUE;
        } else {
            return DestinationType.NAMED_TOPIC;
        }
    }

    protected static String normalizeName(String name) {
        Matcher matcher = NORMALIZE.matcher(name);
        return matcher.replaceAll("#");
    }
}
