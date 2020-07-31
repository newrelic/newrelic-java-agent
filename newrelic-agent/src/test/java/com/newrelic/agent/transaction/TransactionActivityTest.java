/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.TracerFlags;
import org.junit.Before;
import org.junit.Test;

public class TransactionActivityTest {
    @Before
    public void beforeTest() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @Test
    public void txaNoTxn() {
        TransactionActivity txa = TransactionActivity.create(null, Integer.MAX_VALUE);
        ClassMethodSignature classMethodSignature = new ClassMethodSignature("class", "method", "methodDesc");
        OtherRootTracer tracer = new OtherRootTracer(txa, classMethodSignature, null, null,
                TracerFlags.TRANSACTION_TRACER_SEGMENT | TracerFlags.ASYNC, 0);
        tracer.isTransactionSegment();

        // Make sure methods that touch this txa's txn don't throw NPEs.
        txa.tracerStarted(tracer);
        txa.addTracer(tracer);
        txa.tracerFinished(tracer, 0);
    }
}
