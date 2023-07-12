/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.connect.runtime;

import com.nr.agent.instrumentation.target.NoopSinkTask;
import com.nr.agent.instrumentation.target.NoopSinkTranformation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.MetricNameTemplate;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.runtime.errors.Operation;
import org.apache.kafka.connect.runtime.errors.RetryWithToleranceOperator;
import org.apache.kafka.connect.runtime.errors.WorkerErrantRecordReporter;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.storage.ClusterConfigState;
import org.apache.kafka.connect.storage.MemoryStatusBackingStore;
import org.apache.kafka.connect.storage.StringConverter;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.util.ConnectorTaskId;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkerSinkTaskUtil {
    private static final String TOPIC = "topic";
    private static final int PARTITION = 42;
    private static final TopicPartition TOPIC_PARTITION = new TopicPartition(TOPIC, PARTITION);
    private static final Method poll;
    static {
        try {
            poll = WorkerSinkTask.class.getDeclaredMethod("poll",long.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        poll.setAccessible(true);
    }

    public static WorkerSinkTask create(Map<String, String> data) {
        KafkaConsumer<byte[], byte[]> consumer = mockKafkaConsumer(data);
        ConnectMetrics connectMetrics = mockConnectMetrics();
        List<Transformation<SinkRecord>> transformations = singletonList(new NoopSinkTranformation());
        RetryWithToleranceOperator retryWithToleranceOperator = mock(RetryWithToleranceOperator.class);
        when(retryWithToleranceOperator.execute(any(), any(),any()))
                .thenAnswer(invocation -> {
                    Operation<?> supplier = invocation.getArgument(0, Operation.class);
                    return supplier.call();
                });
        WorkerSinkTask workerSinkTask = new WorkerSinkTask(
                new ConnectorTaskId("connector", 1),
                new NoopSinkTask(),
                mock(TaskStatus.Listener.class),
                TargetState.STARTED,
                mock(WorkerConfig.class),
                mock(ClusterConfigState.class),
                connectMetrics,
                new StringConverter(),
                new StringConverter(),
                new StringConverter(),
                new TransformationChain<>(transformations, retryWithToleranceOperator),
                consumer,
                WorkerSinkTaskUtil.class.getClassLoader(),
                Time.SYSTEM,
                retryWithToleranceOperator,
                mock(WorkerErrantRecordReporter.class),
                new MemoryStatusBackingStore());
        workerSinkTask.initialize(mock(TaskConfig.class));
        return workerSinkTask;
    }

    private static KafkaConsumer<byte[], byte[]> mockKafkaConsumer(Map<String, String> data) {
        KafkaConsumer<byte[], byte[]> consumer = mock(KafkaConsumer.class);
        List<ConsumerRecord<byte[], byte[]>> records = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            records.add(createRecord(entry));
        }
        Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> consumerMap = records.isEmpty() ?
                emptyMap() : singletonMap(TOPIC_PARTITION, records);
        ConsumerRecords<byte[], byte[]> consumerRecords = new ConsumerRecords<>(consumerMap);
        when(consumer.poll(any()))
                .thenReturn(consumerRecords);
        return consumer;
    }

    private static ConnectMetrics mockConnectMetrics() {
        ConnectMetrics connectMetrics = mock(ConnectMetrics.class);
        ConnectMetrics.MetricGroup metricGroup = mock(ConnectMetrics.MetricGroup.class);
        when(connectMetrics.registry())
                .thenReturn(mock(ConnectMetricsRegistry.class));
        when(connectMetrics.time())
                .thenReturn(Time.SYSTEM);
        when(connectMetrics.group(any(), any()))
                .thenReturn(metricGroup);

        when(metricGroup.metrics())
                .thenReturn(mock(Metrics.class));
        when(metricGroup.sensor(anyString()))
                .thenReturn(mock(Sensor.class));

        when(metricGroup.metricName((MetricNameTemplate) any()))
                .thenReturn(mock(MetricName.class));
        return connectMetrics;
    }

    public static void poll(WorkerSinkTask workerSinkTask) {
        try {
            poll.invoke(workerSinkTask, 100L);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConsumerRecord<byte[], byte[]> createRecord(Map.Entry<String, String> entry) {
        return new ConsumerRecord<>(TOPIC, PARTITION, 1L, entry.getKey().getBytes(), entry.getValue().getBytes());
    }
}
