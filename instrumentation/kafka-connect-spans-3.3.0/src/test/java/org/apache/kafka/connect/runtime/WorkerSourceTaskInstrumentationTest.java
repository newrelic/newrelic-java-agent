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
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka.connect")
public class WorkerSourceTaskInstrumentationTest {

    private static final String TOPIC = "topic";
    private static final int PARTITION = 42;
    private static final TopicPartition TOPIC_PARTITION = new TopicPartition(TOPIC, PARTITION);

    @Test
    public void noDataTest() throws InterruptedException {
        WorkerSourceTask workerSourceTask = WorkerSourceTaskUtil.create();
        WorkerSourceTaskUtil.fakeRun(workerSourceTask);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // Transaction was ignored and should not be counted
        assertEquals(0, introspector.getFinishedTransactionCount());
    }


    @Test
    public void pollTest() throws InterruptedException {
        WorkerSourceTask workerSourceTask = WorkerSourceTaskUtil.create("sep", "field");
        WorkerSourceTaskUtil.fakeRun(workerSourceTask);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        String transactionName = "OtherTransaction/Message/Kafka/Connect/connector";
        String pollSegmentName = "Java/org.apache.kafka.connect.runtime.WorkerSourceTask/poll";
        String deliverMessagesSegmentName = "Java/org.apache.kafka.connect.runtime.WorkerSourceTask/sendRecords";
        String sourceTaskPollSegmentName = "Java/com.nr.agent.instrumentation.target.StubSourceTask/poll";
        String transformSegmentName = "Java/com.nr.agent.instrumentation.target.NoopSourceTranformation/apply";
        String converterSegmentName = "Java/org.apache.kafka.connect.storage.StringConverter/fromConnectData";

        // asserting transaction traces
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        assertEquals(1, traces.size());

        TraceSegment rootSegment = traces.iterator().next().getInitialTraceSegment();
        assertEquals(pollSegmentName, rootSegment.getName());
        assertEquals(2, rootSegment.getChildren().size());

        Set<String> childrenSegmentNames = new HashSet<>();
        childrenSegmentNames.add(sourceTaskPollSegmentName);
        childrenSegmentNames.add(deliverMessagesSegmentName);
        Set<String> seenSegmentNames = new HashSet<>();
        for (TraceSegment child : rootSegment.getChildren()) {
            seenSegmentNames.add(child.getName());
            assertEquals(0, child.getChildren().size());
        }
        assertEquals(childrenSegmentNames, seenSegmentNames);

        // asserting metrics
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertEquals(1, metricsForTransaction.get(pollSegmentName).getCallCount());
        assertEquals(1, metricsForTransaction.get(deliverMessagesSegmentName).getCallCount());
        assertEquals(1, metricsForTransaction.get(sourceTaskPollSegmentName).getCallCount());
        assertEquals(2, metricsForTransaction.get(transformSegmentName).getCallCount());
        assertEquals(4, metricsForTransaction.get(converterSegmentName).getCallCount());
    }
}