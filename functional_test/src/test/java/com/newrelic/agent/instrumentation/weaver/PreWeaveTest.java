/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PreWeaveTest {

    private TestClass test;

    @Before
    public void before() {
        test = new TestClass();
    }

    @Test
    public void testGetPrivateVariableAccessTest() throws Exception {
        Assert.assertEquals(0, test.getPrivateVariableAccessTest());
    }

    @Test
    public void testGetCallable() throws Exception {
        Assert.assertEquals("callable", test.getCallable().call());
    }

    @Test
    public void testFoo() {
        test.foo();
    }

    @Test
    public void testBar() {
        Assert.assertEquals(1554, test.bar(6));
    }

    @Test
    public void testChangeArgs() {
        Assert.assertEquals(999, test.changeArgs(999));
    }

    @Test
    public void testCallInlineMethod() {
        Assert.assertEquals(808d, test.callInlineMethod(), 0);
    }

    @Test
    public void testCallInlineAfter() {
        Assert.assertEquals(818d, test.callInlineAfter(10), 0);
    }

    @Test
    public void testLocalVariableTest() {
        Assert.assertEquals("Dude", test.integerTest());
    }

    @Test
    public void testOverrideReturn() {
        Assert.assertEquals(9090, test.overrideReturn());
    }

    @Test(expected = RuntimeException.class)
    public void testWrappedCallOriginal() {
        test.wrappedCallOriginal(null);
    }

    @Test
    public void testWrappedCallOriginal2() {
        Assert.assertEquals("Dude", test.wrappedCallOriginal("Dude"));
    }

    @Test(expected = RuntimeException.class)
    public void testThrows() {
        test.throwsException();
    }

    @Test(expected = RuntimeException.class)
    public void testThrows2() {
        test.throwsException(null);
    }

    @Test
    public void testInstrumentationErrorBefore() {
        Assert.assertEquals("Before", test.instrumentationErrorBefore());
    }

    @Test
    public void testInstrumentationErrorAfter() {
        Assert.assertEquals("After", test.instrumentationErrorAfter());
    }

    @Test
    public void testDoubleSlots() {
        DoubleSlotTest doubleSlotTest = new DoubleSlotTest();
        Assert.assertEquals(6, doubleSlotTest.addInts(4, 0L, 2, 0L, "Foo"));
        Assert.assertEquals(6, DoubleSlotTest.staticAddInts(4, 0L, 2, 0L, "Foo"));

        Assert.assertEquals(5000000001L, doubleSlotTest.addLongs(4, 5000000000L, 2, 1L, "Foo"));
        Assert.assertEquals(5000000001L, DoubleSlotTest.staticAddLongs(4, 5000000000L, 2, 1L, "Foo"));

        Assert.assertEquals(1.23456d, doubleSlotTest.addDoubles(99, 1.23d, 78, 0.00456d, "DoubleFoo"), 0.000001d);
        Assert.assertEquals(1.23456d, DoubleSlotTest.staticAddDoubles(99, 1.23d, 78, 0.00456d, "DoubleFoo"), 0.000001d);
    }

}
