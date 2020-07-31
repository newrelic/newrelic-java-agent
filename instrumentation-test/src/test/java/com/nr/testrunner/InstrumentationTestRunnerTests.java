/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.testrunner;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.instrumentation.FrameworkClass;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.example.instrumentation")
public class InstrumentationTestRunnerTests {

    @Test
    public void testTransaction() {
        FrameworkClass original = new FrameworkClass();
        String result = original.transaction();
        assertEquals("weaved transaction", result);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
    }

    @Test
    public void testNameTransaction() {
        FrameworkClass original = new FrameworkClass();
        String result = original.namedTransaction();
        assertEquals("weaved named transaction", result);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        assertEquals("OtherTransaction/newrelic/is/good", introspector.getTransactionNames().toArray()[0]);
    }

    @Test
    public void testNameTransactionSupportability() {
        FrameworkClass original = new FrameworkClass();
        String result = original.namedTransactionSupportability();
        assertEquals("weaved named transaction supportability", result);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        assertEquals("OtherTransaction/newrelic/is/ok", introspector.getTransactionNames().toArray()[0]);
        // One will come from the @Trace(dispatcher = true) and one from the direct "setTransactionName" call
        assertEquals(2, introspector.getUnscopedMetrics().get("Supportability/API/SetTransactionName/Custom").getCallCount());
    }

    /**
     * Assert that calls to NewRelic.setTransactionName(...) work.
     */
    @Test
    public void testNewRelicApiTxName() {
        FrameworkClass original = new FrameworkClass();
        String result = original.nrApiNameTransaction();
        assertEquals("weaved named transaction", result);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        assertEquals("OtherTransaction/newrelic/isCool", introspector.getTransactionNames().toArray()[0]);
    }

    @Test
    public void testNoTransaction() {
        FrameworkClass original = new FrameworkClass();
        assertEquals("weaved no transaction", original.noTransaction());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(0, introspector.getFinishedTransactionCount());
    }

    @Test
    public void testDoTransactionInUnitTest() {
        doTransactionInUnitTest();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
    }

    @Trace(dispatcher = true)
    public void doTransactionInUnitTest() {
    }
}
