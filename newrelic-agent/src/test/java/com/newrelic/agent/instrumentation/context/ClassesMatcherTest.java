/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.classmatchers.AllClassesMatcher;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.methodmatchers.AnnotationMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactReturnTypeMethodMatcher;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.Assert.*;

public class ClassesMatcherTest {

    InstrumentationContextClassMatcherHelper matcherHelper = new InstrumentationContextClassMatcherHelper();

    @Test
    public void getMatchingClasses_Simple() {
        ClassMatchVisitorFactory matcher = OptimizedClassMatcherBuilder.newBuilder()
                .addClassMethodMatcher(
                        new DefaultClassAndMethodMatcher(
                                new ExactClassMatcher(Type.getType(ArrayList.class).getInternalName()),
                                new ExactMethodMatcher("size", "()I")))
                .build();

        Set<Class<?>> matchingClasses = ClassesMatcher.getMatchingClasses(
                Collections.singletonList(matcher), matcherHelper,
                ArrayList.class);

        assertFalse(matchingClasses.isEmpty());
        assertTrue(matchingClasses.contains(ArrayList.class));
    }

    @Trace
    @Test
    public void getMatchingClasses_Annotation() {
        ClassMatchVisitorFactory matcher = OptimizedClassMatcherBuilder.newBuilder()
                .addClassMethodMatcher(
                        new DefaultClassAndMethodMatcher(
                                new AllClassesMatcher(),
                                new AnnotationMethodMatcher(Type.getType(Trace.class))))
                .build();

        Set<Class<?>> matchingClasses = ClassesMatcher.getMatchingClasses(
                Collections.singletonList(matcher), matcherHelper,
                ClassesMatcherTest.class,
                ArrayList.class,
                HashMap.class);

        assertEquals(1, matchingClasses.size());
        assertTrue(matchingClasses.contains(ClassesMatcherTest.class));
    }

    @Trace
    @Test
    public void getMatchingClasses_ReturnType() {
        ClassMatchVisitorFactory matcher = OptimizedClassMatcherBuilder.newBuilder()
                .addClassMethodMatcher(
                        new DefaultClassAndMethodMatcher(
                                new AllClassesMatcher(),
                                new ExactReturnTypeMethodMatcher(Type.getType(List.class))))
                .build();

        Set<Class<?>> matchingClasses = ClassesMatcher.getMatchingClasses(
                Collections.singletonList(matcher), matcherHelper,
                Arrays.class,
                ArrayList.class,
                HashMap.class,
                TestClass.class,
                Proxy.class);

        assertEquals(3, matchingClasses.size());
        assertTrue(matchingClasses.contains(ArrayList.class));

        assertTrue(matchingClasses.contains(TestClass.class));
        assertTrue(matchingClasses.contains(Arrays.class));
    }

    public static class TestClass {
        public void test() {
        }
        public List getList() {
            return Arrays.asList();
        }
    }
}
