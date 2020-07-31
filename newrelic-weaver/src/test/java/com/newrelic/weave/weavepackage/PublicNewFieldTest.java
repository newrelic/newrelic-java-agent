/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.classloading.ClassLoaderUtils;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PublicNewFieldTest {
    private static final ClassLoader CLASSLOADER = PublicNewFieldTest.class.getClassLoader();
    private static final String PREFIX = "com.newrelic.weave.weavepackage.PublicNewFieldTest$";

    @BeforeClass
    public static void beforeClass() throws Exception {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes(PREFIX + "WeaveOriginal"));
        weaveBytes.add(WeaveTestUtils.getClassBytes(PREFIX + "WeaveSubClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes(PREFIX + "WeaveNonDefaultConstructorSubClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes(PREFIX + "WeaveNonDefaultConstructorSubClass2"));
        weaveBytes.add(WeaveTestUtils.getClassBytes(PREFIX + "WeaveUtilitySubClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes(PREFIX + "WeaveAnotherOriginal"));
        weaveBytes.add(WeaveTestUtils.getClassBytes(PREFIX + "WeaveUtilityClass"));
        for (byte[] bytes : weaveBytes) {
            Assert.assertNotNull(bytes);
        }
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        WeavePackage testPackage = new WeavePackage(config, weaveBytes);
        ClassCache cache = new ClassCache(new ClassLoaderFinder(CLASSLOADER));
        PackageValidationResult result = testPackage.validate(cache);
        WeaveTestUtils.expectViolations(result.getViolations());

        weaveClass(PREFIX + "Original", result, cache);
        weaveClass(PREFIX + "OriginalSubClass", result, cache);
        weaveClass(PREFIX + "OriginalNonDefaultConstructorSubClass", result, cache);
        weaveClass(PREFIX + "OriginalNonDefaultConstructorSubClass2", result, cache);
        weaveClass(PREFIX + "AnotherOriginal", result, cache);

        NewClassAppender.appendClasses(CLASSLOADER, result.computeUtilityClassBytes(cache));
    }

    private static void weaveClass(String origName, PackageValidationResult result, ClassCache cache)
            throws IOException {
        ClassInformation ci = cache.getClassInformation(origName);
        byte[] compositeBytes = result.weave(WeaveUtils.getClassInternalName(origName),
                ci.getAllSuperNames(cache).toArray(new String[0]),
                ci.getAllInterfaces(cache).toArray(new String[0]),
                WeaveTestUtils.getClassBytes(origName), cache).getCompositeBytes(cache);
        Assert.assertNotNull("Unable to weave: " + origName, compositeBytes);
        Assert.assertFalse("Original class should not be already loaded: " + origName,
                ClassLoaderUtils.isClassLoadedOnClassLoader(CLASSLOADER, WeaveUtils.getClassBinaryName(origName)));
        WeaveTestUtils.addToContextClassloader(origName, compositeBytes);
    }

    @Test
    public void testPublicNewFields() {
        Original orig = new Original();
        Assert.assertTrue(orig.isWeaved());
        Assert.assertEquals("weaverNewField", Original.getStaticNewField());
        Assert.assertEquals(66, orig.getNewField());

        AnotherOriginal anotherOrig = new AnotherOriginal();
        Assert.assertEquals("weaverNewField", AnotherOriginal.getStaticNewField());
        Assert.assertEquals(66, anotherOrig.getNewField());

        Assert.assertEquals("weaverNewField66", WeaveUtilityClass.utilMethod());
    }

    @Test
    public void testProtectedNewFields() {
        OriginalSubClass sub = new OriginalSubClass();
        Assert.assertTrue(sub.isWeaved());
        Assert.assertTrue(sub.isSubWeaved());
        Assert.assertEquals("weaverNewField", OriginalSubClass.getStaticNewField());
        Assert.assertEquals(66, sub.getNewField());
        Assert.assertEquals("Protected NewField 1 Override", sub.getProtectedNewField1());
        Assert.assertEquals("Protected NewField 2 made public", sub.getProtectedNewField2());
        Assert.assertEquals("Protected NewField 3", sub.getProtectedNewField3());

        WeaveUtilitySubClass utilitySub = new WeaveUtilitySubClass();
        Assert.assertEquals("Protected NewField 3", utilitySub.getProtectedNewField3());
    }

    @Test
    public void testPackagePrivateNewFieldCalls() {
        Original orig = new Original();
        Assert.assertTrue(orig.isWeaved());
        Assert.assertEquals("packagePrivateNewField", orig.getPackagePrivateField());

        AnotherOriginal anotherOrig = new AnotherOriginal();
        Assert.assertEquals("packagePrivateNewField", anotherOrig.getPackagePrivateField());

        Assert.assertEquals("packagePrivateNewField", WeaveUtilityClass.getPackagePrivateField());
    }

    @Test
    public void testProtectedNewFieldsNonDefaultConstructor() {
        OriginalNonDefaultConstructorSubClass sub = new OriginalNonDefaultConstructorSubClass(new Object());
        Assert.assertTrue(sub.isWeaved());
        Assert.assertTrue(sub.isSubWeaved());
        Assert.assertEquals("weaverNewField", OriginalNonDefaultConstructorSubClass.getStaticNewField());
        Assert.assertEquals(66, sub.getNewField());
        Assert.assertEquals("Protected NewField 1 Override", sub.getProtectedNewField1());
        Assert.assertEquals("Protected NewField 2 made public", sub.getProtectedNewField2());
        Assert.assertEquals("Protected NewField 3", sub.getProtectedNewField3());

        WeaveUtilitySubClass utilitySub = new WeaveUtilitySubClass();
        Assert.assertEquals("Protected NewField 3", utilitySub.getProtectedNewField3());

        OriginalNonDefaultConstructorSubClass2 sub2 = new OriginalNonDefaultConstructorSubClass2(new Object());
        Assert.assertTrue(sub2.isWeaved());
        Assert.assertNotNull(sub2.getSomeObject());
    }

    @AfterClass
    public static void afterClass() {
        Assert.assertFalse("Should not load weave class.", ClassLoaderUtils.isClassLoadedOnClassLoader(CLASSLOADER,
                PREFIX + "WeaveOriginal"));
        Assert.assertFalse("Should not load weave class.", ClassLoaderUtils.isClassLoadedOnClassLoader(CLASSLOADER,
                PREFIX + "WeaveSubClass"));
        Assert.assertFalse("Should not load weave class.", ClassLoaderUtils.isClassLoadedOnClassLoader(CLASSLOADER,
                PREFIX + "WeaveAnotherOriginal"));
        Assert.assertFalse("Should not load weave class.", ClassLoaderUtils.isClassLoadedOnClassLoader(CLASSLOADER,
                PREFIX + "WeaveNonDefaultConstructorSubClass"));
        Assert.assertFalse("Should not load weave class.", ClassLoaderUtils.isClassLoadedOnClassLoader(CLASSLOADER,
                PREFIX + "WeaveNonDefaultConstructorSubClass2"));
    }

    public static class Original {
        public static String getStaticNewField() {
            return "original";
        }
        public int getNewField() {
            return -1;
        }

        public String getPackagePrivateField() {
            return "original";
        }

        public boolean isWeaved() {
            return false;
        }
    }

    public static class OriginalSubClass extends Original {
        @Override
        public boolean isWeaved() {
            return false;
        }

        public boolean isSubWeaved() {
            return false;
        }

        public String getProtectedNewField1() {
            return "target";
        }

        public String getProtectedNewField2() {
            return "target";
        }

        public String getProtectedNewField3() {
            return "target";
        }

    }

    public static class OriginalNonDefaultConstructorSubClass extends Original {

        private final Object someObject;

        public OriginalNonDefaultConstructorSubClass(Object someObject) {
            this.someObject = someObject;
        }

        @Override
        public boolean isWeaved() {
            return false;
        }

        public boolean isSubWeaved() {
            return false;
        }

        public String getProtectedNewField1() {
            return "target";
        }

        public String getProtectedNewField2() {
            return "target";
        }

        public String getProtectedNewField3() {
            return "target";
        }

    }


    public static class OriginalNonDefaultConstructorSubClass2 extends OriginalNonDefaultConstructorSubClass {

        private final Object someObject;

        public OriginalNonDefaultConstructorSubClass2(Object object) {
            super(object);
            this.someObject = object;
        }

        @Override
        public boolean isWeaved() {
            return false;
        }

        public Object getSomeObject() {
            return someObject;
        }
    }

    public static class AnotherOriginal {
        private Original orig;

        public AnotherOriginal() {
            orig = new Original();
            orig.toString();
        }

        public static String getStaticNewField() {
            return "original";
        }
        public int getNewField() {
            return -1;
        }

        public String getPackagePrivateField() {
            return "anotheroriginal";
        }
    }

    @Weave(type = MatchType.BaseClass, originalName = PREFIX + "Original")
    public static class WeaveOriginal {
        @NewField
        public static String staticNF = "weaverNewField";
        @NewField
        public int instanceNF = 66;
        @NewField
        String packagePrivateNewField = "packagePrivateNewField";
        @NewField
        protected String protectedNewField1 = "Protected NewField 1";
        @NewField
        protected String protectedNewField2 = "Protected NewField 2";
        @NewField
        protected String protectedNewField3 = "Protected NewField 3";

        public static String getStaticNewField() {
            return staticNF;
        }

        public int getNewField() {
            return instanceNF;
        }

        public String getPackagePrivateField() {
            return packagePrivateNewField;
        }

        public boolean isWeaved() {
            return true;
        }
    }

    @Weave(originalName = PREFIX + "OriginalSubClass")
    public static class WeaveSubClass extends WeaveOriginal {
        @NewField
        protected String protectedNewField1 = "Protected NewField 1 Override";
        @NewField
        public String protectedNewField2 = "Protected NewField 2 made public";

        public String getProtectedNewField1() {
            return protectedNewField1;
        }

        public String getProtectedNewField2() {
            return protectedNewField2;
        }

        public String getProtectedNewField3() {
            return protectedNewField3;
        }

        public boolean isSubWeaved() {
            return true;
        }
    }

    @Weave(originalName = PREFIX + "AnotherOriginal")
    public static class WeaveAnotherOriginal {
        private WeaveOriginal orig;

        public static String getStaticNewField() {
            return WeaveOriginal.staticNF;
        }

        public int getNewField() {
            return orig.instanceNF;
        }

        public String getPackagePrivateField() {
            return orig.packagePrivateNewField;
        }
    }

    @Weave(originalName = PREFIX + "OriginalNonDefaultConstructorSubClass")
    public static class WeaveNonDefaultConstructorSubClass extends WeaveOriginal {

        public WeaveNonDefaultConstructorSubClass(Object someObject) {
        }

        @NewField
        protected String protectedNewField1 = "Protected NewField 1 Override";
        @NewField
        public String protectedNewField2 = "Protected NewField 2 made public";

        public String getProtectedNewField1() {
            return protectedNewField1;
        }

        public String getProtectedNewField2() {
            return protectedNewField2;
        }

        public String getProtectedNewField3() {
            return protectedNewField3;
        }

        public boolean isSubWeaved() {
            return true;
        }
    }

    @Weave(originalName = PREFIX + "OriginalNonDefaultConstructorSubClass2")
    public static class WeaveNonDefaultConstructorSubClass2 extends OriginalNonDefaultConstructorSubClass {

        public WeaveNonDefaultConstructorSubClass2(Object object) {
            super(new Object());
        }

        public boolean isWeaved() {
            return true;
        }
    }

    public static class WeaveUtilityClass {
        private static WeaveOriginal orig = new WeaveOriginal();

        public static String utilMethod() {
            return orig.staticNF + orig.instanceNF;
        }

        public static String getPackagePrivateField() {
            return orig.packagePrivateNewField;
        }
    }

    public static class WeaveUtilitySubClass extends WeaveOriginal {
        public String getProtectedNewField3() {
            return protectedNewField3;
        }
    }
}
