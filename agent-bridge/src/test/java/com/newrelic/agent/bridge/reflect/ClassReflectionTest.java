package com.newrelic.agent.bridge.reflect;

import org.junit.Assert;
import org.junit.Test;
import sun.misc.Launcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClassReflectionTest {

    public ClassReflectionTest() {

    }

    private Object testPrivateField;
    public Object testPublicField;

    @Test
    public void test_allGetters() throws ClassNotFoundException, NoSuchMethodException,
            NoSuchFieldException, IllegalAccessException {
        ClassLoader classLoader = ClassReflection.getClassLoader(ClassReflection.class);
        ClassReflectionTest theInstance = new ClassReflectionTest();

        Class theClass = ClassReflection.loadClass(classLoader, ClassReflectionTest.class.getName());
        Assert.assertEquals("com.newrelic.agent.bridge.reflect.ClassReflectionTest", theClass.getName());

        Method[] methods = ClassReflection.getDeclaredMethods(theClass);
        Assert.assertNotNull(methods);
        Assert.assertTrue(methods.length > 0);

        methods = ClassReflection.getMethods(theClass);
        Assert.assertNotNull(methods);
        Assert.assertTrue(methods.length > 0);

        Method method = ClassReflection.getDeclaredMethod(theClass, "test_allGetters", null);
        Assert.assertNotNull(method);

        Constructor<?>[] constructors = ClassReflection.getDeclaredConstructors(theClass);
        Assert.assertNotNull(constructors);
        Assert.assertTrue(constructors.length > 0);

        Field[] fields = ClassReflection.getDeclaredFields(theClass);
        Assert.assertNotNull(fields);
        Assert.assertTrue(fields.length > 0);

        // if we stop using Jacoco, these obviously won't work any longer
        Field field = ClassReflection.getDeclaredField(theClass, "testPublicField");
        Assert.assertNotNull(field);
        Object value = ClassReflection.get(field, theInstance);
        Assert.assertNotNull(field);
    }

    @Test(expected = IllegalAccessException.class)
    public void test_get_privateFieldThrowsIllegalAccessException() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        ClassLoader classLoader = ClassReflection.getClassLoader(ClassReflection.class);
        ClassReflectionTest theInstance = new ClassReflectionTest();

        Class theClass = ClassReflection.loadClass(classLoader, ClassReflectionTest.class.getName());
        Field privateField = ClassReflection.getDeclaredField(theClass, "testPrivateField");
        Assert.assertNotNull(privateField);
        ClassReflection.setAccessible(privateField, false);
        Object value = ClassReflection.get(privateField, theInstance);
    }
}
