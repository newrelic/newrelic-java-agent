/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import com.newrelic.agent.jmx.create.JmxMetricModifier;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.KafkaMetricGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KafkaProducerJmxValues extends JmxFrameworkValues {

    public static final String PREFIX = "kafka.producer";

    protected static final Pattern BYTES_SENT = Pattern.compile("^JMX/\"(.+)-(.+?)-BytesPerSec\"/");
    protected static final Pattern MESSAGES_SENT = Pattern.compile("^JMX/\"(.+)-(.+?)-MessagesPerSec\"/");
    protected static final Pattern MESSAGES_DROPPED = Pattern.compile("^JMX/\"(.+)-(.+?)-DroppedMessagesPerSec\"/");

    private static final JmxMetricModifier TOPIC_MODIFIER = new JmxMetricModifier() {

        @Override
        public String getMetricName(String fullMetricName) {
            Matcher m = BYTES_SENT.matcher(fullMetricName);
            if (m.matches() && m.groupCount() == 2) {
                return "MessageBroker/Kafka/Topic/Produce/Named/" + m.group(2) + "/Sent/Bytes";
            }
            m = MESSAGES_SENT.matcher(fullMetricName);
            if (m.matches() && m.groupCount() == 2) {
                return "MessageBroker/Kafka/Topic/Produce/Named/" + m.group(2) + "/Sent/Messages";
            }
            m = MESSAGES_DROPPED.matcher(fullMetricName);
            if (m.matches() && m.groupCount() == 2) {
                return "MessageBroker/Kafka/Topic/Produce/Named/" + m.group(2) + "/Dropped Messages";
            }
            return "";
        }
    };

    private static final JmxMetric COUNT = KafkaMetricGenerator.COUNT_MONOTONIC.createMetric("Count");

    private static List<BaseJmxValue> METRICS = new ArrayList<>(1);

    static {

        /*
         * We are pulling three metrics from this: the number of messages dropped per a client id, the number of
         * messages sent per a client id, and the number of bytes sent per a client id. The format for the name portion
         * of the metric is {clientid}-AllTopics[Messages|Bytes|Dropped]PerSec
         * 
         * There are also metrics which include the client and the topic:
         * {clientid}-{topicId}-[Messages|Bytes|Dropped]PerSec We currently do not pull these.
         * 
         * Kafka uses the object com.yammer.metrics.core.Meter which adds the value to the count meaning the count
         * should be the total value.
         */
        METRICS.add(new BaseJmxValue("\"kafka.producer\":type=\"ProducerTopicMetrics\",name=*", "JMX/{name}/",
                TOPIC_MODIFIER, new JmxMetric[] { COUNT }));

    }

    public KafkaProducerJmxValues() {
        super();
    }

    @Override
    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
