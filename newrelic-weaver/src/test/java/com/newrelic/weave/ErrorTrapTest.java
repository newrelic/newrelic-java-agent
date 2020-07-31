/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.weavepackage.ErrorTrapHandler;

public class ErrorTrapTest {
    public static ClassWeave weave;
    public static boolean trapMethodInvoked = false;
    public static ClassNode errorHandlerClassNode;

    @BeforeClass
    public static void beforeClass() throws IOException {
        errorHandlerClassNode = WeaveTestUtils.readClass("com.newrelic.weave.ErrorTrapTest$ErrorHandler");
        weave = WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.ErrorTrapTest$OriginalClass",
                "com.newrelic.weave.ErrorTrapTest$WeaveClass", "com.newrelic.weave.ErrorTrapTest$OriginalClass", false,
                Collections.<String>emptySet(), Collections.<String>emptySet(), errorHandlerClassNode);
    }

    @Before
    public void beforeTest() {
        Assert.assertTrue(new OriginalClass().isWeaved());
    }

    /*-
    @Test
    public void trapReplacedMethod() throws IOException {
        try {
            Assert.assertEquals(5, new OriginalClass().replaceMethod());
        } catch (Throwable t) {
            Assert.fail("Exception was thrown from weaved code: " + t.getMessage());
        }
    }

    @Test
    public void dontTrapReplacedMethodThrow() throws IOException {
        boolean threw;
        try {
            new OriginalClass().replaceMethodThrows();
            threw = false;
        } catch (Throwable t) {
            threw = true;
        }
        if (!threw) {
            Assert.assertTrue("weave method should have thrown an exception", threw);
        }
    }
     */

    @Test
    public void trapMergedMethod() throws IOException {
        trapMethodInvoked = false;
        try {
            Assert.assertEquals(6, new OriginalClass().mergeMethodThrowInPreamble());
        } catch (Throwable t) {
            Assert.fail("Exception was thrown from weaved code: " + t.getMessage());
        }
        Assert.assertTrue(trapMethodInvoked);
        trapMethodInvoked = false;

        try {
            Assert.assertEquals(6, new OriginalClass().mergeMethodThrowInPostamble());
        } catch (Throwable t) {
            Assert.fail("Exception was thrown from weaved code: " + t.getMessage());
        }
        Assert.assertTrue(trapMethodInvoked);
        trapMethodInvoked = false;
    }

    @Test
    public void trapMergedMethodThrows() throws IOException {
        boolean threw = false;
        try {
            new OriginalClass().mergeMethodExplicitThrowInPreamble();
        } catch (Throwable t) {
            threw = true;
        }
        if (!threw) {
            Assert.fail("weave method should have thrown an exception");
        }

        threw = false;
        try {
            new OriginalClass().mergeMethodExplicitThrowInPostamble();
        } catch (Throwable t) {
            threw = true;
        }
        if (!threw) {
            Assert.fail("weave method should have thrown an exception");
        }
    }

    @Test
    public void testInterface() throws IOException {
        trapMethodInvoked = false;
        ClassWeave anotherWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.ErrorTrapTest$Interface", "com.newrelic.weave.ErrorTrapTest$InterfaceWeave",
                "com.newrelic.weave.ErrorTrapTest$AnotherImplementation", true, Collections.<String>emptySet(),
                Collections.<String>emptySet(), errorHandlerClassNode);
        AnotherImplementation another = new AnotherImplementation();
        Assert.assertEquals("Original", another.interfaceMethodCallsImplementationMethods());
        Assert.assertTrue(trapMethodInvoked);
        trapMethodInvoked = false;
    }

    @Test
    public void testWrappedTryCatchMethod() {
        Assert.assertEquals(-2, new OriginalClass().wrappedTryCatchMethod());
    }

    @Test
    public void trapConstructor() {
        trapMethodInvoked = false;
        try {
            new OriginalClass(true, false, false);
            Assert.fail();
        } catch (Exception e) {
        }
        Assert.assertFalse(trapMethodInvoked);

        trapMethodInvoked = false;
        try {
            new OriginalClass(false, true, false);
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertTrue(trapMethodInvoked);

        trapMethodInvoked = false;
        try {
            new OriginalClass(false, false, true);
            Assert.fail();
        } catch (Exception e) {
        }
        Assert.assertFalse(trapMethodInvoked);

        trapMethodInvoked = false;
        try {
            new OriginalClass(false, false, false);
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertFalse(trapMethodInvoked);
    }

    @Test
    public void testOriginalThrow() {
        trapMethodInvoked = false;
        Assert.assertEquals(9, new OriginalClass().originalThrow());
        Assert.assertFalse(trapMethodInvoked);
    }

    public static final class ErrorHandler extends ErrorTrapHandler {
        public static void onWeaverThrow(Throwable weaverError) {
            ErrorTrapTest.trapMethodInvoked = true;
        }
    }

    public static class OriginalClass {

        public OriginalClass() {
        }

        public OriginalClass(boolean originalThrow, boolean weaveThrow, boolean weaveExplicitThrow) {
            if (originalThrow) {
                String s = "foo";
                char c = s.charAt(55);
            } else {
                "stuff".toString();
            }
        }

        public int originalThrow() {
            int numCatches = 0;
            try {
                try {
                    try {
                        try {
                            try {
                                try {
                                    String s = null;
                                    s.toString();
                                } catch (Throwable t) {
                                    if (numCatches == 0)
                                        numCatches++;
                                    throw t;
                                }
                            } catch (Throwable t) {
                                if (numCatches == 1)
                                    numCatches++;
                                throw t;
                            }
                        } catch (Throwable t) {
                            if (numCatches == 2)
                                numCatches++;
                            throw t;
                        }
                    } catch (Throwable t) {
                        if (numCatches == 3)
                            numCatches++;
                        throw t;
                    }
                } catch (Throwable t) {
                    if (numCatches == 4)
                        numCatches++;
                    throw t;
                }
            } catch (Throwable t) {
                if (numCatches == 5)
                    numCatches++;
            }
            return numCatches;
        }

        public void replaceVoid() {
            "original".toString();
        }

        public boolean isWeaved() {
            return false;
        }

        public int replaceMethod() {
            return 5;
        }

        public int replaceMethodThrows() {
            return 5;
        }

        public int mergeMethodThrowInPreamble() {
            return 6;
        }

        public int mergeMethodExplicitThrowInPreamble() {
            return 6;
        }

        public int mergeMethodThrowInPostamble() {
            return 6;
        }

        public int mergeMethodExplicitThrowInPostamble() {
            return 6;
        }

        public int wrappedTryCatchMethod() {
            return -1;
        }

    }

    public interface Interface {
        String interfaceMethodCallsImplementationMethods();
    }

    public static class AnotherImplementation implements Interface {

        @Override
        public String interfaceMethodCallsImplementationMethods() {
            return "Original";
        }

    }

    public static class InterfaceWeave {
        public String interfaceMethodCallsImplementationMethods() {
            return "Foo".subSequence(-3, 55) + privateMethod() + "/" + Weaver.callOriginal();
        }

        // inlined private methods should play nice with the error trap
        private String privateMethod() {
            return "Weave.privateMethod";
        }
    }

    public static class WeaveClass {
        public WeaveClass(boolean originalThrow, boolean weaveThrow, boolean weaveExplicitThrow) {
            if (weaveThrow) {
                String baz = "bazz";
                baz.charAt(934);
            }
            if (weaveExplicitThrow) {
                throw new RuntimeException("bam");
            }
        }

        public int originalThrow() {
            int numThrows = 0;
            try {
                try {
                    try {
                        numThrows = Weaver.callOriginal();
                        String s2 = null;
                        s2.toString();
                    } catch (Throwable t) {
                        if (numThrows == 6)
                            numThrows++;
                        throw t;
                    }
                } catch (Throwable t) {
                    if (numThrows == 7)
                        numThrows++;
                    throw t;
                }
            } catch (Throwable t) {
                if (numThrows == 8)
                    numThrows++;
            }
            return numThrows;
        }

        public void replaceVoid() {
            "replaced".toString();
        }

        public boolean isWeaved() {
            Weaver.callOriginal();
            return true;
        }

        public int replaceMethod() {
            String s = "No original call.";
            char myChar = s.charAt(s.length());
            Character.getNumericValue(myChar);
            return 4;
        }

        public int mergeMethodThrowInPreamble() {
            String preambleString = "In the preamble";
            preambleString.charAt(preambleString.length());
            preambleString.toString();
            Weaver.callOriginal();
            String postambleString = "In the postamble";
            postambleString.toString();
            return 7;
        }

        public int mergeMethodThrowInPostamble() {
            String preambleString = "In the preamble";
            preambleString.toString();
            int orig = Weaver.callOriginal();
            String postambleString = "In the postamble";
            postambleString.charAt(postambleString.length());
            postambleString.toString();
            return orig + 1;
        }

        public int replaceMethodThrows() {
            throw new RuntimeException("replaced exception");
        }

        public int mergeMethodExplicitThrowInPreamble() {
            String preambleString = "In the preamble";
            preambleString.toString();
            if (preambleString.equals(preambleString)) {
                throw new RuntimeException("explicit preamble exception");
            }
            Weaver.callOriginal();
            String postambleString = "In the postamble";
            postambleString.toString();
            return 7;
        }

        public int mergeMethodExplicitThrowInPostamble() {
            String preambleString = "In the preamble";
            preambleString.toString();
            int orig = Weaver.callOriginal();
            if (orig == 6) {
                throw new RuntimeException("explicit postamble exception");
            }
            String postambleString = "In the postamble";
            postambleString.toString();
            return 7;
        }

        public int wrappedTryCatchMethod() {
            try {
                Weaver.callOriginal();
                Integer.valueOf("bogus");
                return 0;
            } catch (NumberFormatException nfe) {
                return -2;
            }
        }

    }

}
