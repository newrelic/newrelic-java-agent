package com.newrelic.weave.utils;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

public class ClassClassFinderTest {
    @Test
    public void findResource_withClassWithCodeSource_returnsProperClassLocation() throws MalformedURLException, ClassNotFoundException {
        Class clazz = Class.forName("com.newrelic.weave.utils.ClassClassFinder");

        ClassClassFinder classClassFinder = new ClassClassFinder(clazz);
        URL result = classClassFinder.findResource("MyClass");
        assertNotNull(result);
    }

    @Test
    public void findResource_withoutClassWithCodeSource_returnsProperClassLocation() throws MalformedURLException, ClassNotFoundException {
        Class clazz = Class.forName("java.lang.String");

        ClassClassFinder classClassFinder = new ClassClassFinder(clazz);
        URL result = classClassFinder.findResource("MyClass");
        assertNull(result);
    }
}
