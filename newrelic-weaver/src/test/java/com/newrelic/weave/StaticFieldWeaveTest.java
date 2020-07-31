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

import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.api.agent.weaver.Weaver;

/**
 * Test cases around static fields.
 */
public class StaticFieldWeaveTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassWeave simpleWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.StaticFieldWeaveTest$SimpleOriginal",
                "com.newrelic.weave.StaticFieldWeaveTest$SimpleWeave");
        assertNull(simpleWeave.getMatch().getExtension());

        ClassWeave newWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.StaticFieldWeaveTest$NewOriginal",
                "com.newrelic.weave.StaticFieldWeaveTest$NewWeave");
        assertNotNull(newWeave.getMatch().getExtension());
    }

    @Test
    public void testConstants() {
        assertEquals(1, SimpleOriginal.MY_CONSTANT);
        assertEquals(2, SimpleOriginal.myStatic);
        assertEquals(5, new SimpleOriginal().myField);
    }

    public static class SimpleOriginal {
        public static final int MY_CONSTANT = 1;
        public static int myStatic = 2;
        public int myField = 3;
    }

    public static class SimpleWeave {
        public static final int MY_CONSTANT = Weaver.callOriginal();
        public static int myStatic = Weaver.callOriginal();
        public int myField = 5;
    }

    @Test
    public void testNewConstants() {
        assertEquals("ORIGINAL_CONSTANT", NewOriginal.ORIGINAL_CONSTANT);
        assertEquals("originalStaticField", NewOriginal.originalStaticField);

        assertEquals("ORIGINAL_CONSTANT", NewOriginal.getOriginalConstant());
        assertEquals("originalStaticField", NewOriginal.getOriginalStaticField());

        assertEquals("NEW_CONSTANT", NewOriginal.getNewConstant());
        assertEquals("NEW_CONSTANT_2", NewOriginal.getNewConstant2());
        assertEquals("newStaticField", NewOriginal.getNewStaticField());
        assertEquals("newStaticField2", NewOriginal.getNewStaticField2());
    }

    public static class NewOriginal {
        private static final String ORIGINAL_CONSTANT = "ORIGINAL_CONSTANT";
        private static String originalStaticField = "originalStaticField";

        // methods used to retrieve values from the weave class
        public static String getOriginalConstant() {
            return null;
        }

        public static String getOriginalStaticField() {
            return null;
        }

        public static String getNewConstant() {
            return null;
        }

        public static String getNewConstant2() {
            return null;
        }

        public static String getNewStaticField() {
            return null;
        }

        public static String getNewStaticField2() {
            return null;
        }

        // non-private or protected static methods can be used to initialize a new constant
        public static String someString() {
            return "newStaticField2";
        }
    }

    public static class NewWeave {
        private static final String ORIGINAL_CONSTANT = Weaver.callOriginal();
        private static String originalStaticField = Weaver.callOriginal();

        // some different ways of initializing new static fields
        private static final String NEW_CONSTANT = new StringBuilder().append("NEW_CONSTANT").toString();
        private static final String NEW_CONSTANT_2 = "NEW_CONSTANT_2";
        private static String newStaticField = computeValue();
        private static String newStaticField2 = NewOriginal.someString();

        // new methods are allowed w/private access and are inlined in the class initializer
        private static String computeValue() {
            return "newStaticField";
        }

        public static String getOriginalConstant() {
            return ORIGINAL_CONSTANT;
        }

        public static String getOriginalStaticField() {
            return originalStaticField;
        }

        public static String getNewConstant() {
            return NEW_CONSTANT;
        }

        public static String getNewConstant2() {
            return NEW_CONSTANT_2;
        }

        public static String getNewStaticField() {
            return newStaticField;
        }

        public static String getNewStaticField2() {
            return newStaticField2;
        }
    }
}
