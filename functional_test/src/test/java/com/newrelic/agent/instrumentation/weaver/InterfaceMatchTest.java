/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.instrument.UnmodifiableClassException;
import java.util.List;

public class InterfaceMatchTest {

    private IList<String> list;

    @BeforeClass
    public static void setup() throws Exception {
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(MyList.class.getName());
    }

    @Before
    public void before() {
        list = new MyList();
    }

    @Test
    public void testCount() {
        Assert.assertEquals(666, list.count());
    }

    @Test
    @Trace(dispatcher = true)
    public void testRemove() {
        Assert.assertTrue(list.remove(""));
        Transaction transaction = Transaction.getTransaction();
        List<Tracer> tracers = transaction.getTransactionActivity().getTracers();
        Assert.assertNotNull(tracers);
        Assert.assertEquals(1, tracers.size());
        Assert.assertEquals("DUDE", tracers.get(0).getMetricName());
    }

    @Test
    public void retransform() throws UnmodifiableClassException {
        ServiceFactory.getCoreService().getInstrumentation().retransformClasses(MyList.class);
        instanceFieldTest();
    }

    @Test
    public void instanceFieldTest() {
        Assert.assertEquals("WORKING_INSTANCE", list.instanceFieldTest());
    }

    @Test
    public void staticFieldTest() {
        Assert.assertEquals("WORKING_STATIC", list.staticFieldTest());
    }

    @Test
    public void testRemoveWithClassReference() {
        // this is to make sure that we weave into the actual method implementation, not the bridge method
        MyList list = new MyList();
        Assert.assertTrue(list.remove(""));
    }

    @Test
    public void unimplementedWeaveMethodTest() {
        // Make sure that interface methods not implemented in a @Weave class
        // can be called from woven instrumentation in the class.
        Assert.assertEquals(987 + 5, list.unimplementedWeaveMethodTest());
    }
}
