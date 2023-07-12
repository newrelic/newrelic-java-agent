/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.connect.runtime;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionTrace;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka.connect")
public class WorkerSinkTaskInstrumentationTest {

    @Test
    public void noDataTest() {
        WorkerSinkTask workerSinkTask = WorkerSinkTaskUtil.create(emptyMap());

        WorkerSinkTaskUtil.poll(workerSinkTask);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // Transaction was ignored and should not be counted
        assertEquals(0, introspector.getFinishedTransactionCount());
    }

    @Test
    public void pollTest() {
        Map<String, String> data = new HashMap<>();
        data.put("answer", "42");
        data.put("entry", "mostly harmless");
        WorkerSinkTask workerSinkTask = WorkerSinkTaskUtil.create(data);

        WorkerSinkTaskUtil.poll(workerSinkTask);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        String transactionName = "OtherTransaction/Message/Kafka/Connect/connector";
        String pollSegmentName = "Java/org.apache.kafka.connect.runtime.WorkerSinkTask/poll";
        String deliverMessagesSegmentName = "Java/org.apache.kafka.connect.runtime.WorkerSinkTask/deliverMessages";
        String putSegmentName = "Java/com.nr.agent.instrumentation.target.NoopSinkTask/put";
        String transformSegmentName = "Java/com.nr.agent.instrumentation.target.NoopSinkTranformation/apply";
        String converterSegmentName = "Java/org.apache.kafka.connect.storage.StringConverter/toConnectData";

        // asserting transaction traces
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        assertEquals(1, traces.size());

        TraceSegment parentSegment = traces.iterator().next().getInitialTraceSegment();
        assertEquals(pollSegmentName, parentSegment.getName());
        assertEquals(1, parentSegment.getChildren().size());

        TraceSegment deliverMessages = parentSegment.getChildren().iterator().next();
        assertEquals(deliverMessagesSegmentName, deliverMessages.getName());
        assertEquals(1,deliverMessages.getChildren().size());

        TraceSegment put = deliverMessages.getChildren().iterator().next();
        assertEquals(putSegmentName, put.getName());
        assertEquals(0,put.getChildren().size());

        // asserting metrics
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertEquals(1, metricsForTransaction.get(pollSegmentName).getCallCount());
        assertEquals(1, metricsForTransaction.get(deliverMessagesSegmentName).getCallCount());
        assertEquals(1, metricsForTransaction.get(putSegmentName).getCallCount());
        assertEquals(2, metricsForTransaction.get(transformSegmentName).getCallCount());
        assertEquals(4, metricsForTransaction.get(converterSegmentName).getCallCount());
    }
}