/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.runtime;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.nr.instrumentation.kafka.connect.KafkaConnectMetricsReporter;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.runtime.errors.RetryWithToleranceOperator;
import org.apache.kafka.connect.runtime.errors.ToleranceType;
import org.apache.kafka.connect.runtime.isolation.PluginClassLoader;
import org.apache.kafka.connect.runtime.standalone.StandaloneConfig;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.kafka.connect.storage.ClusterConfigState;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.HeaderConverter;
import org.apache.kafka.connect.storage.StatusBackingStore;
import org.apache.kafka.connect.storage.StringConverter;
import org.apache.kafka.connect.util.ConnectorTaskId;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka")
public class WorkerSinkTaskInstrumentationTest {

    @Test
    public void testReporterAttachesToWorkerSinkTask() {

        // Worker setup
        Time time = Time.SYSTEM;
        Map<String, String> workerProps = new HashMap<>();
        workerProps.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        workerProps.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
        workerProps.put("offset.storage.file.filename", "/tmp/connect.offsets");
        WorkerConfig workerConfig = new StandaloneConfig(workerProps);
        ConnectMetrics metrics = new ConnectMetrics("workerId", workerConfig, Time.SYSTEM, null);

        ConnectorTaskId taskId = new ConnectorTaskId("connector", 0);
        SinkTask sinkTask = mock(SinkTask.class);
        TaskStatus.Listener statusListener = mock(TaskStatus.Listener.class);
        TargetState initialState = TargetState.STARTED;
        Converter keyConverter = new StringConverter();
        Converter valueConverter = new StringConverter();
        HeaderConverter headerConverter = new StringConverter();
        RetryWithToleranceOperator retryOperator = new RetryWithToleranceOperator(0, 60000, ToleranceType.NONE, Time.SYSTEM);
        TransformationChain<SinkRecord> transformationChain = new TransformationChain<>(Collections.emptyList(), retryOperator);
        KafkaConsumer<byte[], byte[]> consumer = mock(KafkaConsumer.class);
        ClassLoader pluginLoader = mock(PluginClassLoader.class);
        StatusBackingStore statusBackingStore = mock(StatusBackingStore.class);
        WorkerTask workerTask = new WorkerSinkTask(
                taskId, sinkTask, statusListener, initialState, workerConfig, ClusterConfigState.EMPTY, metrics,
                keyConverter, valueConverter, headerConverter,
                transformationChain, consumer, pluginLoader, time,
                retryOperator, null, statusBackingStore);

        // this is where the instrumentation applies
        workerTask.initialize(mock(TaskConfig.class));

        // Check that the Metrics Reporter is attached
        List<MetricsReporter> reporters = workerTask.taskMetricsGroup().metricGroup().metrics().reporters();
        List<MetricsReporter> nrReporter = reporters.stream()
                .filter(reporter -> reporter instanceof KafkaConnectMetricsReporter)
                .collect(Collectors.toList());
        assertEquals(nrReporter.size(), 1);
    }
}
