/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.ClassName;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.MethodParameters;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReinstrumentUtilsTest {

    public class SampleObject {
        private boolean hello;

        private boolean getHello() {
            return hello;
        }

        protected void setHello(boolean pHello) {
            hello = pHello;
        }

        public String doTheWork(String worker, int time) {
            return worker + time;
        }

        public String falala() {
            return "falala";
        }

        private String yada(String additional) {
            return "yada" + additional;
        }

        protected String yada(int additional) {
            return "yada" + additional;
        }
    }

    private static void setClassName(Pointcut pc, Class clazz) {
        ClassName name = new ClassName();
        name.setValue(clazz.getName());
        pc.setClassName(name);
    }

    @Test
    public void checkInputMatchesClasses() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();
        Instrumentation inst = new Instrumentation();
        ext.setInstrumentation(inst);

        Pointcut pc = new Pointcut();
        inst.getPointcut().add(pc);
        setClassName(pc, SampleObject.class);
        Method m1 = new Method();
        m1.setName("getHello");
        MethodParameters params1 = new MethodParameters(new ArrayList<String>());
        m1.setParameters(params1);

        Method m2 = new Method();
        m2.setName("setHello");
        MethodParameters params2 = new MethodParameters(Arrays.asList("boolean"));
        m2.setParameters(params2);

        Method m3 = new Method();
        m3.setName("doTheWork");
        MethodParameters params3 = new MethodParameters(Arrays.asList("java.lang.String", "int"));
        m3.setParameters(params3);

        pc.getMethod().addAll(Arrays.asList(m1, m2, m3));

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNull("There should not have been an error. Errors: " + actual, actual);
    }

    @Test
    public void checkInputNotMatchingClasses() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();
        Instrumentation inst = new Instrumentation();
        ext.setInstrumentation(inst);

        Pointcut pc = new Pointcut();
        inst.getPointcut().add(pc);
        setClassName(pc, SampleObject.class);

        // this should match
        Method m1 = new Method();
        m1.setName("falala");
        MethodParameters params1 = new MethodParameters(new ArrayList<String>());
        m1.setParameters(params1);

        // this should not match
        Method m2 = new Method();
        m2.setName("notAMatch");
        MethodParameters params2 = new MethodParameters(Arrays.asList("boolean"));
        m2.setParameters(params2);

        // this should not match
        Method m3 = new Method();
        m3.setName("getHello");
        MethodParameters params3 = new MethodParameters(Arrays.asList("java.lang.String", "int"));
        m3.setParameters(params3);

        pc.getMethod().addAll(Arrays.asList(m1, m2, m3));

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNotNull("There should not have been an error. Errors: " + actual, actual);
        Assert.assertFalse("The method " + m1.getName() + " should  NOT be in the error message",
                ((String) actual).contains(m1.getName()));
        Assert.assertTrue("The method " + m2.getName() + " should be in the error message",
                ((String) actual).contains(m2.getName()));
        Assert.assertTrue("The method " + m3.getName() + " should be in the error message",
                ((String) actual).contains(m3.getName()));
    }

    @Test
    public void checkInputMildCardClasses() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();
        Instrumentation inst = new Instrumentation();
        ext.setInstrumentation(inst);

        Pointcut pc = new Pointcut();
        inst.getPointcut().add(pc);
        setClassName(pc, SampleObject.class);

        // this should match
        Method m1 = new Method();
        m1.setName("yada");

        pc.getMethod().add(m1);

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNull("There should not have been an error. Errors: " + actual, actual);
    }

    @Test
    public void checkInputClassesNullPointers1() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNull("There should not have been an error. Errors: " + actual, actual);

    }

    @Test
    public void checkInputClassesNullPointers2() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();
        Instrumentation inst = new Instrumentation();
        ext.setInstrumentation(inst);

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNull("There should not have been an error. Errors: " + actual, actual);

    }

    @Test
    public void checkInputClassesNullPointers3() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();
        Instrumentation inst = new Instrumentation();
        ext.setInstrumentation(inst);

        Pointcut pc = new Pointcut();
        inst.getPointcut().add(pc);
        // no class name set

        Method m1 = new Method();
        m1.setName("falala");
        MethodParameters params1 = new MethodParameters(new ArrayList<String>());
        m1.setParameters(params1);

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNull("There should not have been an error. Errors: " + actual, actual);

    }

    @Test
    public void checkInputClassesNullPointers4() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();
        Instrumentation inst = new Instrumentation();
        ext.setInstrumentation(inst);

        Pointcut pc = new Pointcut();
        inst.getPointcut().add(pc);
        setClassName(pc, SampleObject.class);

        Method m1 = new Method();
        // no method name
        MethodParameters params1 = new MethodParameters(new ArrayList<String>());
        m1.setParameters(params1);

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNull("There should not have been an error. Errors: " + actual, actual);

    }

    @Test
    public void checkInputClassesNullPointers5() {
        SampleObject obj = new SampleObject();

        ReinstrumentResult result = new ReinstrumentResult();
        obj.getClass().getClassLoader();
        Set<ClassLoader> loaders = new HashSet<>();
        loaders.add(this.getClass().getClassLoader());

        Extension ext = new Extension();
        Instrumentation inst = new Instrumentation();
        ext.setInstrumentation(inst);

        Pointcut pc = new Pointcut();
        inst.getPointcut().add(pc);
        setClassName(pc, SampleObject.class);

        Method m1 = new Method();
        m1.setName("falala");
        // no method parameters set

        Map<String, Class<?>> daClasses = new HashMap<>();
        daClasses.put(SampleObject.class.getName(), SampleObject.class);

        ReinstrumentUtils.checkInputClasses(result, loaders, ext, daClasses);
        Object actual = result.getStatusMap().get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNull("There should not have been an error. Errors: " + actual, actual);

    }
}
