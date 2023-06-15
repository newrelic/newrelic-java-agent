/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.connect.source;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.target.StubSourceTask;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka.connect")
public class SourceTaskInstrumentationTest {

    @Test
    public void pollTest() throws InterruptedException {
        startTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = "OtherTransaction/Custom/org.apache.kafka.connect.source.SourceTaskInstrumentationTest/startTx";

        // checking transaction trace
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        assertEquals(1, traces.size());
        TraceSegment initialTraceSegment = traces.iterator().next().getInitialTraceSegment();
        List<TraceSegment> childSegments = initialTraceSegment.getChildren();
        assertEquals(1, childSegments.size());
        TraceSegment traceSegment = childSegments.get(0);
        assertEquals("Java/com.nr.agent.instrumentation.target.StubSourceTask/poll", traceSegment.getName());

        // checking metrics
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/com.nr.agent.instrumentation.target.StubSourceTask/poll"));
    }

    @Trace(dispatcher = true)
    private void startTx() throws InterruptedException {
        SourceTask sourceTask = new StubSourceTask("topic");
        sourceTask.poll();
    }
}