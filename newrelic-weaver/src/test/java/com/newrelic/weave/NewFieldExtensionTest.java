/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.ExtensionClassTemplate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Collections;

/**
 * New fields / extension class tests.
 */
public class NewFieldExtensionTest {
    public static int GET_EXTENSION_COUNTS = 0;
    public static int REMOVAL_COUNTS = 0;

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassNode customExtTemplate = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                WeaveUtils.getClassResourceName("com.newrelic.weave.NewFieldExtensionTest$CountingExtension"),
                NewFieldExtensionTest.class.getClassLoader()));

        final String originalName = "com.newrelic.weave.NewFieldExtensionTest$Original";
        final String weaveName = "com.newrelic.weave.NewFieldExtensionTest$Weave";
        final String targetName = originalName;
        final boolean isBaseMatch = false;
        final ClassNode errorNode = WeaveTestUtils.getErrorHandler();

        ClassWeave weave = WeaveTestUtils.weaveAndAddToContextClassloader(originalName, weaveName, targetName,
                isBaseMatch, Collections.<String>emptySet(), Collections.<String>emptySet(), errorNode, customExtTemplate);
        Assert.assertNotNull(weave.getMatch().getExtension());
        Assert.assertTrue(new Original().isWeaved());
    }

    private static class CountingExtension extends ExtensionClassTemplate {
        public static java.util.WeakHashMap<Object, CountingExtension> INSTANCE_MAP = new java.util.WeakHashMap<>();

        public static synchronized CountingExtension getAndRemoveExtension(Object instance) {
            NewFieldExtensionTest.REMOVAL_COUNTS++;
            return INSTANCE_MAP.remove(instance);
        }

        public static synchronized CountingExtension getExtension(Object instance) {
            Assert.assertEquals("Extension field should not be altered.", 99, newStaticField);
            Assert.assertEquals("Extension method should not be altered.", 100, get_newField((Original) null));
            Assert.assertEquals("Extension method should not be altered.", 100, get_newField());
            Assert.assertEquals("Extension method should not be altered.", 101, ext_get_newField((Original) null));
            NewFieldExtensionTest.GET_EXTENSION_COUNTS++;
            CountingExtension result = INSTANCE_MAP.get(instance);
            if(result == null) {
                result = new CountingExtension();
                INSTANCE_MAP.put(instance, result);
            }
            return result;
        }

        // conflicting field name
        private static int newStaticField = 99;
        // conflicting method sig
        public static int get_newField(Original orig) {
            return newStaticField+1;
        }

        public static int ext_get_newField(Original orig) {
            return newStaticField+2;
        }
        public static int get_newField() {
            return newStaticField+1;
        }
    }

    @Test
    public void testCustomExtension() {
        Original orig = new Original();
        int preExtensionCount = GET_EXTENSION_COUNTS;
        Assert.assertEquals(9, orig.getNewField());
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(8, orig.getNewStaticField());
        Assert.assertEquals(preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(9, orig.getNewField());
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
    }

    @Test
    public void testOnlyOneMapOperation() {
        Original orig = new Original();
        int preExtensionCount = GET_EXTENSION_COUNTS;
        // extension optimizer should only hit the extension map once.
        // Other uses of the extension class will be done on a local variable.
        orig.multipleNewFieldOps();
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
    }

    @Test
    public void testRemovals() {
        int preRemovalCount = REMOVAL_COUNTS;
        Original orig = new Original();
        int preExtensionCount = GET_EXTENSION_COUNTS;
        orig.setNewField(0);
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(preRemovalCount, REMOVAL_COUNTS);

        orig.setOtherNewField(0);
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(preRemovalCount, REMOVAL_COUNTS);

        orig.setNewBoolean(false);
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(preRemovalCount, REMOVAL_COUNTS);

        orig.setNewFloat(0f);
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(preRemovalCount, REMOVAL_COUNTS);

        orig.setNewDouble(0d);
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(preRemovalCount, REMOVAL_COUNTS);

        orig.setNewString(null);
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        // everything is zero'd or null'd out.
        // the extension object should have been removed from the map
        Assert.assertEquals(++preRemovalCount, REMOVAL_COUNTS);

        // a new extension object will be created for the GETFIELDs
        Assert.assertEquals(0, orig.getNewField());
        Assert.assertFalse(orig.getNewBoolean());
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(preRemovalCount, REMOVAL_COUNTS);

        // Remove the new extension object by setting another one to zero
        orig.setNewFloat(0f);
        Assert.assertEquals(++preExtensionCount, GET_EXTENSION_COUNTS);
        Assert.assertEquals(++preRemovalCount, REMOVAL_COUNTS);
    }

    public static class Original {
        public boolean isWeaved() {return false;}
        public int getNewStaticField() {return -1;}
        public void multipleNewFieldOps() {}

        public int getNewField() {return -1;}
        public int getOtherNewField() {return -1;}
        public boolean getNewBoolean() {return false;}
        public float getNewFloat() {return -1f;}
        public double getNewDouble() {return -1d;}
        public String getNewString() {return "original";}
        public void setNewField(int i) {}
        public void setOtherNewField(int i) {}
        public void setNewBoolean(boolean b) {}
        public void setNewFloat(float f) {}
        public void setNewDouble(double d) {}
        public void setNewString(String s) {}
    }

    public static class Weave {
        private static int newStaticField = 8;
        private int newField = 9;
        private int otherNewField = 12;

        private boolean newBoolean = true;
        private float newFloat = 99.4f;
        private double newDouble = 6.66d;
        private String newString = "newString";

        public boolean isWeaved() {
            return true;
        }
        public int getNewStaticField() {
            return newStaticField;
        }
        public void multipleNewFieldOps() {
            int origNewField = newField;
            int origOtherNewField = otherNewField;
            newField++;
            otherNewField++;
            for (int i = 0; i < 10; ++i) {
                if (newField > otherNewField) {
                    otherNewField++;
                } else {
                    newField++;
                }
            }
            newField = origNewField;
            otherNewField = origOtherNewField;
        }

        public int getNewField() {
            return newField;
        }
        public int getOtherNewField() {
            return otherNewField;
        }
        public float getNewFloat() {
            return newFloat;
        }
        public double getNewDouble() {
            return newDouble;
        }
        public String getNewString() {
            return newString;
        }
        public void setNewField(int i) {
            newField = i;
        }
        public void setOtherNewField(int i) {
            otherNewField = i;
        }
        public void setNewBoolean(boolean b) {
            newBoolean = b;
        }
        public void setNewFloat(float f) {
            newFloat = f;
        }
        public void setNewDouble(double d) {
            newDouble = d;
        }
        public void setNewString(String s) {
            newString = s;
        }
    }
}
