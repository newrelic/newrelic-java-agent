/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.jruby.DoNothingClassThatExistsForTesting;
import com.newrelic.weave.utils.Streams;
import org.junit.Assert;
import org.junit.Test;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RetransformPointCutTest {
    private static final Class<?> CLASS_LOADED_BEFORE_AGENT = DoNothingClassThatExistsForTesting.class;
    private static final Class<?> CLASS_LOADED_AFTER_AGENT = HttpURLConnection.class;

    @Test
    public void retransformPointCutClass() throws UnmodifiableClassException {
        InstrumentationProxy ip = ServiceFactory.getCoreService().getInstrumentation();

        ip.retransformClasses(CLASS_LOADED_BEFORE_AGENT);
        Assert.assertTrue(CLASS_LOADED_BEFORE_AGENT.getName() + " should have been retransformed.",
                PointCutClassTransformer.isInstrumented(CLASS_LOADED_BEFORE_AGENT));

        ip.retransformClasses(CLASS_LOADED_AFTER_AGENT);
        Assert.assertTrue(CLASS_LOADED_AFTER_AGENT.getName() + " should have been retransformed.",
                PointCutClassTransformer.isInstrumented(CLASS_LOADED_AFTER_AGENT));
    }

    @Test
    public void redefinePointCutClass() throws Exception {
        InstrumentationProxy ip = ServiceFactory.getCoreService().getInstrumentation();

        System.out.println("Resource: "
                + ClassLoader.getSystemResource("com/newrelic/api/jruby/DoNothingClassThatExistsForTesting.class"));
        ip.redefineClasses(getClassDefinition(CLASS_LOADED_BEFORE_AGENT));
        Assert.assertTrue(CLASS_LOADED_BEFORE_AGENT.getName() + " should have been redefined.",
                PointCutClassTransformer.isInstrumented(CLASS_LOADED_BEFORE_AGENT));

        ip.redefineClasses(getClassDefinition(CLASS_LOADED_AFTER_AGENT));
        Assert.assertTrue(CLASS_LOADED_AFTER_AGENT.getName() + " should have been redefined.",
                PointCutClassTransformer.isInstrumented(CLASS_LOADED_AFTER_AGENT));
    }

    private static ClassDefinition getClassDefinition(Class<?> clazz) throws Exception {
        String internalName = clazz.getName().replace('.', '/') + ".class";
        URL resource = ClassLoader.getSystemResource(internalName);

        if (resource != null) {
            byte[] classfileBuffer;
            classfileBuffer = Streams.read(resource.openStream(), true);
            return new ClassDefinition(clazz, classfileBuffer);
        } else {
            throw new RuntimeException("Unable to find class resource stream: " + clazz);
        }
    }

}
