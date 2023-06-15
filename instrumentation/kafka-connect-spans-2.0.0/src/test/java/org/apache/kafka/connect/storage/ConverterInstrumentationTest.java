/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.connect.storage;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.target.NoopConverter;
import org.apache.kafka.connect.data.Schema;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka.connect")
public class ConverterInstrumentationTest {

    @Test
    public void fromConnectDataTest() throws InterruptedException {
        fromTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = "OtherTransaction/Custom/org.apache.kafka.connect.storage.ConverterInstrumentationTest/fromTx";

        // checking transaction trace
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        assertEquals(1, traces.size());
        TraceSegment trace = traces.iterator().next().getInitialTraceSegment();
        // The annotation in the converter prevents it from showing in the transaction trace
        assertTrue(trace.getChildren().isEmpty());

        // checking metrics
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/com.nr.agent.instrumentation.target.NoopConverter/fromConnectData"));
    }

    @Test
    public void toConnectDataTest() throws InterruptedException {
        toTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = "OtherTransaction/Custom/org.apache.kafka.connect.storage.ConverterInstrumentationTest/toTx";

        // checking transaction trace
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        assertEquals(1, traces.size());
        TraceSegment trace = traces.iterator().next().getInitialTraceSegment();
        // The annotation in the converter prevents it from showing in the transaction trace
        assertTrue(trace.getChildren().isEmpty());

        // checking metrics
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/com.nr.agent.instrumentation.target.NoopConverter/toConnectData"));
    }


    @Trace(dispatcher = true)
    private void fromTx() {
        NoopConverter converter = new NoopConverter();
        converter.fromConnectData("topic", Schema.STRING_SCHEMA, "value");
    }

    @Trace(dispatcher = true)
    private void toTx() {
        NoopConverter converter = new NoopConverter();
        converter.toConnectData("topic", new byte[0]);
    }

}