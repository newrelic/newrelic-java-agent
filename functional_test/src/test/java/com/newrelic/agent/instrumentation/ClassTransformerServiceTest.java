/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.instrument.UnmodifiableClassException;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Type;

import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AllMethodsMatcher;
import com.newrelic.agent.service.ServiceFactory;

public class ClassTransformerServiceTest {

    @Test
    public void addTraceMatcher() throws UnmodifiableClassException {
        Assert.assertFalse(TestClass.class.isAnnotationPresent(InstrumentedClass.class));
        ServiceFactory.getClassTransformerService().addTraceMatcher(
                new DefaultClassAndMethodMatcher(new ExactClassMatcher(
                        Type.getType(ClassTransformerServiceTest.class).getInternalName() + "$TestClass"),
                        new AllMethodsMatcher()), "dude");

        ServiceFactory.getCoreService().getInstrumentation().retransformClasses(TestClass.class);

        Assert.assertTrue(TestClass.class.isAnnotationPresent(InstrumentedClass.class));
    }

    private static final class TestClass {
        public void test() {

        }
    }
}
