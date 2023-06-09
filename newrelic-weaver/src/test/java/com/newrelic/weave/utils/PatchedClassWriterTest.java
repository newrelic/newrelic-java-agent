package com.newrelic.weave.utils;

import com.newrelic.weave.utils.testclasses.ChildClass1;
import com.newrelic.weave.utils.testclasses.ChildClass2;
import com.newrelic.weave.utils.testclasses.TopLevel;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;

public class PatchedClassWriterTest {

    PatchedClassWriter api = new PatchedClassWriter(ClassWriter.COMPUTE_FRAMES, new ClassCache(new ClassLoaderFinder(Thread.currentThread().getContextClassLoader())));
    @Test
    public void test_getCommonSuperClass_sameClass() {
        String className = PatchedClassWriterTest.class.getName();
        Assert.assertEquals(className, api.getCommonSuperClass(className, className));
    }

    @Test
    public void test_getCommonSuperClass_commonSuper() {
        Assert.assertEquals(TopLevel.class.getName().replace('.','/'),
                api.getCommonSuperClass(ChildClass1.class.getName(), ChildClass2.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_noCommonSuper() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass(ChildClass1.class.getName(), PatchedClassWriterTest.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_invalidClasses() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass("noexist1", "noexist2"));
    }
}
