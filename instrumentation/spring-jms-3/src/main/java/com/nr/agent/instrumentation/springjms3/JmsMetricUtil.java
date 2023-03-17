/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.springjms3;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.Topic;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public abstract class JmsMetricUtil {
    private static final String CATEGORY = "Message";

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

    public static void processConsume(Message message, TracedMethod tracer) {
        try {
            DestinationType destinationType = getDestinationType(message.getJMSDestination());
            String destinationName = getDestinationName(message.getJMSDestination());
            tracer.reportAsExternal(MessageConsumeParameters
                    .library("JMS")
                    .destinationType(destinationType)
                    .destinationName(destinationName)
                    .inboundHeaders(new InboundWrapper(message))
                    .build());
        } catch (JMSException exception) {
            NewRelic.getAgent().getLogger().log(Level.FINE, exception,
                    "Unable to record metrics for JMS message consume.");
        }
    }

    private static String getDestinationName(Destination destination) throws JMSException {
        if (destination instanceof TemporaryQueue || destination instanceof TemporaryTopic) {
            return "Temp";
        }

        if (destination instanceof Queue) {
            Queue queue = (Queue) destination;
            return queue.getQueueName();
        }

        if (destination instanceof Topic) {
            Topic topic = (Topic) destination;
            return topic.getTopicName();
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
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                                false, CATEGORY, "JMS/Queue/Named", queue.getQueueName());
                    }
                } else if (dest instanceof Topic) {
                    Topic topic = (Topic) dest;
                    if (topic instanceof TemporaryTopic) {
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW,
                                false, CATEGORY, "JMS/Topic/Temp");
                    } else {
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                                false, CATEGORY, "JMS/Topic/Named", topic.getTopicName());
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

}
