/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.MethodParameters;
import com.newrelic.agent.extension.util.MethodMapper;
import com.newrelic.agent.extension.util.MethodMatcherUtility;
import com.newrelic.agent.extension.util.XmlException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the ExactParamMethodMatcher.
 * 
 * @since Sep 20, 2012
 */
public class OrExactParamMethodMatcherTest {

    @Test
    public void testMatchingBasic() throws XmlException {
        List<String> pts = new ArrayList<>();
        Method m1 = new Method();
        m1.setName("hello");

        pts.add("int[]");
        pts.add("char");
        pts.add("boolean");
        pts.add("float");
        pts.add("byte");
        pts.add("double");
        pts.add("java.lang.String");
        MethodParameters mps = new MethodParameters(pts);
        m1.setParameters(mps);

        Method m2 = new Method();
        m2.setName("configure");
        List<String> ptsForM2 = new ArrayList<>();

        ptsForM2.add("java.util.List<String>");
        ptsForM2.add("java.util.Map<Integer>[][][]");
        ptsForM2.add("short");

        MethodParameters mps2 = new MethodParameters(ptsForM2);
        m2.setParameters(mps2);

        List<Method> methods = new ArrayList<>();
        methods.add(m1);
        methods.add(m2);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("classz", methods, mapper, "EXT");
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "([ICZFBDLjava/lang/String;)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure",
                "([ICZFBDLjava/lang/String;)I", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "([ICZBFDLjava/lang/String;)S",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure",
                "(Ljava/util/List;[[[Ljava/util/Map;S)java/lang/String",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello",
                "(Ljava/util/List;[[[Ljava/util/Map;S)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure",
                "(Ljava/util/List;[[[Ljava/util/Map;)V", com.google.common.collect.ImmutableSet.<String> of()));

    }

    @Test
    public void testMachesWithSameNames() throws XmlException {
        List<String> pts = new ArrayList<>();
        Method m1 = new Method();
        m1.setName("hello");

        pts.add("int[]");
        pts.add("char");
        pts.add("boolean");
        pts.add("float");
        pts.add("byte");
        pts.add("double");
        pts.add("java.lang.String");

        MethodParameters mps1 = new MethodParameters(pts);
        m1.setParameters(mps1);

        Method m2 = new Method();
        m2.setName("hello");
        List<String> ptsForM2 = new ArrayList<>();
        ptsForM2.add("java.util.List<String>");
        ptsForM2.add("java.util.Map<Integer>[][][]");
        ptsForM2.add("short");

        MethodParameters mps2 = new MethodParameters(ptsForM2);
        m2.setParameters(mps2);

        Method m3 = new Method();
        m3.setName("hello");
        List<String> ptsForM3 = new ArrayList<>();
        MethodParameters mps3 = new MethodParameters(ptsForM3);
        m3.setParameters(mps3);

        List<Method> methods = new ArrayList<>();
        methods.add(m1);
        methods.add(m2);
        methods.add(m3);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("classy", methods, mapper, "EXT");
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "([ICZFBDLjava/lang/String;)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure",
                "([ICZFBDLjava/lang/String;)I", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello",
                "([ICZBFDLjava/lang/String;)java/lang/String", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello",
                "(Ljava/util/List;[[[Ljava/util/Map;S)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure",
                "(Ljava/util/List;[[[Ljava/util/Map;S)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello",
                "(Ljava/util/List;[[[[Ljava/util/Map;)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "()java/lang/String",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void testMatchingBasicDuplicates() throws Exception {
        List<String> pts = new ArrayList<>();
        Method m1 = new Method();
        m1.setName("hello");

        pts.add("int");
        pts.add("char");

        MethodParameters mps1 = new MethodParameters(pts);
        m1.setParameters(mps1);

        Method m2 = new Method();
        m2.setName("hello");
        List<String> ptsForM2 = new ArrayList<>();

        ptsForM2.add("int");
        ptsForM2.add("char");

        MethodParameters mps2 = new MethodParameters(pts);
        m2.setParameters(mps2);

        List<Method> methods = new ArrayList<>();
        methods.add(m1);
        methods.add(m2);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("classz", methods, mapper, "EXT");
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "(IC)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void testMatchingBasicTwoMethods() throws Exception {

        Method m1 = new Method();
        m1.setName("hello");
        List<String> pts = new ArrayList<>();

        pts.add("int");
        pts.add("char");

        MethodParameters mps1 = new MethodParameters(pts);
        m1.setParameters(mps1);

        Method m2 = new Method();
        m2.setName("configure");
        List<String> ptsForM2 = new ArrayList<>();
        ptsForM2.add("int");
        ptsForM2.add("char");

        MethodParameters mps2 = new MethodParameters(ptsForM2);
        m2.setParameters(mps2);

        List<Method> methods = new ArrayList<>();
        methods.add(m1);
        methods.add(m2);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("classz", methods, mapper, "EXT");
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "(IC)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure", "(IC)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void testMatchingBasicEmptyParameterType() throws Exception {

        Method m1 = new Method();
        m1.setName("hello");
        List<String> pts = new ArrayList<>();
        MethodParameters mps1 = new MethodParameters(pts);
        m1.setParameters(mps1);

        Method m2 = new Method();
        m2.setName("configure");
        m2.setParameters(new MethodParameters(null));

        List<Method> methods = new ArrayList<>();
        methods.add(m1);
        methods.add(m2);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("classz", methods, mapper, "EXT");
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void testMatchingBasicEmptyParameterTypeMultiples() throws Exception {

        Method m1 = new Method();
        m1.setName("hello");
        List<String> pts = new ArrayList<>();
        m1.setParameters(new MethodParameters(pts));

        Method m2 = new Method();
        m2.setName("configure");
        m2.setParameters(new MethodParameters(null));

        List<Method> methods = new ArrayList<>();
        methods.add(m1);
        methods.add(m2);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher1 = MethodMatcherUtility.createMethodMatcher("classy", methods, mapper, "EXT");
        Assert.assertTrue(matcher1.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher1.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));

        // different class and so this should be okay
        MethodMatcher matcher2 = MethodMatcherUtility.createMethodMatcher("classing", methods, mapper, "EXT");
        Assert.assertTrue(matcher2.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher2.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));

        try {
            // we should fail here
            MethodMatcherUtility.createMethodMatcher("classing", methods, mapper, "EXT");
            Assert.fail("Should fail due to methods already being added.");
        } catch (Exception e) {
            // should get to here
        }
    }

    @Test(expected = XmlException.class)
    public void testMatchingBasicDuplicatesInSecondCall() throws Exception {

        Method m1 = new Method();
        m1.setName("hello");
        List<String> pts = new ArrayList<>();

        pts.add("int");
        pts.add("char");
        MethodParameters mps1 = new MethodParameters(pts);
        m1.setParameters(mps1);

        Method m2 = new Method();
        m2.setName("configure");
        List<String> ptsForM2 = new ArrayList<>();

        ptsForM2.add("int");
        ptsForM2.add("char");

        MethodParameters mps2 = new MethodParameters(ptsForM2);
        m2.setParameters(mps2);

        List<Method> methods = new ArrayList<>();
        methods.add(m1);
        methods.add(m2);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher1 = MethodMatcherUtility.createMethodMatcher("classz", methods, mapper, "EXT");
        Assert.assertTrue(matcher1.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "(IC)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher1.matches(MethodMatcher.UNSPECIFIED_ACCESS, "configure", "(IC)V",
                com.google.common.collect.ImmutableSet.<String> of()));

        MethodMatcherUtility.createMethodMatcher("classz", methods, mapper, "EXT");
    }

    @Test
    public void testNoParamTag() throws Exception {
        Method m1 = new Method();
        m1.setName("hello");

        List<Method> methods = new ArrayList<>();
        methods.add(m1);

        Map<String, MethodMapper> mapper = new HashMap<>();
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("hello", methods, mapper, "EXT");
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "hello", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }
}
