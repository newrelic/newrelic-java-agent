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
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MethodMatcherTests {

    @Test
    public void exactMatch() {
        ExactMethodMatcher matcher = new ExactMethodMatcher("execute", "()V", "(I)V");

        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));

        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void exactMatchMethodNameOnly() {
        ExactMethodMatcher matcher = new ExactMethodMatcher("execute");

        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));

        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void orMatcher() {
        MethodMatcher matcher = OrMethodMatcher.getMethodMatcher(new ExactMethodMatcher("execute", "()V", "(I)V"),
                new ExactMethodMatcher("go", "()Ljava/lang/String;"), new ExactMethodMatcher("test"));

        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));

        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "execute", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "go", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "go", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "go", "()Ljava/lang/String;",
                com.google.common.collect.ImmutableSet.<String> of()));

        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "test", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "test", "dude",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "test", "",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void testEqualsAndHashCode() {
        List<String> desc = new ArrayList<>(1);
        desc.add("()V");
        MethodMatcher matcher1 = new ExactMethodMatcher("method1", desc);
        Assert.assertTrue(matcher1.equals(matcher1));
        Assert.assertEquals(matcher1.hashCode(), matcher1.hashCode());

        MethodMatcher matcher2 = new ExactMethodMatcher("method1", desc);
        Assert.assertTrue(matcher1.equals(matcher2));
        Assert.assertEquals(matcher1.hashCode(), matcher2.hashCode());

        MethodMatcher matcher3 = new ExactMethodMatcher("method3", desc);
        Assert.assertFalse(matcher1.equals(matcher3));
        Assert.assertNotSame(matcher1.hashCode(), matcher3.hashCode());

        List<String> descForM4 = new ArrayList<>(1);
        desc.add("()J");
        MethodMatcher matcher4 = new ExactMethodMatcher("method1", descForM4);
        Assert.assertFalse(matcher1.equals(matcher4));
        Assert.assertNotSame(matcher1.hashCode(), matcher4.hashCode());

        Assert.assertFalse(matcher1.equals(null));
    }

    @Test
    public void createMethodMatcherNameOnly() throws XmlException {
        Method m1 = new Method();
        m1.setName("m1");

        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("myclass", Arrays.asList(m1),
                new HashMap<String, MethodMapper>(), "EXT");
        Assert.assertNotNull(matcher);
        Assert.assertTrue(matcher + " not of type NameMethodMatcher", matcher instanceof NameMethodMatcher);

        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void createMethodMatcherExactOnly() throws XmlException {
        Method m1 = new Method();
        m1.setName("m1");
        m1.setParameters(new MethodParameters(Arrays.asList("int")));
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("myclass", Arrays.asList(m1),
                new HashMap<String, MethodMapper>(), "EXT");
        Assert.assertNotNull(matcher);
        Assert.assertTrue(matcher + " not of type ExactParamsMethodMatcher",
                matcher instanceof ExactParamsMethodMatcher);

        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void createOrMethodMatcherExactOnly() throws XmlException {
        Method m1 = new Method();
        m1.setName("m1");
        m1.setParameters(new MethodParameters(Arrays.asList("int")));
        Method m2 = new Method();
        m2.setName("m2");
        m2.setParameters(new MethodParameters(Arrays.asList("int[]")));
        Method m3 = new Method();
        m3.setName("m3");
        m3.setParameters(new MethodParameters(null));
        Method m4 = new Method();
        m4.setName("m4");
        m4.setParameters(new MethodParameters(Collections.EMPTY_LIST));
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("myclass", Arrays.asList(m1, m2, m3, m4),
                new HashMap<String, MethodMapper>(), "EXT");
        Assert.assertNotNull(matcher);
        Assert.assertTrue(matcher + " not of type OrMethodMatcher", matcher instanceof OrMethodMatcher);

        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m3", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m3", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m4", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m4", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void createOrMethodMatcherNameOnly() throws XmlException {
        Method m1 = new Method();
        m1.setName("m1");
        Method m2 = new Method();
        m2.setName("m2");
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("myclass", Arrays.asList(m1, m2),
                new HashMap<String, MethodMapper>(), "EXT");
        Assert.assertNotNull(matcher);
        Assert.assertTrue(matcher + " not of type OrMethodMatcher", matcher instanceof OrMethodMatcher);

        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m3", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void createOrMethodMatcherMix() throws XmlException {
        Method m1 = new Method();
        m1.setName("m1");
        m1.setParameters(new MethodParameters(Arrays.asList("int")));
        Method m2 = new Method();
        m2.setName("m2");
        MethodMatcher matcher = MethodMatcherUtility.createMethodMatcher("myclass", Arrays.asList(m1, m2),
                new HashMap<String, MethodMapper>(), "EXT");
        Assert.assertNotNull(matcher);
        Assert.assertTrue(matcher + " not of type OrMethodMatcher", matcher instanceof OrMethodMatcher);

        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m1", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "(I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m2", "([I)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "m3", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void matchStatic() {
        MethodMatcher matcher = new AccessMethodMatcher(Opcodes.ACC_STATIC);

        Assert.assertTrue(matcher.matches(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE, null, null,
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(Opcodes.ACC_PRIVATE, null, null,
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, null, null,
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void matchStaticAndPublic() {

        for (MethodMatcher matcher : Arrays.asList(
                OrMethodMatcher.getMethodMatcher(new AccessMethodMatcher(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC)),
                AndMethodMatcher.getMethodMatcher(new AccessMethodMatcher(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC)))) {

            Assert.assertTrue(matcher.matches(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC, null, null,
                    com.google.common.collect.ImmutableSet.<String> of()));
            Assert.assertTrue(matcher.matches(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC + Opcodes.ACC_NATIVE, null, null,
                    com.google.common.collect.ImmutableSet.<String> of()));

            Assert.assertTrue(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, null, null,
                    com.google.common.collect.ImmutableSet.<String> of()));
            Assert.assertFalse(matcher.matches(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE, null, null,
                    com.google.common.collect.ImmutableSet.<String> of()));
            Assert.assertFalse(matcher.matches(Opcodes.ACC_STATIC, null, null,
                    com.google.common.collect.ImmutableSet.<String> of()));
            Assert.assertFalse(matcher.matches(Opcodes.ACC_PUBLIC, null, null,
                    com.google.common.collect.ImmutableSet.<String> of()));
        }
    }

    @Test
    public void matchReturnType() {
        MethodMatcher matcher = OrMethodMatcher.getMethodMatcher(new ExactReturnTypeMethodMatcher(
                Type.getType(Action.class)));

        Assert.assertTrue(matcher.matches(Opcodes.ACC_PUBLIC, "test", "()Ljavax/swing/Action;",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(matcher.matches(Opcodes.ACC_PUBLIC, "test2", "()Ljavax/swing/Action;",
                com.google.common.collect.ImmutableSet.<String> of()));

        Assert.assertFalse(matcher.matches(Opcodes.ACC_PUBLIC, "test", "(Ljavax/swing/Action;)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void annotationMatcher_Or() {
        MethodMatcher matcher = OrMethodMatcher.getMethodMatcher(new AnnotationMethodMatcher(Type.getType(Trace.class)));

        Assert.assertTrue(matcher.matches(Opcodes.ACC_PUBLIC, "test", "()Ljavax/swing/Action;",
                com.google.common.collect.ImmutableSet.of(Type.getDescriptor(Trace.class))));
        Assert.assertFalse(matcher.matches(Opcodes.ACC_PUBLIC, "test2", "()Ljavax/swing/Action;",
                com.google.common.collect.ImmutableSet.<String> of()));

        Assert.assertFalse(matcher.matches(Opcodes.ACC_PUBLIC, "test", "(Ljavax/swing/Action;)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void annotationMatcher_And() {
        MethodMatcher matcher = AndMethodMatcher.getMethodMatcher(new AnnotationMethodMatcher(Type.getType(Trace.class)));

        Assert.assertTrue(matcher.matches(Opcodes.ACC_PUBLIC, "test", "()Ljavax/swing/Action;",
                com.google.common.collect.ImmutableSet.of(Type.getDescriptor(Trace.class))));
        Assert.assertFalse(matcher.matches(Opcodes.ACC_PUBLIC, "test2", "()Ljavax/swing/Action;",
                com.google.common.collect.ImmutableSet.<String> of()));

        Assert.assertFalse(matcher.matches(Opcodes.ACC_PUBLIC, "test", "(Ljavax/swing/Action;)V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void testNotMethodMatcher() {
        AllMethodsMatcher allMatcher = new AllMethodsMatcher();
        MethodMatcher matcher = new NotMethodMatcher(allMatcher);
        MethodMatcher nestedMatcher = new NotMethodMatcher(matcher);

        Assert.assertTrue(allMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, null, null,
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(matcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, null, null,
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(nestedMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, null, null,
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertEquals(matcher, matcher);
        Assert.assertEquals(nestedMatcher, nestedMatcher);
        Assert.assertEquals(matcher, new NotMethodMatcher(allMatcher));
        Assert.assertEquals(nestedMatcher, new NotMethodMatcher(matcher));
        Assert.assertFalse(matcher.equals(nestedMatcher));
        Assert.assertFalse(nestedMatcher.equals(new NotMethodMatcher(allMatcher)));
        Assert.assertEquals(matcher.hashCode(), matcher.hashCode());
        Assert.assertEquals(matcher.hashCode(), new NotMethodMatcher(allMatcher).hashCode());
        Assert.assertFalse(matcher.hashCode() == nestedMatcher.hashCode());
    }

    @Test
    public void testLambdaMethodMatcher() {
        Assert.assertFalse(new LambdaMethodMatcher("", false).matches(Opcodes.F_NEW, "methodName", null, com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(new LambdaMethodMatcher("methodName", false).matches(Opcodes.ACC_PUBLIC, "methodName", null, com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(new LambdaMethodMatcher("methodName", false).matches(Opcodes.F_NEW, "methodName", null, com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(new LambdaMethodMatcher("", true).matches(Opcodes.ACC_PUBLIC, "methodName", null, com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(new LambdaMethodMatcher("methodName", true).matches(Opcodes.ACC_PUBLIC, "methodName", null, com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void testReturnTypeMethodMatcher() {
        Assert.assertFalse(new ReturnTypeMethodMatcher(Collections.<String>emptyList()).matches(Opcodes.ACC_PUBLIC, null, Type.getDescriptor(String.class), com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(new ReturnTypeMethodMatcher(Collections.singletonList(Type.getDescriptor(Boolean.class))).matches(Opcodes.ACC_PUBLIC, null, Type.getDescriptor(String.class), com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(new ReturnTypeMethodMatcher(Collections.singletonList(Type.getDescriptor(String.class))).matches(Opcodes.ACC_PUBLIC, null, Type.getDescriptor(String.class), com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(new ReturnTypeMethodMatcher(Arrays.asList(Type.getDescriptor(Boolean.class), Type.getDescriptor(Short.class))).matches(Opcodes.ACC_PUBLIC, null, Type.getDescriptor(String.class), com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(new ReturnTypeMethodMatcher(Arrays.asList(Type.getDescriptor(Boolean.class), Type.getDescriptor(String.class))).matches(Opcodes.ACC_PUBLIC, null, Type.getDescriptor(String.class), com.google.common.collect.ImmutableSet.<String> of()));
    }
}
