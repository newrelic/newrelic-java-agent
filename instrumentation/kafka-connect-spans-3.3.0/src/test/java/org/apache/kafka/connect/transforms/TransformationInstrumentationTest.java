/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.connect.transforms;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.target.NoopSourceTranformation;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka.connect")
public class TransformationInstrumentationTest {

    @Test
    public void testSpan() throws InterruptedException {
        startTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = "OtherTransaction/Custom/org.apache.kafka.connect.transforms.TransformationInstrumentationTest/startTx";

        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        assertEquals(1, traces.size());
        TraceSegment trace = traces.iterator().next().getInitialTraceSegment();
        // The annotation in the converter prevents it from showing in the transaction trace
        assertTrue(trace.getChildren().isEmpty());

        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/com.nr.agent.instrumentation.target.NoopSourceTranformation/apply"));
    }

    @Trace(dispatcher = true)
    private void startTx() {
        NoopSourceTranformation noopTranformation = new NoopSourceTranformation();

        SourceRecord record = mock(SourceRecord.class);
        noopTranformation.apply(record);
    }
}