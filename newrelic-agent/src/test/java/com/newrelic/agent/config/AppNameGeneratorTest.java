package com.newrelic.agent.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AppNameGeneratorTest {
    String previousSysProp;

    @Before
    public void setup() {
        previousSysProp = System.getProperty("sun.java.command");
    }

    @After
    public void teardown() {
        System.setProperty("sun.java.command", previousSysProp);
    }

    @Test
    public void generateAppName_withCmdLinePropertyValue_returnsAppName() {
        // Started with class file arg
        System.setProperty("sun.java.command", "com.nr.MyClass");
        assertEquals("com.nr.MyClass", AppNameGenerator.generateAppName());

        // Started with class file and program args
        System.setProperty("sun.java.command", "com.nr.MyClass arg1 arg2 arg3");
        assertEquals("com.nr.MyClass", AppNameGenerator.generateAppName());

        // Started with jar file arg
        System.setProperty("sun.java.command", "myjar.jar");
        assertEquals("myjar", AppNameGenerator.generateAppName());

        // Started with jar file and program args
        System.setProperty("sun.java.command", "myjar.jar arg1 arg2 arg3");
        assertEquals("myjar", AppNameGenerator.generateAppName());

        System.setProperty("sun.java.command", "");
    }

    @Test
    public void generateAppName_withEmptyCmdLinePropertyValue_returnsNull() {
        System.setProperty("sun.java.command", "");
        assertNull(AppNameGenerator.generateAppName());
    }

    @Test
    public void generateAppName_fromStackTrace_returnsAppName() {
        System.setProperty("sun.java.command", "");
        String s = AppNameGenerator.generateAppName();
        System.out.println(s);
    }
}
