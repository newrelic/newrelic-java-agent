/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.connect.runtime;

import com.nr.agent.instrumentation.target.NoopSourceTranformation;
import com.nr.agent.instrumentation.target.StubSourceTask;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.MetricNameTemplate;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.runtime.distributed.ClusterConfigState;
import org.apache.kafka.connect.runtime.errors.Operation;
import org.apache.kafka.connect.runtime.errors.RetryWithToleranceOperator;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.CloseableOffsetStorageReader;
import org.apache.kafka.connect.storage.MemoryStatusBackingStore;
import org.apache.kafka.connect.storage.OffsetStorageWriter;
import org.apache.kafka.connect.storage.StringConverter;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.util.ConnectorTaskId;
import org.apache.kafka.connect.util.TopicAdmin;
import org.apache.kafka.connect.util.TopicCreationGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkerSourceTaskUtil  {
    private static final String TOPIC = "everything";



    public static WorkerSourceTask create(String ... polledValues) {
        KafkaProducer<byte[], byte[]> producer = mock(KafkaProducer.class);
        ConnectMetrics connectMetrics = mockConnectMetrics();
        List<Transformation<SourceRecord>> transformations = singletonList(new NoopSourceTranformation());
        RetryWithToleranceOperator retryWithToleranceOperator = mock(RetryWithToleranceOperator.class);
        when(retryWithToleranceOperator.execute(any(), any(),any()))
                .thenAnswer(invocation -> {
                    Operation<?> supplier = invocation.getArgument(0, Operation.class);
                    return supplier.call();
                });
        TopicCreationGroup topicCreationGroup = mock(TopicCreationGroup.class);
        WorkerSourceTask workerSourceTask = new WorkerSourceTask(
                new ConnectorTaskId("connector", 1),
                new StubSourceTask(TOPIC, polledValues),
                mock(TaskStatus.Listener.class),
                TargetState.STARTED,
                new StringConverter(),
                new StringConverter(),
                new StringConverter(),
                new TransformationChain<>(transformations, retryWithToleranceOperator),
                producer,
                mock(TopicAdmin.class),
                singletonMap(TOPIC, topicCreationGroup),
                mock(CloseableOffsetStorageReader.class),
                mock(OffsetStorageWriter.class),
                mock(WorkerConfig.class),
                mock(ClusterConfigState.class),
                connectMetrics,
                WorkerSourceTaskUtil.class.getClassLoader(),
                Time.SYSTEM,
                retryWithToleranceOperator,
                new MemoryStatusBackingStore(),
                Executors.newSingleThreadExecutor()
                );
        workerSourceTask.initialize(mock(TaskConfig.class));

        return workerSourceTask;
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

    public static void fakeRun(WorkerSourceTask workerSourceTask) throws InterruptedException {
        try {
            List<SourceRecord> data = (List<SourceRecord>) poll.invoke(workerSourceTask);
            toSend.set(workerSourceTask, data);
            sendRecords.invoke(workerSourceTask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Method poll;
    private static final Method sendRecords;
    public static final Field toSend;

    private static Method getMethod(String methodName) throws NoSuchMethodException {
        Method method = WorkerSourceTask.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
    }
    static {
        try {
            poll = getMethod("poll");
            sendRecords = getMethod("sendRecords");
            toSend = WorkerSourceTask.class.getDeclaredField("toSend");
            toSend.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
