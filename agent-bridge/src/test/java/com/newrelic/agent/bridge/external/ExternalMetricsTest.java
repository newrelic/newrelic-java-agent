/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class ExternalMetricsTest {

    @Test
    public void testNullHost() throws Exception {
        TracedMethod tracedMethod = Mockito.mock(TracedMethod.class);
        Transaction transaction = Mockito.mock(Transaction.class);

        String nullHost = null;
        ExternalMetrics.makeExternalComponentTrace(transaction,tracedMethod, nullHost, "library", true, "http://uri", "operation");
    }

    @Test
    public void makeExternalComponentMetric_withTracedMethodAndEmptyOperations_callsSetMetricName() {
        TracedMethod tracedMethod = Mockito.mock(TracedMethod.class);
        ExternalMetrics.makeExternalComponentMetric(tracedMethod, "host", "lib", true, "https://www.newrelic.com");

        Mockito.verify(tracedMethod, Mockito.times(1)).setMetricNameFormatInfo("External/host/lib", "External/host/lib", "https://www.newrelic.com");
    }

    @Test
    public void makeExternalComponentMetric_withTracedMethodAndOperations_callsSetMetricName() {
        TracedMethod tracedMethod = Mockito.mock(TracedMethod.class);
        ExternalMetrics.makeExternalComponentMetric(tracedMethod, "host", "lib", true, "https://www.newrelic.com", "op1", "op2");

        Mockito.verify(tracedMethod, Mockito.times(1)).setMetricNameFormatInfo("External/host/lib/op1/op2", "External/host/lib/op1/op2", "https://www.newrelic.com");
    }

    @Test
    public void makeExternalComponentTrace_withWebTxnAndHost_callsAddExclusiveRollupMethods() {
        TracedMethod tracedMethod = Mockito.mock(TracedMethod.class);
        ExternalMetrics.makeExternalComponentTrace(true, tracedMethod, "host", "lib", true, "https://www.newrelic.com", "op1", "op2");

        Mockito.verify(tracedMethod, Mockito.times(1)).setMetricNameFormatInfo("External/host/lib/op1/op2", "External/host/lib/op1/op2", "https://www.newrelic.com");
        Mockito.verify(tracedMethod, Mockito.times(1)).addExclusiveRollupMetricName(ExternalMetrics.ALL);
        Mockito.verify(tracedMethod, Mockito.times(1)).addExclusiveRollupMetricName(ExternalMetrics.ALL_WEB);
        Mockito.verify(tracedMethod, Mockito.times(1)).addExclusiveRollupMetricName("External/host/all");
    }

    @Test
    public void makeExternalComponentTrace_withNoWebTxnAndHost_callsAddExclusiveRollupMethods() {
        TracedMethod tracedMethod = Mockito.mock(TracedMethod.class);
        ExternalMetrics.makeExternalComponentTrace(false, tracedMethod, "host", "lib", true, "https://www.newrelic.com", "op1", "op2");

        Mockito.verify(tracedMethod, Mockito.times(1)).setMetricNameFormatInfo("External/host/lib/op1/op2", "External/host/lib/op1/op2", "https://www.newrelic.com");
        Mockito.verify(tracedMethod, Mockito.times(1)).setMetricNameFormatInfo("External/host/lib/op1/op2", "External/host/lib/op1/op2", "https://www.newrelic.com");
        Mockito.verify(tracedMethod, Mockito.times(1)).addExclusiveRollupMetricName(ExternalMetrics.ALL);
        Mockito.verify(tracedMethod, Mockito.times(1)).addExclusiveRollupMetricName(ExternalMetrics.ALL_OTHER);
        Mockito.verify(tracedMethod, Mockito.times(1)).addExclusiveRollupMetricName("External/host/all");
    }

    @Test
    public void makeExternalComponentTrace_withWebTxnAndNoHost_callsAddExclusiveRollupMethods() {
        TracedMethod tracedMethod = Mockito.mock(TracedMethod.class);
        ExternalMetrics.makeExternalComponentTrace(true, tracedMethod, null, "lib", true, "https://www.newrelic.com", "op1", "op2");

        Mockito.verify(tracedMethod, Mockito.times(1)).setMetricNameFormatInfo("External/UnknownHost/lib/op1/op2", "External/UnknownHost/lib/op1/op2", "https://www.newrelic.com");
        Mockito.verify(tracedMethod, Mockito.times(0)).addExclusiveRollupMetricName(ExternalMetrics.ALL);
        Mockito.verify(tracedMethod, Mockito.times(0)).addExclusiveRollupMetricName(ExternalMetrics.ALL_WEB);
        Mockito.verify(tracedMethod, Mockito.times(0)).addExclusiveRollupMetricName("External/UnknownHost/all");
    }

    @Test
    public void makeExternalComponentTrace_withNoWebTxnAndNoHost_callsAddExclusiveRollupMethods() {
        TracedMethod tracedMethod = Mockito.mock(TracedMethod.class);
        ExternalMetrics.makeExternalComponentTrace(false, tracedMethod, null, "lib", true, "https://www.newrelic.com", "op1", "op2");

        Mockito.verify(tracedMethod, Mockito.times(1)).setMetricNameFormatInfo("External/UnknownHost/lib/op1/op2", "External/UnknownHost/lib/op1/op2", "https://www.newrelic.com");
        Mockito.verify(tracedMethod, Mockito.times(0)).addExclusiveRollupMetricName(ExternalMetrics.ALL);
        Mockito.verify(tracedMethod, Mockito.times(0)).addExclusiveRollupMetricName(ExternalMetrics.ALL_OTHER);
        Mockito.verify(tracedMethod, Mockito.times(0)).addExclusiveRollupMetricName("External/host/all");
    }
}