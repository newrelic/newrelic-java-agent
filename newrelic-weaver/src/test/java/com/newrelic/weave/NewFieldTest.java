/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * New fields / extension class tests.
 */
public class NewFieldTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassWeave weave = WeaveTestUtils.weaveAndAddToContextClassloader("com.newrelic.weave.NewFieldTest$Original",
                "com.newrelic.weave.NewFieldTest$Weave");
        assertNotNull(weave.getMatch().getExtension());

        ClassWeave ctorWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NewFieldTest$CtorOriginal", "com.newrelic.weave.NewFieldTest$CtorWeave");
        assertNotNull(ctorWeave.getMatch().getExtension());

        ClassWeave onceWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NewFieldTest$OnceOriginal", "com.newrelic.weave.NewFieldTest$OnceWeave");
        assertNotNull(onceWeave.getMatch().getExtension());

    }

    @Test
    public void testField() {
        Original original = new Original();
        original.setMemberInt(100);
        assertEquals(210, original.getMemberInt());
        original.setStaticInt(200);
        assertEquals(400, original.getStaticInt());
    }

    public static class Original {

        public int getMemberInt() {
            return 100;
        }

        public void setMemberInt(int i) {
        }

        public int getStaticInt() {
            return 200;
        }

        public void setStaticInt(int i) {
        }
    }

    public static class Weave {
        private static int myNewStaticInt;
        private int myNewInt;
        private int myNewInitializedInt = 10;

        public int getMemberInt() {
            int original = Weaver.callOriginal();
            return original + myNewInt + myNewInitializedInt;
        }

        public void setMemberInt(int i) {
            myNewInt = i;
        }

        public int getStaticInt() {
            int original = Weaver.callOriginal();
            return original + myNewStaticInt;
        }

        public void setStaticInt(int i) {
            myNewStaticInt = i;
        }
    }

    @Test
    public void testConstructors() {
        CtorOriginal original = new CtorOriginal();
        assertEquals("originalField", original.getOriginalField());
        assertEquals("newField", original.getNewField());

        original = new CtorOriginal("myOriginalField");
        assertEquals("myOriginalField", original.getOriginalField());
        assertNull(original.getNewField());
    }

    public static class CtorOriginal {
        private String originalField;

        public CtorOriginal() {
            this("originalField");
        }

        public CtorOriginal(String originalField) {
            this.originalField = originalField;
            if (System.currentTimeMillis() < 0) {
                new CtorOriginal("").getOriginalField();
            }
        }

        public String getOriginalField() {
            return originalField;
        }

        public String getNewField() {
            return null; // to access new field in test cases
        }
    }

    public static class CtorWeave {
        private String newField;

        public CtorWeave() {
            this.newField = "newField";
        }

        public String getNewField() {
            return newField;
        }
    }

    @Test
    public void testObjectInitOnce() {
        OnceOriginal orig = new OnceOriginal();
        assertEquals("ONE", orig.getNewFieldOne());
        assertEquals("two", orig.getNewFieldTwo());
    }

    public static class OnceOriginal {
        public String getNewFieldOne() {
            return null;
        }

        public String getNewFieldTwo() {
            return null;
        }
    }

    public static class OnceWeave {
        private static final String ONE = new String("ONE");
        private final String two = new String("two");

        public String getNewFieldOne() {
            return ONE;
        }

        public String getNewFieldTwo() {
            return two;
        }
    }

    @Test
    public void testMapInitializedInSuper() throws IOException {
        ClassWeave superWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NewFieldTest$AnInterface", "com.newrelic.weave.NewFieldTest$InterfaceWeave",
                "com.newrelic.weave.NewFieldTest$SuperClass", true);
        assertNotNull(superWeave.getMatch().getExtension());
        ClassWeave superWeave2 = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NewFieldTest$AnInterface", "com.newrelic.weave.NewFieldTest$InterfaceWeave",
                "com.newrelic.weave.NewFieldTest$ChildClass", true);
        assertNotNull(superWeave2.getMatch().getExtension());

        AnInterface anIntr = new AnInterface() {
            @Override
            public boolean isWeaved() {
                return false;
            }

            @Override
            public String getFoo() {
                return null;
            }

        };
        // WeaveTestUtils.printRaw(superWeave.getComposite());
        ChildClass child = new ChildClass("", anIntr);
        Assert.assertTrue(child.isWeaved());
        Assert.assertNotNull(child.getFoo());
    }

    public static class InterfaceWeave {
        @NewField
        private String foo;

        public InterfaceWeave() {
            foo = "foo";
        }

        public boolean isWeaved() {
            return true;
        }

        public String getFoo() {
            return foo;
        }
    }

    public static interface AnInterface {
        boolean isWeaved();

        String getFoo();
    }

    public abstract static class SuperClass implements AnInterface {
        private String str1;
        private AnInterface anIntr;
        private boolean closed;
        private final Object lock;

        public SuperClass(final String str1, final AnInterface anIntr) {
            this.closed = false;
            this.lock = new Object();
            if (null == str1) {
                throw new RuntimeException("fffff1");
            }
            if (null == anIntr) {
                throw new RuntimeException("fffff2");
            }
            this.str1 = str1;
            this.anIntr = anIntr;
        }

        @Override
        public String getFoo() {
            return null;
        }

        @Override
        public boolean isWeaved() {
            return false;
        }
    }

    public static class ChildClass extends SuperClass {
        public ChildClass(String str1, AnInterface anIntr) {
            super(str1, anIntr);
        }
    }

    public static class Overrider {
        final String name;
        final int age;

        public Overrider(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public int hashCode() {
            return name.hashCode() + age;
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Overrider)) {
                return false;
            }

            Overrider other = (Overrider) obj;
            return name.equals(other.name) && age == other.age;
        }

        public int getNumber() {
            return -1;
        }

        public void setNumber(int n) {

        }
    }


    public static class OverriderWeave {

        @NewField
        public int number;

        public int getNumber() {
            return number;
        }

        public void setNumber(int n) {
            number = n;
        }

    }


    @Test
    public void testNewFieldOverrideHashCodeEquals() throws IOException {
        ClassWeave superWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.NewFieldTest$Overrider", "com.newrelic.weave.NewFieldTest$OverriderWeave");
        assertNotNull(superWeave.getMatch().getExtension());

        String name = "overrider";
        int age = 123;
        Overrider one = new Overrider(name, age);

        assertEquals(0, one.getNumber());
        one.setNumber(3);
        assertEquals(3, one.getNumber());

        Overrider two = new Overrider(name, age);
        assertTrue(one.equals(two));
        assertEquals(0, two.getNumber());
    }
}
