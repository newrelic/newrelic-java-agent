/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.weavepackage.testclasses.OriginalNameInterface;
import com.newrelic.weave.weavepackage.testclasses.OriginalNameInterfaceImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Basic class weave tests.
 */
public class ClassWeaveTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassWeave weave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.ClassWeaveTest$MyOriginal", "com.newrelic.weave.ClassWeaveTest$MyWeave");

        ClassWeave weave2 = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.weavepackage.testclasses.OriginalNameInterface",
                "com.newrelic.weave.weavepackage.testclasses.Weave_OriginalNameInterface",
                "com.newrelic.weave.weavepackage.testclasses.OriginalNameInterfaceImpl");

        assertNull(weave.getMatch().getExtension());
        assertNull(weave2.getMatch().getExtension());
    }
    
    @Test
    public void testOriginalNameInterface() {
        OriginalNameInterface originalNameInterface = new OriginalNameInterfaceImpl();
        originalNameInterface.foo();
        originalNameInterface.bar();
    }

    @Test
    public void testConstructors() {
        MyOriginal original = new MyOriginal();
        assertEquals(0, original.getMyField());
        assertEquals(3000, original.getMyOriginalOnlyField());

        original = new MyOriginal(400);
        assertEquals(800, original.getMyField());
        assertEquals(3000, original.getMyOriginalOnlyField());
    }

    @Test
    public void testVoidMethod() {
        MyOriginal original = new MyOriginal();
        original.voidMethod();
        assertEquals(1001, original.getMyField());
    }

    @Test
    public void testRetIntMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(1001, original.retIntMethod());
    }

    @Test
    public void testRetDoubleMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(1001d, original.retDoubleMethod(), Double.MIN_NORMAL);
    }

    @Test
    public void testRetStringMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals("a string was weaved", original.retStringMethod());
    }

    @Test
    public void testConditionalMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(1000, original.conditionalMethod(-1));
        assertEquals(5100, original.conditionalMethod(100));
    }

    @Test
    public void testSwitchMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(1000, original.switchMethod(0));
        assertEquals(2002, original.switchMethod(1));
        assertEquals(2003, original.switchMethod(2));
        assertEquals(2004, original.switchMethod(3));
        assertEquals(5006, original.switchMethod(4));
    }

    @Test
    public void testTryCatchMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(1010, original.tryCatchMethod("10"));
        assertEquals(10, original.getMyField());
        assertEquals(999, original.tryCatchMethod("invalid"));
        assertEquals(-1, original.getMyField());
    }

    @Test
    public void testWrappedTryCatchMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(20, original.wrappedTryCatchMethod("10"));
        assertEquals(20, original.getMyField());
        assertEquals(-2, original.wrappedTryCatchMethod("invalid"));
        assertEquals(-2, original.getMyField());
    }

    @Test
    public void testAbstractUnweavedMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(1000, original.someWeavedMethod());
    }

    @Test
    public void testCallsPrivateWeaveMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(1001, original.callsPrivateWeaveMethod());
    }

    @Test
    public void testCallsPrivateTryCatchFinallyWeaveMethod() {
        MyOriginal original = new MyOriginal();
        assertEquals(500, original.callsPrivateTryCatchFinallyWeaveMethod());
    }

    @Test
    public void testAutboxingFun() {
        MyOriginal original = new MyOriginal();
        assertEquals(2, original.primitiveInt());
    }

    @Test
    @Ignore("Haven't found a permanent fix for this yet. Remove the \"static\" from \"methodHandler(long contentLength)\" to get this test to fail")
    public void testInlinePrivateMethodWithException() {
        MyOriginal original = new MyOriginal();
        assertNotNull(original.inlinePrivateMethodWithException());
    }

    @Test
    public void testClassReplacement() {
        MyOriginal original = new MyOriginal();
        // this shouldn't throw an error
        original.replaceClassReference();
    }

    public static class MyOriginal {

        public String sharedField = "I am a field";

        // constructors
        private int myOriginalOnlyField;

        public int getMyOriginalOnlyField() {
            return myOriginalOnlyField;
        }

        public MyOriginal() {
            myOriginalOnlyField = 3000;
        }

        public MyOriginal(int myField) {
            this();
            this.myField = myField;
        }

        // void method & target field access
        private int myField;

        public int getMyField() {
            return myField;
        }

        public void voidMethod() {
            myField = 1;
        }

        // different return values
        public int retIntMethod() {
            return 1;
        }

        public double retDoubleMethod() {
            return 1.0;
        }

        public String retStringMethod() {
            return "a string";
        }

        // if/else block
        public int conditionalMethod(int value) {
            if (value < 0) {
                return 0;
            } else {
                return value;
            }
        }

        // switch block
        public int switchMethod(int value) {
            switch (value) {
            case 0:
                return value;
            case 1:
            case 2:
            case 3:
                return 1 + value;
            default:
                return 2 + value;
            }
        }

        // try/catch/finally blocks
        public int tryCatchMethod(String integerString) {
            int result = -1;
            try {
                result = Integer.valueOf(integerString);
                return result;
            } catch (NumberFormatException nfe) {
                return result;
            } finally {
                myField = result;
            }
        }

        public int wrappedTryCatchMethod(String integerString) {
            int result = -1;
            try {
                result = Integer.valueOf(integerString);
                return result;
            } catch (NumberFormatException nfe) {
                return result;
            } finally {
                myField = result;
            }
        }

        // abstract unweaved method
        public int someUnweavedMethod() {
            return 1000;
        }

        public int someWeavedMethod() {
            return 1;
        }

        // private weave method
        public int callsPrivateWeaveMethod() {
            return 1;
        }

        // private try catch finally weave method
        public int callsPrivateTryCatchFinallyWeaveMethod() {
            return 1;
        }

        // autoboxing fun - make sure we box the primitive when the weave method uses the boxed or Object type
        public int primitiveInt() {
            return 1;
        }

        public Object inlinePrivateMethodWithException() {
            return null;
        }

        public void replaceClassReference(){

        }
    }

    public abstract static class MyWeave {

        public String sharedField = Weaver.callOriginal();

        // constructor weaving
        public MyWeave() {
        }

        public MyWeave(int myField) {
            this.myField += myField;
        }

        // void method & target field access
        private int myField;

        public void voidMethod() {
            Weaver.callOriginal();
            myField += 1000;
        }

        // different return values
        public int retIntMethod() {
            return 1000 + (Integer) Weaver.callOriginal();
        }

        public double retDoubleMethod() {
            double result = Weaver.callOriginal();
            return 1000d + result;
        }

        public String retStringMethod() {
            String result = Weaver.callOriginal();
            return result + " was weaved";
        }

        // if/else block
        public int conditionalMethod(int value) {
            int result = Weaver.callOriginal();
            if (result == 0) {
                return 1000 + result;
            }
            return 5000 + result;
        }

        // switch block
        public int switchMethod(int value) {
            int result = Weaver.callOriginal();
            switch (value) {
            case 0:
                return 1000 + result;
            case 1:
            case 2:
            case 3:
                return 2000 + result;
            default:
                return 5000 + result;
            }
        }

        // try/catch/finally block
        public int tryCatchMethod(String number) {
            int val = Weaver.callOriginal();
            return val + 1000;
        }

        public int wrappedTryCatchMethod(String integerString) {
            int result = 0;
            try {
                result = Weaver.callOriginal();
                result += Integer.valueOf(integerString);
                return result;
            } catch (NumberFormatException nfe) {
                result = -2;
                return result;
            } finally {
                myField = result;
            }
        }

        // abstract unweaved method
        public abstract int someUnweavedMethod();

        public int someWeavedMethod() {
            return someUnweavedMethod();
        }

        // private weave method
        private int weavePrivateMethod() {
            return 500;
        }

        // private try catch finally weave method
        private void weavePrivateTryCatchFinallyMethod() {
            try {
                throw new RuntimeException();
            } catch (Throwable t) {

            }
        }

        public int callsPrivateWeaveMethod() {
            int result = Weaver.callOriginal();
            int fromPrivate = weavePrivateMethod();
            return result + fromPrivate + weavePrivateMethod();
        }

        public int callsPrivateTryCatchFinallyWeaveMethod() {
            try {
                weavePrivateTryCatchFinallyMethod();
                weavePrivateMethod();
            } catch (Exception e) {
                return 0;
            }
            return 500;
        }

        // autoboxing fun - make sure we box the primitive when the weave method uses the boxed or Object type
        public int primitiveInt() {
            Integer original = Weaver.<Integer> callOriginal();
            return original + 1;
        }

        public Object inlinePrivateMethodWithException() {
            methodHandler(getContentLength());

            // We are doing this on purpose (not using Weaver.callOriginal()) so we can validate it was actually weaved
            return new Object();
        }

        private static void methodHandler(long contentLength) {
            // doesn't do anything but this is important to reproduce the VerifyError
        }

        private static long getContentLength() {
            try {
                return Long.parseLong("1024");
            } catch (Exception e) {
                return -1L;
            }
        }

        public void replaceClassReference() {
            MyWeave.class.cast(this).sharedField = null;
        }
    }
}
