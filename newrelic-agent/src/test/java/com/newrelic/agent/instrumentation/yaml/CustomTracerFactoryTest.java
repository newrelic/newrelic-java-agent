/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.api.agent.MethodTracer;
import com.newrelic.api.agent.MethodTracerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CustomTracerFactoryTest {
    static final ClassMethodSignature signature = new ClassMethodSignature("", "", "");

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @Before
    public void before() {
        Transaction.clearTransaction();
    }

    @Test
    public void testNoTracerWithoutParent() {
        Transaction transaction = Transaction.getTransaction();
        NoTracerMethodTracerFactory mtf = new NoTracerMethodTracerFactory();
        CustomTracerFactory factory = new CustomTracerFactory(mtf);
        Tracer tracer = factory.getTracer(transaction, signature, this, new Object[0]);
        Assert.assertTrue(tracer instanceof TransactionActivityInitiator);
        Assert.assertEquals(1, mtf.invocationCount);
    }

    @Test
    public void testNoTracerWithParent() {
        Transaction transaction = Transaction.getTransaction();
        transaction.getTransactionActivity().tracerStarted(
                new OtherRootTracer(transaction, signature, this, new SimpleMetricNameFormat("test")));
        NoTracerMethodTracerFactory mtf = new NoTracerMethodTracerFactory();
        CustomTracerFactory factory = new CustomTracerFactory(mtf);
        Tracer tracer = factory.getTracer(transaction, signature, this, new Object[0]);
        Assert.assertFalse(tracer instanceof TransactionActivityInitiator);
        Assert.assertEquals(1, mtf.invocationCount);
    }

    @Test
    public void testTracerNoParent() {
        Transaction transaction = Transaction.getTransaction();
        TracingMethodTracerFactory mtf = new TracingMethodTracerFactory();
        CustomTracerFactory factory = new CustomTracerFactory(mtf);
        Tracer tracer = factory.getTracer(transaction, signature, this, new Object[0]);
        Assert.assertTrue(tracer instanceof TransactionActivityInitiator);
        Assert.assertNotNull(mtf.tracer);

        tracer.finish(0, this);
        Assert.assertEquals(this, mtf.tracer.returnValue);
    }

    @Test
    public void testTracerWithParent() {
        Transaction transaction = Transaction.getTransaction();
        transaction.getTransactionActivity().tracerStarted(
                new OtherRootTracer(transaction, signature, this, new SimpleMetricNameFormat("test")));
        TracingMethodTracerFactory mtf = new TracingMethodTracerFactory();
        CustomTracerFactory factory = new CustomTracerFactory(mtf);
        Tracer tracer = factory.getTracer(transaction, signature, this, new Object[0]);
        Assert.assertFalse(tracer instanceof TransactionActivityInitiator);
        Assert.assertNotNull(mtf.tracer);

        Exception ex = new Exception();
        tracer.finish(ex);
        Assert.assertEquals(ex, mtf.tracer.exception);
    }

    private static class NoTracerMethodTracerFactory implements MethodTracerFactory {
        public int invocationCount = 0;

        @Override
        public MethodTracer methodInvoked(String methodName, Object invocationTarget, Object[] arguments) {
            invocationCount++;
            return null;
        }
    }

    private static class TracingMethodTracerFactory implements MethodTracerFactory {
        public DummyMethodTracer tracer;

        @Override
        public MethodTracer methodInvoked(String methodName, Object invocationTarget, Object[] arguments) {
            return (this.tracer = new DummyMethodTracer());
        }
    }

    private static class DummyMethodTracer implements MethodTracer {

        public Object returnValue;
        public Throwable exception;

        @Override
        public void methodFinished(Object returnValue) {
            this.returnValue = returnValue;
        }

        @Override
        public void methodFinishedWithException(Throwable exception) {
            this.exception = exception;

        }

    }
}
