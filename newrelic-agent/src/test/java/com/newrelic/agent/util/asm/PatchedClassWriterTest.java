package com.newrelic.agent.util.asm;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;

public class PatchedClassWriterTest {

    PatchedClassWriter api = new PatchedClassWriter(ClassWriter.COMPUTE_FRAMES, this.getClass().getClassLoader());

    @Test
    public void test_getCommonSuperClass_sameClass() {
        String className = PatchedClassWriterTest.class.getName();
        Assert.assertEquals(className, api.getCommonSuperClass(className, className));
    }

    @Test
    public void test_getCommonSuperClass_commonSuper() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass(String.class.getName(), Integer.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_noCommonSuper() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass(String.class.getName(), PatchedClassWriterTest.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_invalidClasses() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass("noexist1", "noexist2"));
    }
}
