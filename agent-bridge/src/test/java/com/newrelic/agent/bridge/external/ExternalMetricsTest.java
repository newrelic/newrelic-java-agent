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

}