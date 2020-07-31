/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import java.io.IOException;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import com.newrelic.agent.util.asm.Utils;

public class ClassMatcherTest {

    @Test
    public void interfaceMatcher() throws IOException {
        InterfaceMatcher matcher = new InterfaceMatcher(Type.getType(List.class).getInternalName());

        assertMatch(matcher, ArrayList.class, true);
        assertMatch(matcher, HashMap.class, false);
    }

    @Test
    public void interfaceMatcher2() throws IOException {
        InterfaceMatcher matcher = new InterfaceMatcher(Collection.class.getName());
        assertMatch(matcher, ArrayList.class, true);
        assertMatch(matcher, Vector.class, true);
        assertMatch(matcher, Object.class, false);
    }

    @Test
    public void interfaceMatcher_missing() throws IOException {
        InterfaceMatcher matcher = new InterfaceMatcher("com/test/Dude");
        assertMatch(matcher, ArrayList.class, false);
        assertMatch(matcher, Vector.class, false);
        assertMatch(matcher, Object.class, false);
    }

    @Test
    public void interfaceMatcher_class() throws IOException {
        InterfaceMatcher matcher = new InterfaceMatcher(Type.getType(Object.class).getInternalName());
        assertMatch(matcher, ArrayList.class, false);
        assertMatch(matcher, Vector.class, false);
        assertMatch(matcher, Object.class, false);
    }

    @Test
    public void orMatcherTest() throws IOException {
        ClassMatcher matcher = new OrClassMatcher(new ExactClassMatcher(intern(String.class)), new ExactClassMatcher(
                intern(HashSet.class)));

        assertOrMatcher(matcher);
    }

    @Test
    public void orMatcherStringTest() throws IOException {
        ClassMatcher matcher = ExactClassMatcher.or(intern(String.class), intern(HashSet.class));

        assertOrMatcher(matcher);
    }

    @Test
    public void childClassMatcherTest1() throws IOException {
        Type type = Type.getType(AbstractList.class);
        ChildClassMatcher matcher = new ChildClassMatcher(type.getInternalName());

        assertMatch(matcher, AbstractList.class, false);
        assertMatch(matcher, ArrayList.class, true);
        assertMatch(matcher, Vector.class, true);
        assertMatch(matcher, AbstractMap.class, false);

        Assert.assertTrue(matcher.getClassNames().contains(type.getInternalName()));
    }

    @Test
    public void childClassMatcherTest2() throws IOException {
        String name = "java/util/AbstractList";
        String child = "java/util/ArrayList";
        ChildClassMatcher matcher = new ChildClassMatcher(name, false, new String[] { child });
        assertMatch(matcher, ArrayList.class, true);
        assertMatch(matcher, Vector.class, true);

        Assert.assertTrue(matcher.getClassNames().contains(child));
        Assert.assertEquals(2, matcher.getClassNames().size());
    }

    @Test
    public void childClassMatcherTest_missing() throws IOException {
        String name = "java/util/Dude";
        ChildClassMatcher matcher = new ChildClassMatcher(name);
        assertMatch(matcher, ArrayList.class, false);
        assertMatch(matcher, Vector.class, false);
        assertMatch(matcher, AbstractMap.class, false);
    }

    @Test
    public void childClassMatcherTest_object() throws IOException {
        ChildClassMatcher matcher = new ChildClassMatcher(Type.getType(Object.class).getInternalName());
        assertMatch(matcher, ArrayList.class, true);
        assertMatch(matcher, Vector.class, true);
        assertMatch(matcher, AbstractMap.class, true);
        assertMatch(matcher, Collections.class, true);
    }

    @Test
    public void allClasses() throws IOException {
        ClassMatcher matcher = new AllClassesMatcher();
        assertMatch(matcher, ArrayList.class, true);
        assertMatch(matcher, Vector.class, true);
        assertMatch(matcher, AbstractMap.class, true);
        assertMatch(matcher, Collections.class, true);
        assertMatch(matcher, List.class, false);
        Assert.assertTrue(matcher.getClassNames().isEmpty());
    }

    private void assertOrMatcher(ClassMatcher matcher) throws IOException {
        Assert.assertTrue(matcher.isMatch(String.class));
        Assert.assertTrue(matcher.isMatch(HashSet.class));
        Assert.assertFalse(matcher.isMatch(Collection.class));

        Assert.assertTrue(matcher.isMatch(null, getClassReader(HashSet.class)));
    }

    private static void assertMatch(ClassMatcher matcher, Class<?> clazz, boolean isMatch) throws IOException {
        Assert.assertEquals(isMatch, matcher.isMatch(clazz));
        ClassReader classReader = getClassReader(clazz);
        Assert.assertEquals(isMatch, matcher.isMatch(clazz.getClass().getClassLoader(), classReader));
    }

    private String intern(Class<?> clazz) {
        return Type.getType(clazz).getInternalName();
    }

    public static ClassReader getClassReader(Class<?> clazz) throws IOException {
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return Utils.readClass(classLoader, clazz.getName().replace('.', '/'));
    }
}
