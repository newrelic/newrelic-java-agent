/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.api.agent.weaver.Weaver;

public class NestedClassWeaveTest {

    // used in test cases
    private String outerString;

    @BeforeClass
    public static void beforeClass() throws IOException {

        ClassWeave nonstaticMatch = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NestedClassWeaveTest$NonstaticOriginal",
                "com.newrelic.weave.NestedClassWeaveTest$StaticWeave");
        assertNotNull(nonstaticMatch.getMatch().getExtension());

        ClassWeave interfaceMatch = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NestedClassWeaveTest$Interface",
                "com.newrelic.weave.NestedClassWeaveTest$InterfaceWeave",
                "com.newrelic.weave.NestedClassWeaveTest$InterfaceTarget");
        assertNotNull(interfaceMatch.getMatch().getExtension());

        ClassWeave parentMatchParent = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NestedClassWeaveTest$Parent",
                "com.newrelic.weave.NestedClassWeaveTest$ParentWeave", "com.newrelic.weave.NestedClassWeaveTest$Parent");
        assertNull(parentMatchParent.getMatch().getExtension());

        ClassWeave parentMatchChild = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NestedClassWeaveTest$Parent",
                "com.newrelic.weave.NestedClassWeaveTest$ParentWeave", "com.newrelic.weave.NestedClassWeaveTest$Child");
        assertNull(parentMatchChild.getMatch().getExtension());

        ClassWeave unmatchedInner = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NestedClassWeaveTest$UnmatchedNestedOriginal",
                "com.newrelic.weave.NestedClassWeaveTest$UnmatchedNestedWeave");
        assertNotNull(unmatchedInner.getMatch().getExtension());

        ClassWeave chainingInner = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NestedClassWeaveTest$ChainingAnonOriginal",
                "com.newrelic.weave.NestedClassWeaveTest$ChainingAnonWeave");
        assertNull(chainingInner.getMatch().getExtension());

        ClassWeave usesUnrelatedAnnon = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NestedClassWeaveTest$UsesUnrelatedAnonOriginal",
                "com.newrelic.weave.NestedClassWeaveTest$UsesUnrelatedAnonWeave");
        assertNull(usesUnrelatedAnnon.getMatch().getExtension());
    }

    @Test
    public void testStaticMatchingNonstatic() {
        NonstaticOriginal original = new NonstaticOriginal();
        assertEquals("originalField", original.originalField);
        assertEquals("newField", original.getNewField());
        outerString = "outerString";
        assertEquals("weaved outerString", original.getOuterString());

        original = new NonstaticOriginal("sss");
        assertEquals("sss", original.originalField);
        assertEquals("sss", original.getNewField());
        outerString = "new outerString";
        assertEquals("weaved new outerString", original.getOuterString());
    }

    public class NonstaticOriginal {
        public String originalField = "originalField";

        public NonstaticOriginal() {
        }

        public NonstaticOriginal(String field) {
            this.originalField = field;
        }

        public String getOuterString() {
            return outerString;
        }

        public String getNewField() {
            return null; // for testing purposes
        }
    }

    public static class StaticWeave {
        private String newField = "newField";

        public StaticWeave() {
        }

        public StaticWeave(String field) {
            this.newField = field;
        }

        public String getNewField() {
            return newField;
        }

        public String getOuterString() {
            return "weaved " + Weaver.callOriginal();
        }
    }

    @Test
    public void testInterface() {
        InterfaceTarget target = new InterfaceTarget();
        assertEquals("weaved originalField", target.getOriginalField());
        assertEquals("newField", target.getNewField());

        target = new InterfaceTarget("sss");
        assertEquals("weaved sss", target.getOriginalField());
        assertEquals("newField", target.getNewField());
    }

    public interface Interface {
        String getOriginalField();

        String getNewField();
    }

    public static class InterfaceWeave {
        private String newField;

        public InterfaceWeave() {
            newField = "newField";
        }

        public String getOriginalField() {
            return "weaved " + Weaver.callOriginal();
        }

        public String getNewField() {
            return newField;
        }
    }

    public class InterfaceTarget implements Interface {
        public String originalField = "originalField";

        public InterfaceTarget() {
        }

        public InterfaceTarget(String field) {
            this.originalField = field;
        }

        public String getOriginalField() {
            return originalField;
        }

        public String getNewField() {
            return null; // for testing purposes
        }
    }

    @Test
    public void testBase() {
        Child child = new Child("child");
        outerString = "outerString";
        assertEquals("weaved outerString", child.getOuterString());
        assertEquals("child", child.getChild());
    }

    public class Parent {
        public String getOuterString() {
            return outerString;
        }
    }

    public static class ParentWeave {
        public String getOuterString() {
            return "weaved " + Weaver.callOriginal();
        }
    }

    public class Child extends Parent {
        private String childString;

        public Child(String childString) {
            this.childString = childString;
        }

        public String getChild() {
            return childString;
        }
    }

    @Test
    public void testWeaveUsesUnmatchedInner() {
        UnmatchedNestedOriginal orig = new UnmatchedNestedOriginal();
        assertEquals("anonymous inner original", orig.usesAnonInner("anonymous inner"));
        assertEquals("anonymous inner 1.0 original",
                orig.usesAnonInnerWithNewMembers("anonymous inner"));

        orig.setNewMember(2.0);
        assertEquals("anonymous inner 2.0 original",
                orig.usesAnonInnerWithNewMembers("anonymous inner"));

        assertEquals("inner original", orig.usesInner());

        assertEquals("static inner original", orig.usesStaticInner());
        assertEquals("static inner newStatic original", orig.usesStaticInnerWithNewMembers());

        UnmatchedNestedOriginal.setNewStatic("newStatic2");
        assertEquals("static inner newStatic2 original", orig.usesStaticInnerWithNewMembers());
    }

    public static class UnmatchedNestedOriginal {
        public String usesAnonInner(String weave) {
            return "original";
        }

        public String usesAnonInnerWithNewMembers(String weave) {
            return "original";
        }

        public String usesInner() {
            return "original";
        }

        public String usesStaticInner() {
            return "original";
        }

        public String usesStaticInnerWithNewMembers() {
            return "original";
        }

        public void setNewMember(double value) {
            // this is for changing the new field value in the weave class only
        }

        public static void setNewStatic(String value) {
            // this is for changing the new static field value in the weave class only
        }
    }

    public abstract static class UnmatchedNestedWeave {
        private static String newStatic = "newStatic";
        private double newMember = 1.0;

        public void setNewMember(final double value) {
            new Runnable() {
                public void run() {
                    newMember = value;
                }
            }.run(); // we use a runnable to test that the generated synthetic mutator gets rewritten properly
        }

        public static void setNewStatic(final String value) {
            new Runnable() {
                public void run() {
                    newStatic = value;
                }
            }.run(); // we use a runnable to test that the generated synthetic mutator gets rewritten properly
        }

        public String usesAnonInner(final String weave) {
            String str = (new Object() {
                public String toString() {
                    return weave;
                }
            }).toString();
            return str + " " + Weaver.callOriginal();
        }

        public String usesAnonInnerWithNewMembers(final String weave) {
            String str = (new Object() {
                public String toString() {
                    return weave + " " + newMember;
                }
            }).toString();
            return str + " " + Weaver.callOriginal();
        }

        public String usesInner() {
            return new Inner().toString() + " " + Weaver.callOriginal();
        }

        public class Inner {
            public String toString() {
                return "inner";
            }
        }

        public String usesStaticInner() {
            return new StaticInner().toString() + " " + Weaver.callOriginal();
        }

        public static class StaticInner {
            public String toString() {
                return "static inner";
            }
        }

        public String usesStaticInnerWithNewMembers() {
            return new StaticInnerWithNewMembers().toString() + " " + Weaver.callOriginal();
        }

        public static class StaticInnerWithNewMembers {
            public String toString() {
                return "static inner " + newStatic;
            }
        }
    }

    @Test
    public void testChainingAnonClasses() throws Exception {
        ChainingAnonOriginal original = new ChainingAnonOriginal();
        assertEquals("wrap callable", original.getCallable().call());
    }

    public static class ChainingAnonOriginal {

        public Callable<String> getCallable() {
            return new Callable<String>() {

                @Override
                public String call() throws Exception {
                    return "callable";
                }

            };
        }
    }

    public static class ChainingAnonWeave {

        public Callable<String> getCallable() {
            final Callable<String> callable = Weaver.callOriginal();
            return new Callable<String>() {

                @Override
                public String call() throws Exception {
                    return "wrap " + callable.call();
                }
            };
        }
    }

    @Test
    public void testUsingUnrelatedAnonClasses() throws Exception {
        UsesUnrelatedAnonOriginal original = new UsesUnrelatedAnonOriginal();
        assertEquals("someString", original.doSomething());
    }

    public static class UnrelatedClass {
        public String someString = "someString";

        public Builder newBuilder() {
            return new Builder();
        }

        public static class Builder {
            UnrelatedClass build() {
                return new UnrelatedClass();
            }
        }
    }

    public static class UsesUnrelatedAnonOriginal {
        String doSomething() {
            return null;
        }
    }

    public static class UsesUnrelatedAnonWeave {
        String doSomething() {
            return new UnrelatedClass.Builder().build().someString;
        }
    }

}
