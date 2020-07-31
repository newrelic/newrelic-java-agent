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

/*
 * Kafka reports a lot of consumer metrics. Currently we only report the ones for topics due to a metric explosion fear.
 * There are also metrics for consumer groups and consumer ids.
 */
public class KafkaConsumerJmxValues extends JmxFrameworkValues {

    public static final String PREFIX = "kafka.consumer";

    protected static final Pattern BYTES_RECEIVED = Pattern.compile("^JMX/\"(.+)-(.+?)-BytesPerSec\"/");
    protected static final Pattern MESSAGES_RECEIVED = Pattern.compile("^JMX/\"(.+)-(.+?)-MessagesPerSec\"/");

    private static final JmxMetricModifier TOPIC_MODIFIER = new JmxMetricModifier() {

        @Override
        public String getMetricName(String fullMetricName) {
            Matcher m = BYTES_RECEIVED.matcher(fullMetricName);
            if (m.matches() && m.groupCount() == 2) {
                return "MessageBroker/Kafka/Topic/Consume/Named/" + m.group(2) + "/Received/Bytes";
            }
            m = MESSAGES_RECEIVED.matcher(fullMetricName);
            if (m.matches() && m.groupCount() == 2) {
                return "MessageBroker/Kafka/Topic/Consume/Named/" + m.group(2) + "/Received/Messages";
            }
            return "";
        }
    };

    private static final JmxMetric COUNT = KafkaMetricGenerator.COUNT_MONOTONIC.createMetric("Count");

    private static List<BaseJmxValue> METRICS = new ArrayList<>(1);

    static {
        /*
         * This will report the number of bytes received by the JVM per a topic and the number of messages received by
         * the JVM per a topic. The format of the metric is {clientId}-{topicId}-[Bytes|Messages]PerSec. The clientId is
         * being removed because these contain unique numbers and will cause metric explosion.
         */
        METRICS.add(new BaseJmxValue("\"kafka.consumer\":type=\"ConsumerTopicMetrics\",name=*", "JMX/{name}/",
                TOPIC_MODIFIER, new JmxMetric[] { COUNT }));
    }

    public KafkaConsumerJmxValues() {
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
