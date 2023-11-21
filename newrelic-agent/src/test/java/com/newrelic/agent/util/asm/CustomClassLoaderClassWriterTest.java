package com.newrelic.agent.util.asm;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;

import java.io.Serializable;

public class CustomClassLoaderClassWriterTest {

    CustomClassLoaderClassWriter api = new CustomClassLoaderClassWriter(ClassWriter.COMPUTE_FRAMES, this.getClass().getClassLoader());

    @Test
    public void test_getCommonSuperClass_sameClass() {
        String className = CustomClassLoaderClassWriterTest.class.getName();
        Assert.assertEquals(className, api.getCommonSuperClass(className, className));
    }

    @Test
    public void test_getCommonSuperClass_commonSuper() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass(String.class.getName(), Integer.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_commonSuper2() {
        Assert.assertEquals(Number.class.getName(),
                api.getCommonSuperClass(Integer.class.getName(), Number.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_interfaces() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass(Comparable.class.getName(), Serializable.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_noCommonSuper() {
        Assert.assertEquals(Object.class.getName().replace('.','/'),
                api.getCommonSuperClass(String.class.getName(), CustomClassLoaderClassWriterTest.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_invalidClass1() {
        Assert.assertEquals(Object.class.getName().replace('.', '/'),
                api.getCommonSuperClass("noexist1", CustomClassLoaderClassWriterTest.class.getName()));
    }

    @Test
    public void test_getCommonSuperClass_invalidClass2() {
        Assert.assertEquals(Object.class.getName().replace('.', '/'),
                api.getCommonSuperClass(CustomClassLoaderClassWriterTest.class.getName(), "noexist2"));
    }

}
