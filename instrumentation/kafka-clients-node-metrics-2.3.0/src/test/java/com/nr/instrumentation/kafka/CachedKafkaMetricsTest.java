/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Measurable;
import org.apache.kafka.common.metrics.stats.Avg;
import org.apache.kafka.common.metrics.stats.CumulativeSum;
import org.apache.kafka.common.metrics.stats.Max;
import org.apache.kafka.common.metrics.stats.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;

import java.util.HashMap;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CachedKafkaMetricsTest {

    @Mock
    private FiniteMetricRecorder finiteMetricRecorder;

    @Test
    public void cachedKafkaVersion() {
        KafkaMetric versionKafkaMetric = createKafkaMetric(KafkaMetricType.VERSION);

        CachedKafkaMetric cachedKafkaMetric = CachedKafkaMetrics.newCachedKafkaMetric(versionKafkaMetric);

        assertThat(cachedKafkaMetric.getClass().getName(),
                equalTo("com.nr.instrumentation.kafka.CachedKafkaMetrics$CachedKafkaVersion"));
        assertThat(cachedKafkaMetric.isValid(), is(true));
        assertThat(cachedKafkaMetric.displayName(),
                equalTo("app-info/version/42"));

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).recordMetric(eq("MessageBroker/Kafka/Internal/app-info/version/42"), eq(1.0f));
    }

    @Test
    public void invalidCachedKafkaMetric() {
        KafkaMetric invalidKafkaMetric = createKafkaMetric(KafkaMetricType.INVALID);

        CachedKafkaMetric cachedKafkaMetric = CachedKafkaMetrics.newCachedKafkaMetric(invalidKafkaMetric);

        assertThat(cachedKafkaMetric.getClass().getName(),
                equalTo("com.nr.instrumentation.kafka.CachedKafkaMetrics$InvalidCachedKafkaMetric"));
        assertThat(cachedKafkaMetric.isValid(), is(false));
        assertThat(cachedKafkaMetric.displayName(),
                equalTo("data/invalid {}"));

        cachedKafkaMetric.report(finiteMetricRecorder);
        verifyNoInteractions(finiteMetricRecorder);
    }

    @Test
    public void cachedKafkaSummary() {
        KafkaMetric summaryKafkaMetric = createKafkaMetric(KafkaMetricType.SUMMARY);

        CachedKafkaMetric cachedKafkaMetric = CachedKafkaMetrics.newCachedKafkaMetric(summaryKafkaMetric);

        assertThat(cachedKafkaMetric.getClass().getName(),
                equalTo("com.nr.instrumentation.kafka.CachedKafkaMetrics$CachedKafkaSummary"));
        assertThat(cachedKafkaMetric.isValid(), is(true));
        assertThat(cachedKafkaMetric.displayName(),
                equalTo("data/summary {}"));

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).recordMetric(eq("MessageBroker/Kafka/Internal/data/summary"), eq(2.0f));
    }

    @Test
    public void cachedKafkaCounter() {
        KafkaMetric counterKafkaMetric = createKafkaMetric(KafkaMetricType.COUNTER);

        CachedKafkaMetric cachedKafkaMetric = CachedKafkaMetrics.newCachedKafkaMetric(counterKafkaMetric);

        assertThat(cachedKafkaMetric.getClass().getName(),
                equalTo("com.nr.instrumentation.kafka.CachedKafkaMetrics$CachedKafkaCounter"));
        assertThat(cachedKafkaMetric.isValid(), is(true));
        assertThat(cachedKafkaMetric.displayName(),
                equalTo("data/something {}"));

        when(finiteMetricRecorder.tryRecordMetric(any(), anyFloat()))
                .thenReturn(true);

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).tryRecordMetric(eq("MessageBroker/Kafka/Internal/data/something"), eq(3.0f));
        verifyNoMoreInteractions(finiteMetricRecorder);

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).tryRecordMetric(eq("MessageBroker/Kafka/Internal/data/something"), eq(4.0f));
        verify(finiteMetricRecorder).incrementCounter(eq("MessageBroker/Kafka/Internal/data/something-counter"), eq(1));
    }

    @Test
    public void cachedKafkaCounterTotal() {
        KafkaMetric counterKafkaMetric = createKafkaMetric(KafkaMetricType.COUNTER_TOTAL);

        CachedKafkaMetric cachedKafkaMetric = CachedKafkaMetrics.newCachedKafkaMetric(counterKafkaMetric);

        assertThat(cachedKafkaMetric.getClass().getName(),
                equalTo("com.nr.instrumentation.kafka.CachedKafkaMetrics$CachedKafkaCounter"));
        assertThat(cachedKafkaMetric.isValid(), is(true));
        assertThat(cachedKafkaMetric.displayName(),
                equalTo("data/something-total {}"));

        when(finiteMetricRecorder.tryRecordMetric(any(), anyFloat()))
                .thenReturn(true);

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).tryRecordMetric(eq("MessageBroker/Kafka/Internal/data/something-total"), eq(4.0f));
        verifyNoMoreInteractions(finiteMetricRecorder);

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).tryRecordMetric(eq("MessageBroker/Kafka/Internal/data/something-total"), eq(5.0f));
        verify(finiteMetricRecorder).incrementCounter(eq("MessageBroker/Kafka/Internal/data/something-counter"), eq(1));
    }

    @Test
    public void cachedKafkaCounterTotalCantTrustValue() {
        KafkaMetric counterKafkaMetric = createKafkaMetric(KafkaMetricType.COUNTER_TOTAL);

        CachedKafkaMetric cachedKafkaMetric = CachedKafkaMetrics.newCachedKafkaMetric(counterKafkaMetric);

        assertThat(cachedKafkaMetric.getClass().getName(),
                equalTo("com.nr.instrumentation.kafka.CachedKafkaMetrics$CachedKafkaCounter"));
        assertThat(cachedKafkaMetric.isValid(), is(true));
        assertThat(cachedKafkaMetric.displayName(),
                equalTo("data/something-total {}"));

        // when this method returns false, it means that the value was not recorded
        // and thus, the increaseCount will not be called.
        when(finiteMetricRecorder.tryRecordMetric(any(), anyFloat()))
                .thenReturn(false);

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).tryRecordMetric(eq("MessageBroker/Kafka/Internal/data/something-total"), eq(4.0f));
        verifyNoMoreInteractions(finiteMetricRecorder);

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).tryRecordMetric(eq("MessageBroker/Kafka/Internal/data/something-total"), eq(5.0f));
        verifyNoMoreInteractions(finiteMetricRecorder);
    }


    @Test
    public void cachedKafkaWithoutMeasurable() {
        KafkaMetric counterKafkaMetric = createKafkaMetric(KafkaMetricType.WITHOUT_MEASURABLE);

        CachedKafkaMetric cachedKafkaMetric = CachedKafkaMetrics.newCachedKafkaMetric(counterKafkaMetric);

        assertThat(cachedKafkaMetric.getClass().getName(),
                equalTo("com.nr.instrumentation.kafka.CachedKafkaMetrics$CachedKafkaSummary"));
        assertThat(cachedKafkaMetric.isValid(), is(true));
        assertThat(cachedKafkaMetric.displayName(),
                equalTo("data/unmeasurable {}"));

        cachedKafkaMetric.report(finiteMetricRecorder);
        verify(finiteMetricRecorder).recordMetric(eq("MessageBroker/Kafka/Internal/data/unmeasurable"), eq(6.0f));
    }

    private KafkaMetric createKafkaMetric(KafkaMetricType metricType) {
        KafkaMetric kafkaMetric = mock(KafkaMetric.class, Mockito.RETURNS_DEEP_STUBS);
        when(kafkaMetric.metricName().name())
                .thenReturn(metricType.metricName);
        when(kafkaMetric.metricName().group())
                .thenReturn(metricType.metricGroup);

        OngoingStubbing<Object> valuesStubbing = when(kafkaMetric.metricValue());
        for (Object value : metricType.values) {
            valuesStubbing = valuesStubbing.thenReturn(value);
        }

        when(kafkaMetric.measurable())
                .thenReturn(metricType.measurable);
        when(kafkaMetric.metricName().tags())
                .thenReturn(new HashMap<>());
        return kafkaMetric;
    }

    /**
     * These are the scenarios being tested and respective values.
     */
    private enum KafkaMetricType {
        VERSION("app-info", "version", new Value(), 42),
        INVALID("data", "invalid", new Max(), "towel"),
        SUMMARY("data", "summary", new Avg(), 2.0f),
        COUNTER("data", "something", new CumulativeSum(), 3, 4),
        COUNTER_TOTAL("data", "something-total", new CumulativeSum(), 4, 5),
        WITHOUT_MEASURABLE("data", "unmeasurable", null, 6),
        ;

        KafkaMetricType(String metricGroup, String metricName, Measurable measurable, Object... values) {
            this.metricGroup = metricGroup;
            this.metricName = metricName;
            this.values = values;
            this.measurable = measurable;
        }

        private final String metricGroup;
        private final String metricName;
        private final Object[] values;
        private final Measurable measurable;
    }
}