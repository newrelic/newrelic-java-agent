/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetricNameUtilTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private KafkaMetric kafkaMetric;

    @Test
    public void buildDisplayName() {
        setupKafkaMetric();
        String actual = MetricNameUtil.buildDisplayName(kafkaMetric);
        assertThat(actual, equalTo("group/name {}"));
    }

    // not testing with more than one tag because iteration order in a Hashmap is not guaranteed.
    @Test
    public void buildDisplayName_withTag() {
        setupKafkaMetric(Tag.TOPIC);
        String actual = MetricNameUtil.buildDisplayName(kafkaMetric);
        assertThat(actual, equalTo("group/name {topic=t}"));
    }

    @Test
    public void buildMetricName() {
        setupKafkaMetric();
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/name"));
    }

    @Test
    public void buildMetricName_withAllTags() {
        setupKafkaMetric(Tag.CLIENT_ID, Tag.TOPIC, Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/topic/t/client/ci/name"));
    }

    @Test
    public void buildMetricName_withClientId() {
        setupKafkaMetric(Tag.CLIENT_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/client/ci/name"));
    }

    @Test
    public void buildMetricName_withTopic() {
        setupKafkaMetric(Tag.TOPIC);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/name"));
    }

    @Test
    public void buildMetricName_withNodeId() {
        setupKafkaMetric(Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/name"));
    }

    @Test
    public void buildMetricName_withClientIdTopic() {
        setupKafkaMetric(Tag.CLIENT_ID, Tag.TOPIC);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/topic/t/client/ci/name"));
    }

    @Test
    public void buildMetricName_withClientIdNodeId() {
        setupKafkaMetric(Tag.CLIENT_ID, Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/node/ni/client/ci/name"));
    }

    @Test
    public void buildMetricName_withTopicNodeId() {
        setupKafkaMetric(Tag.TOPIC, Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric);
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/name"));
    }

    @Test
    public void buildMetricName_nameOverride() {
        setupKafkaMetric();
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/diffName"));
    }

    @Test
    public void buildMetricName_nameOverride_withAllTags() {
        setupKafkaMetric(Tag.CLIENT_ID, Tag.TOPIC, Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/topic/t/client/ci/diffName"));
    }

    @Test
    public void buildMetricName_nameOverride_withClientId() {
        setupKafkaMetric(Tag.CLIENT_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/client/ci/diffName"));
    }

    @Test
    public void buildMetricName_nameOverride_withTopic() {
        setupKafkaMetric(Tag.TOPIC);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/diffName"));
    }

    @Test
    public void buildMetricName_nameOverride_withNodeId() {
        setupKafkaMetric(Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/diffName"));
    }

    @Test
    public void buildMetricName_nameOverride_withClientIdTopic() {
        setupKafkaMetric(Tag.CLIENT_ID, Tag.TOPIC);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/topic/t/client/ci/diffName"));
    }

    @Test
    public void buildMetricName_nameOverride_withClientIdNodeId() {
        setupKafkaMetric(Tag.CLIENT_ID, Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/node/ni/client/ci/diffName"));
    }

    @Test
    public void buildMetricName_nameOverride_withTopicNodeId() {
        setupKafkaMetric(Tag.TOPIC, Tag.NODE_ID);
        String actual = MetricNameUtil.buildMetricName(kafkaMetric, "diffName");
        assertThat(actual, equalTo("MessageBroker/Kafka/Internal/group/diffName"));
    }

    private void setupKafkaMetric(Tag... tags) {
        reset(kafkaMetric);
        when(kafkaMetric.metricName().group())
                .thenReturn("group");
        when(kafkaMetric.metricName().name())
                .thenReturn("name");
        
        Map<String, String> tagMap = new HashMap<>();
        for (Tag tag : tags) {
            tagMap.put(tag.label, tag.value);
        }
        when(kafkaMetric.metricName().tags())
                .thenReturn(tagMap);
    }
    
    private enum Tag {
        CLIENT_ID("client-id", "ci"),
        NODE_ID("node-id", "ni"),
        TOPIC("topic", "t"),
        ;
        private final String label;
        private final String value;

        Tag(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}