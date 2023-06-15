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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.runtime.errors.RetryWithToleranceOperator;
import org.apache.kafka.connect.runtime.errors.ToleranceType;
import org.apache.kafka.connect.runtime.isolation.PluginClassLoader;
import org.apache.kafka.connect.runtime.standalone.StandaloneConfig;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.storage.CloseableOffsetStorageReader;
import org.apache.kafka.connect.storage.ClusterConfigState;
import org.apache.kafka.connect.storage.ConnectorOffsetBackingStore;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.HeaderConverter;
import org.apache.kafka.connect.storage.OffsetStorageWriter;
import org.apache.kafka.connect.storage.StatusBackingStore;
import org.apache.kafka.connect.storage.StringConverter;
import org.apache.kafka.connect.util.ConnectorTaskId;
import org.apache.kafka.connect.util.TopicAdmin;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka")
public class WorkerSourceTaskInstrumentationTest {

    @Test
    public void testReporterAttachesToWorkerSourceTask() {

        // Worker setup
        Time time = Time.SYSTEM;
        Map<String, String> workerProps = new HashMap<>();
        workerProps.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        workerProps.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
        workerProps.put("offset.storage.file.filename", "/tmp/connect.offsets");
        WorkerConfig workerConfig = new StandaloneConfig(workerProps);
        ConnectMetrics metrics = new ConnectMetrics("workerId", workerConfig, Time.SYSTEM, null);

        ConnectorTaskId taskId = new ConnectorTaskId("connector", 0);
        SourceTask sourceTask = mock(SourceTask.class);
        TaskStatus.Listener statusListener = mock(TaskStatus.Listener.class);
        TargetState initialState = TargetState.STARTED;
        Converter keyConverter = new StringConverter();
        Converter valueConverter = new StringConverter();
        HeaderConverter headerConverter = new StringConverter();
        RetryWithToleranceOperator retryOperator = new RetryWithToleranceOperator(0, 60000, ToleranceType.NONE, Time.SYSTEM);
        TransformationChain<SourceRecord> transformationChain = new TransformationChain<>(Collections.emptyList(), retryOperator);
        KafkaProducer<byte[], byte[]> producer = mock(KafkaProducer.class);
        TopicAdmin topicAdmin = mock(TopicAdmin.class);
        CloseableOffsetStorageReader offsetReader = mock(CloseableOffsetStorageReader.class);
        ClassLoader pluginLoader = mock(PluginClassLoader.class);
        StatusBackingStore statusBackingStore = mock(StatusBackingStore.class);
        OffsetStorageWriter offsetWriter = mock(OffsetStorageWriter.class);
        ConnectorOffsetBackingStore offsetStore = mock(ConnectorOffsetBackingStore.class);
        ClusterConfigState clusterConfigState = ClusterConfigState.EMPTY;
        WorkerTask workerTask = new WorkerSourceTask(
                taskId, sourceTask, statusListener, initialState,
                keyConverter, valueConverter, headerConverter,
                transformationChain, producer, topicAdmin, Collections.emptyMap(),
                offsetReader, offsetWriter, offsetStore, workerConfig, clusterConfigState,
                metrics, pluginLoader, time, retryOperator, statusBackingStore, Executors.newSingleThreadExecutor());

        // this is where the instrumentation applies
        workerTask.initialize(mock(TaskConfig.class));

        // Check that the Metrics Reporter is attached
        List<MetricsReporter> reporters = workerTask.taskMetricsGroup().metricGroup().metrics().reporters();
        List<MetricsReporter> nrReporter = reporters.stream()
                .filter(reporter -> reporter instanceof KafkaConnectMetricsReporter)
                .collect(Collectors.toList());
        assertEquals(nrReporter.size(), 1);
        MetricsReporter nrReporter1 = nrReporter.get(0);


        ConnectorTaskId taskId2 = new ConnectorTaskId("connector2", 0);
        WorkerTask workerTask2 = new WorkerSourceTask(
                taskId2, sourceTask, statusListener, initialState,
                keyConverter, valueConverter, headerConverter,
                transformationChain, producer, topicAdmin, Collections.emptyMap(),
                offsetReader, offsetWriter, offsetStore, workerConfig, clusterConfigState,
                metrics, pluginLoader, time, retryOperator, statusBackingStore, Executors.newSingleThreadExecutor());
        workerTask2.initialize(mock(TaskConfig.class));

        reporters = workerTask2.taskMetricsGroup().metricGroup().metrics().reporters();
        nrReporter = reporters.stream()
                .filter(reporter -> reporter instanceof KafkaConnectMetricsReporter)
                .collect(Collectors.toList());
        assertEquals(nrReporter.size(), 1);
        MetricsReporter nrReporter2 = nrReporter.get(0);

        // The reporter should only attach once, since the Connect metrics are shared between workers
        assertSame(nrReporter1, nrReporter2);

    }
}
