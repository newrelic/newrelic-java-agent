/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.api.agent.NewRelicApiImplementation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.fail;

public class WeaveTest {

    private TestClass test;
    private String errorMessage;
    protected Throwable throwable;
    protected Throwable instrumentationThrowable;
    protected String libraryName;

    @BeforeClass
    public static void setup() throws Exception {
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(TestClass.class.getName());
    }

    @Before
    public void before() {
        test = new TestClass();

        errorMessage = null;
        throwable = null;
        libraryName = null;
        instrumentationThrowable = null;
        AgentBridge.publicApi = new NewRelicApiImplementation() {

            @Override
            public void noticeError(String message) {
                errorMessage = message;
            }

            @Override
            public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
                WeaveTest.this.throwable = throwable;
            }
        };

        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG) {

            @Override
            public void noticeInstrumentationError(Throwable throwable, String libraryName) {
                WeaveTest.this.instrumentationThrowable = throwable;
                WeaveTest.this.libraryName = libraryName;
            }

        };
    }

    @Test
    public void testConstructorWeave() {
        Assert.assertEquals("weaved unknown", test.constructorReturnString);

        test = new TestClass(0);
        Assert.assertEquals("weaved too few", test.constructorReturnString);

        test = new TestClass(1);
        Assert.assertEquals("weaved one", test.constructorReturnString);

        test = new TestClass(2);
        Assert.assertEquals("weaved two", test.constructorReturnString);

        test = new TestClass(3);
        Assert.assertEquals("weaved too many", test.constructorReturnString);

        test = new TestClass(4);
        Assert.assertEquals("weaved too many", test.constructorReturnString);
    }

    @Test
    public void testStaticMethod() {
        Assert.assertEquals(10, TestClass.staticMethod(5));
    }

    @Test
    public void testWrappedCallOriginal() {
        Assert.assertNull(throwable);
        Assert.assertEquals("Dude", test.wrappedCallOriginal("Dude"));
        Assert.assertNull(throwable);

        try {
            test.wrappedCallOriginal(null);
            fail("Method should have thrown");
        } catch (NullPointerException ex) {
        }

        Assert.assertNotNull(throwable);
        Assert.assertTrue(throwable instanceof NullPointerException);
    }

    @Test
    public void testBasicThrows() {
        Assert.assertNull(throwable);
        try {
            test.throwsException();

            fail("Method should have thrown");
        } catch (RuntimeException ex) {
        }

        Assert.assertNotNull(throwable);
        Assert.assertTrue(throwable instanceof RuntimeException);
    }

    @Test
    public void testNullThrows() {
        Assert.assertNull(throwable);
        try {
            test.throwsException(null);

            fail("Method should have thrown");
        } catch (NullPointerException ex) {
        }
        Assert.assertNotNull(throwable);
        Assert.assertTrue(throwable instanceof NullPointerException);
    }

    @Test
    public void testAssignedThrows() {
        Assert.assertNull(throwable);
        try {
            test.throwsException("Dude");

            fail("Method should have thrown");
        } catch (RuntimeException ex) {
        }
        Assert.assertNotNull(throwable);
        Assert.assertTrue(throwable instanceof RuntimeException);
    }

    @Test
    public void testGetPrivateVariableAccessTest() throws Exception {
        Assert.assertEquals(999, test.getPrivateVariableAccessTest());
    }

    @Test
    public void testGetCallable() throws Exception {
        Assert.assertEquals("wrap callable", test.getCallable().call());
    }

    /**
     * This test verifies that invocations of the IRequest interface in the weaved version of TestClass are invoked
     * correctly (as an interface). IRequest is weaved so in the weaved code it's actually an abstract class and the
     * calls to getPath() are invoked with INVOKE_VIRTUAL.
     *
     * @throws Exception
     */
    @Test
    public void testUseWeavedInterface() throws Exception {
        IRequest request = new IRequest() {

            @Override
            public String getPath() {
                return "/dude";
            }
        };
        Assert.assertEquals("/oh/dude/man/dude/man", test.getPath(request));
    }

    @Test
    public void testOverrideReturn() {
        Assert.assertEquals(777, test.overrideReturn());
    }

    @Test
    public void testFoo() {
        test.foo();
        Assert.assertEquals("Test Error", errorMessage);
    }

    @Test
    public void testApiCallAfter() {
        test.apiCallAfter();
        Assert.assertEquals("Error after", errorMessage);
    }

    @Test
    public void testChangeArgs() {
        Assert.assertEquals(666, test.changeArgs(999));
    }

    @Test
    public void testExistingInnerClass() {
        Assert.assertEquals("Weave_hmm", test.testExistingInnerClass());
    }

    @Test
    public void testInstrumentationErrorBefore() {
        Assert.assertEquals("Before", test.instrumentationErrorBefore());

        Assert.assertEquals("Weave Test", libraryName);
        Assert.assertTrue(instrumentationThrowable instanceof NullPointerException);
    }

    @Test
    public void testInstrumentationErrorAfter() {
        Assert.assertEquals("After", test.instrumentationErrorAfter());

        Assert.assertEquals("Weave Test", libraryName);
        Assert.assertTrue(instrumentationThrowable instanceof NullPointerException);
    }

    @Test
    public void testCallInlineMethod() {
        Assert.assertEquals(909d, test.callInlineMethod(), 0);
    }

    @Test
    public void testCallInlineAfter() {
        Assert.assertEquals(101d, test.callInlineAfter(10), 0);
    }

    @Test
    public void testIntegerLocalVariable() {
        Assert.assertEquals("Dude7", test.integerTest());
    }

    @Test
    public void testLong() {
        Assert.assertEquals("Dude8", test.longTest());
    }

    @Test
    public void testFloat() {
        Assert.assertEquals("Dude9.0", test.floatTest());
    }

    @Test
    public void testChar() {
        Assert.assertEquals("Dude5", test.charTest());
    }

    @Test
    public void testString() {
        Assert.assertEquals("Dudeyo", test.stringTest());
    }

    @Test
    public void testArray() {
        Assert.assertEquals("Dude1", test.arrayTest());
    }

    @Test
    public void testBoolean() {
        Assert.assertEquals("Dudetrue", test.booleanTest());
    }

    @Test
    public void testByte() {
        Assert.assertEquals("Dude123", test.byteTest());
    }

    @Test
    public void testDouble() {
        Assert.assertEquals("Dude10.0", test.doubleTest());
    }

    @Test
    public void testDiscardOriginalReturn() {
        Assert.assertEquals("dude", test.discardOriginalReturn());
    }

    @Test
    public void testDiscardOriginalReturnDouble() {
        Assert.assertEquals(55d, test.discardOriginalReturn(3333d), 0);
    }

    @Test
    public void classAnnotation() throws NoSuchMethodException, SecurityException {
        InstrumentedClass annotation = TestClass.class.getAnnotation(InstrumentedClass.class);
        Assert.assertNotNull(annotation);
        Assert.assertFalse(annotation.legacy());
    }

    @Test
    public void annotationNotTraced() throws NoSuchMethodException, SecurityException {
        Method method = TestClass.class.getDeclaredMethod("integerTest");
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(InstrumentationType.WeaveInstrumentation, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("Weave Test", annotation.instrumentationNames()[0]);
    }

    @Test
    public void testTraceNoImpl() throws NoSuchMethodException, SecurityException {
        Method method = TestClass.class.getDeclaredMethod("fooBar");
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(InstrumentationType.TracedWeaveInstrumentation, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("Weave Test", annotation.instrumentationNames()[0]);
    }

    @Test
    public void testTraceUtilityClass() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
        Class<?> clazz = Class.forName("com.nr.instrumentation.instrumentation.weaver.NotWeaved");
        AgentBridge.instrumentation.retransformUninstrumentedClass(clazz);
        Method method = clazz.getDeclaredMethod("notWeavedButStillTraced");
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(InstrumentationType.TracedWeaveInstrumentation, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("Weave Test", annotation.instrumentationNames()[0]);
    }

    @Test
    public void testDoubleSlots() {
        DoubleSlotTest doubleSlotTest = new DoubleSlotTest();
        Assert.assertEquals(7, doubleSlotTest.addInts(4, 0L, 2, 0L, "Foo"));
        Assert.assertEquals(7, DoubleSlotTest.staticAddInts(4, 0L, 2, 0L, "Foo"));

        Assert.assertEquals(5000000002L, doubleSlotTest.addLongs(4, 5000000000L, 2, 1L, "Foo"));
        Assert.assertEquals(5000000002L, DoubleSlotTest.staticAddLongs(4, 5000000000L, 2, 1L, "Foo"));

        Assert.assertEquals(2.23456d, doubleSlotTest.addDoubles(99, 1.23d, 78, 0.00456d, "DoubleFoo"), 0.000001d);
        Assert.assertEquals(2.23456d, DoubleSlotTest.staticAddDoubles(99, 1.23d, 78, 0.00456d, "DoubleFoo"), 0.000001d);
    }

    /**
     * The weaver sometimes changes the scope of local variables. This test makes sure we do this without introducing a
     * slot collision. A slot collision happens when we write bytecode which tries to store two different variables in
     * the same register at the same time.
     */
    @Test
    public void testSlotCollision() {
        ScopeTest tcf = new ScopeTest();
        try {
            Assert.assertEquals(6, tcf.returnFive());
            Assert.assertEquals(5, tcf.returnFour());
            Assert.assertEquals(4, tcf.returnThree());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testErrorTrapExit() {
        TestClass tc = new TestClass();
        tc.errorTrapExitTest();
        Assert.assertTrue(tc.preambleRan);
        Assert.assertFalse(tc.postambleRan);
    }

    @Test
    public void checkedExceptionTest() throws Exception {
        TestClass tc = new TestClass();
        try {
            tc.checkedExceptionTest();
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue(tc.exceptionLogged);
            return;
        }
        fail();
    }

    @Test
    public void checkedExceptionTest2() throws Exception {
        TestClass tc = new TestClass();
        try {
            tc.checkedExceptionTest2();
        } catch (IOException e) {
            Assert.assertTrue(tc.exceptionLogged);
            return;
        }
        fail();
    }

    @Test
    public void checkedThrowableTest() throws Throwable {
        TestClass tc = new TestClass();
        try {
            tc.checkedThrowableTest();
        } catch (OutOfMemoryError e) {
            // We do not allow instrumentation to capture Throwables, only Exceptions
            Assert.assertFalse(tc.exceptionLogged);
            return;
        }
        fail();
    }

    @Test
    public void checkedExceptionRuntimeTest() throws Exception {
        TestClass tc = new TestClass();
        try {
            tc.checkedExceptionRuntimeTest();
        } catch (OutOfMemoryError e) {
            // We do not allow instrumentation to capture Throwables, only Exceptions
            Assert.assertFalse(tc.exceptionLogged);
            return;
        }
        fail();
    }

    @Test
    public void checkedRuntimeExceptionTest() throws Exception {
        TestClass tc = new TestClass();
        try {
            tc.checkedRuntimeExceptionTest();
        } catch (IllegalStateException e) {
            Assert.assertTrue(tc.exceptionLogged);
            return;
        }
        fail();
    }

    @Test
    public void uncheckedRuntimeExceptionTest() throws Exception {
        TestClass tc = new TestClass();
        try {
            tc.uncheckedRuntimeExceptionTest();
        } catch (RuntimeException e) {
            Assert.assertTrue(tc.exceptionLogged);
            return;
        }
        fail();
    }

    @Test
    public void uncheckedRuntimeExceptionTest2() throws Exception {
        TestClass tc = new TestClass();
        try {
            tc.uncheckedRuntimeExceptionTest2();
        } catch (RuntimeException e) {
            Assert.assertTrue(tc.exceptionLogged);
            return;
        }
        fail();
    }

    @Test
    public void getTokenApiSupportabilityNPEBefore() {
        TestClass tc = new TestClass();
        try {
            tc.getTokenApiSupportabilityNPEBefore();
        } catch (Throwable t) {
            fail("An exception should not have been thrown into customer code");
        }
    }

    @Test
    public void getTokenApiSupportabilityNPEAfter() {
        TestClass tc = new TestClass();
        try {
            tc.getTokenApiSupportabilityNPEAfter();
        } catch (Throwable t) {
            fail("An exception should not have been thrown into customer code");
        }
    }
}
