/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.jakarta;

import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;

public class JakartaWebServiceTest {
    static final TransactionDataList transactions = new TransactionDataList();

    @BeforeClass
    public static void beforeClass() {
        ServiceFactory.getTransactionService().addTransactionListener(transactions);
    }

    @Before
    public void setup() {
        transactions.clear();
    }

    @Test
    public void testTracedInterfaceMethods() throws NoSuchMethodException, SecurityException {
        Assert.assertTrue(HelloWorldImpl.class.isAnnotationPresent(InstrumentedClass.class));

        Method method = HelloWorldImpl.class.getMethod("run");
        Assert.assertNotNull(method);
        Assert.assertFalse(method.isAnnotationPresent(InstrumentedMethod.class));

        method = HelloWorldImpl.class.getMethod("getHelloWorld", String.class);
        Assert.assertNotNull(method);
        Assert.assertTrue(method.isAnnotationPresent(InstrumentedMethod.class));
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertEquals("com.newrelic.agent.instrumentation.webservices.JakartaWebServiceVisitor",
                annotation.instrumentationNames()[0]);
        Assert.assertEquals(InstrumentationType.BuiltIn, annotation.instrumentationTypes()[0]);
    }

    @Test
    public void testTracedMethods() throws NoSuchMethodException, SecurityException {
        Assert.assertTrue(JakartaWsExample.class.isAnnotationPresent(InstrumentedClass.class));

        Method method = JakartaWsExample.class.getMethod("run");
        Assert.assertNotNull(method);
        Assert.assertFalse(method.isAnnotationPresent(InstrumentedMethod.class));

        method = JakartaWsExample.class.getMethod("getWebMethod", String.class);
        Assert.assertNotNull(method);
        Assert.assertTrue(method.isAnnotationPresent(InstrumentedMethod.class));
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertEquals("com.newrelic.agent.instrumentation.webservices.JakartaWebServiceVisitor",
                annotation.instrumentationNames()[0]);
        Assert.assertEquals(InstrumentationType.BuiltIn, annotation.instrumentationTypes()[0]);
    }

}
