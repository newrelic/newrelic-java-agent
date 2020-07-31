/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.api.agent.weaver.Weaver;

/**
 * ConstructorWeaveTest.java
 */

public class ConstructorWeaveTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$ImplicitOriginal",
                "com.newrelic.weave.ConstructorWeaveTest$ImplicitWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.ConstructorWeaveTest$DisregardImplicitOriginal",
                "com.newrelic.weave.ConstructorWeaveTest$DisregardImplicitWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$NoArgOriginal",
                "com.newrelic.weave.ConstructorWeaveTest$NoArgWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$InitFieldOriginal",
                "com.newrelic.weave.ConstructorWeaveTest$InitFieldWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$InitFieldOriginal2",
                "com.newrelic.weave.ConstructorWeaveTest$InitFieldWeave2");
        WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.ConstructorWeaveTest$InitFinalFieldNoDefaultConstructorOriginal",
                "com.newrelic.weave.ConstructorWeaveTest$InitFinalFieldNoDefaultConstructorWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.ConstructorWeaveTest$MultipleReturnOriginal",
                "com.newrelic.weave.ConstructorWeaveTest$MultipleReturnWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$TryCatchOriginal",
                "com.newrelic.weave.ConstructorWeaveTest$TryCatchWeave");
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructors_Original",
                "com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructors_Weave", "com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructors_Original", false);
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructorsBase_Original",
                "com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructorsBase_Weave", "com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructorsBase_Original", true);
        WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructorsBase_Original",
                "com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructorsBase_Weave", "com.newrelic.weave.ConstructorWeaveTest$WeaveAllConstructorsChild", true);
    }

    @Test
    public void testImplicit() {
        ImplicitOriginal original = new ImplicitOriginal();
        assertEquals(1, original.myField);
    }

    public static class ImplicitOriginal {
        public int myField;
    }

    public static class ImplicitWeave {
        public int myField;

        public ImplicitWeave() {
            myField = 1;
        }
    }

    @Test
    public void testDisregardImplicitWeave() {
        DisregardImplicitOriginal original = new DisregardImplicitOriginal("hello");
        assertEquals("hello", original.myField);
        assertEquals("weaved hello", original.getMyField());
    }

    public static class DisregardImplicitOriginal {
        public String myField;

        public DisregardImplicitOriginal(String msg) {
            myField = msg;
        }

        public String getMyField() {
            return myField;
        }
    }

    public static class DisregardImplicitWeave {
        public String getMyField() {
            return "weaved " + Weaver.callOriginal();
        }
    }

    @Test
    public void testNoArg() {
        NoArgOriginal original = new NoArgOriginal();
        assertEquals(101, original.myField);
    }

    public static class NoArgOriginal {
        public int myField;

        public NoArgOriginal() {
            this.myField = 100;
        }
    }

    public static class NoArgWeave {
        public int myField;

        public NoArgWeave() {
            myField += 1;
        }
    }

    @Test
    public void testInitField() {
        InitFieldOriginal original = new InitFieldOriginal();
        assertEquals(200, original.myField);
        original = new InitFieldOriginal(1);
        assertEquals(201, original.myField);
        assertEquals(1, original.myFinalField);
        assertEquals(2, original.myOriginalField);

        InitFieldOriginal2 original2 = new InitFieldOriginal2();
        assertEquals(100, original2.myField);
        original2 = new InitFieldOriginal2(1);
        assertEquals(102, original2.myField);
    }

    public static class InitFieldOriginal {
        public int myField = 100;
        public final int myFinalField = 1;
        public int myOriginalField = 2;

        public InitFieldOriginal() {
        }

        public InitFieldOriginal(int inc) {
            myField += inc;
        }
    }

    public static class InitFieldWeave {
        public int myField = 200;
        public final int myFinalField = Weaver.callOriginal();
        public int myOriginalField = Weaver.callOriginal(); // this is not necessary or recommended, but supported

        public InitFieldWeave() {
        }

        public InitFieldWeave(int inc) {
            myField += inc;
        }
    }

    public static class InitFieldOriginal2 {
        public int myField = 100;

        public InitFieldOriginal2() {
        }

        public InitFieldOriginal2(int inc) {
            myField += inc;
        }
    }

    public static class InitFieldWeave2 {
        public int myField;

        public InitFieldWeave2(int inc) {
            myField += inc;
        }
    }

    @Test
    public void testInitFinalFieldWithNoDefaultConstructor() {
        // this tests that final fields can be assigned if there's no matching constructor
        InitFinalFieldNoDefaultConstructorOriginal original = new InitFinalFieldNoDefaultConstructorOriginal(1);
        assertEquals(1, original.myFinalField);
    }

    public static class InitFinalFieldNoDefaultConstructorOriginal {
        public final int myFinalField;

        public InitFinalFieldNoDefaultConstructorOriginal(int i) {
            myFinalField = i;
        }
    }

    public static class InitFinalFieldNoDefaultConstructorWeave {
        public final int myFinalField = Weaver.callOriginal();
    }

    @Test
    public void testMultipleReturn() {
        MultipleReturnOriginal original = new MultipleReturnOriginal(0);
        assertEquals("weaved zero", original.val);

        original = new MultipleReturnOriginal(1);
        assertEquals("weaved exactly one", original.val);

        original = new MultipleReturnOriginal(2);
        assertEquals("weaved two", original.val);

        original = new MultipleReturnOriginal(3);
        assertEquals("weaved lots", original.val);
    }

    public static class MultipleReturnOriginal {
        public String val = "unknown";

        public MultipleReturnOriginal(int i) {
            if (i < 1) {
                val = "few";
                return;
            }
            if (i == 1) {
                val = "one";
                return;
            }
            if (i == 2) {
                val = "two";
                return;
            }
            val = "lots";
        }
    }

    public static class MultipleReturnWeave {
        public String val;

        public MultipleReturnWeave(int i) {
            if (i == 0) {
                val = "weaved zero";
                return;
            }
            if (i == 1) {
                val = "weaved exactly " + val;
                return;
            }
            val = "weaved " + val;
        }
    }

    @Test
    public void testTryCatch() {
        TryCatchOriginal original = new TryCatchOriginal();
        assertEquals(-100, original.val);
        assertEquals(100, original.baseConstructorVal);

        original = new TryCatchOriginal("1");
        assertEquals(4, original.val);
        assertEquals(100, original.baseConstructorVal);

        original = new TryCatchOriginal("invalid");
        assertEquals(-4, original.val);
        assertEquals(40, original.baseConstructorVal);
    }

    public static class TryCatchOriginal {
        public int val;
        public int baseConstructorVal;

        public TryCatchOriginal() {
            baseConstructorVal = 100;
        }

        public TryCatchOriginal(String stringVal) {
            this();
            try {
                val = Integer.parseInt(stringVal);
            } catch (NumberFormatException nfe) {
                val = -1;
            } finally {
                val++;
            }
        }
    }

    public static class TryCatchWeave {
        public int val;
        public int baseConstructorVal;

        public TryCatchWeave() {
            val = -100;
        }

        public TryCatchWeave(String stringVal) {
            try {
                val += Integer.parseInt(stringVal);
            } catch (NumberFormatException nfe) {
                val += -5;
                baseConstructorVal -= 60;
            } finally {
                val++;
            }
        }
    }

    public static class DelegatingOriginal {
        public DelegatingOriginal() {
        }

        public DelegatingOriginal(int i) {
            this();
        }
    }

    public static class DelegatingWeave {
        public DelegatingWeave() {
        }

        public DelegatingWeave(int i) {
            this();
        }
    }

    public static class InheritedDelegatingOriginal extends DelegatingOriginal {
        public InheritedDelegatingOriginal() {
            this(100);
        }

        public InheritedDelegatingOriginal(int i) {
        }
    }

    public static class InheritedDelegatingWeave {
        public InheritedDelegatingWeave() {
        }

        public InheritedDelegatingWeave(int i) {
        }
    }

    @Test
    public void testWeaveAllConstructorsExactMatch() {
        WeaveAllConstructors_Original defaultCtor = new WeaveAllConstructors_Original();
        assertEquals(1, defaultCtor.getNumCalls());

        WeaveAllConstructors_Original ctorWithArgs = new WeaveAllConstructors_Original("a");
        assertEquals(2, ctorWithArgs.getNumCalls());
    }

    public static class WeaveAllConstructors_Original {
        private int numCalls;

        public WeaveAllConstructors_Original() {
        }

        public WeaveAllConstructors_Original(String arg) {
            this();
        }

        public int getNumCalls() {
            return numCalls;
        }
    }

    public static class WeaveAllConstructors_Weave {
        private int numCalls ;

        @WeaveAllConstructors
        public WeaveAllConstructors_Weave() {
            numCalls++;
        }

        public int getNumCalls() {
            return numCalls;
        }
    }

    @Test
    public void testWeaveAllConstructorsInheritance() {
        WeaveAllConstructorsChild child = new WeaveAllConstructorsChild("c");
        Assert.assertEquals(2, child.getNumCalls());
    }

    public static class WeaveAllConstructorsBase_Original {

        public WeaveAllConstructorsBase_Original() {
        }

        public int getNumCalls() {
            return 0;
        }
    }

    public static class WeaveAllConstructorsChild extends WeaveAllConstructorsBase_Original {
        public WeaveAllConstructorsChild(String c) {
        }
    }

    public static class WeaveAllConstructorsBase_Weave {
        private int numCalls;

        @WeaveAllConstructors
        public WeaveAllConstructorsBase_Weave() {
            numCalls++;
        }

        public int getNumCalls() {
            return numCalls;
        }

    }
}
