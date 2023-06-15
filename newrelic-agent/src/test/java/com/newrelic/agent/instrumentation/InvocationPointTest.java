package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.InvocationHandler;

public class InvocationPointTest {

    @Test
    public void test_invoke_withTracerFactory_ignoreApdexFalse() throws Throwable {
        TracerFactory factory = Mockito.mock(TracerFactory.class);
        ClassMethodSignature methodSignature = new ClassMethodSignature("class", "method", "desc");
        InvocationHandler resultHandler = InvocationPoint.getInvocationPoint(factory, null, methodSignature, false);

        Assert.assertNull(resultHandler.invoke(null, null, new Object[]{1, new Object[]{}}));
    }

    @Test
    public void test_invoke_withTracerFactory_ignoreApdexTrue() throws Throwable {
        Dispatcher dispatcher = Mockito.mock(Dispatcher.class);
        Transaction tx = Mockito.mock(Transaction.class);
        Mockito.when(tx.getDispatcher()).thenReturn(dispatcher);
        Transaction.setTransaction(tx);

        TracerFactory factory = Mockito.mock(TracerFactory.class);
        ClassMethodSignature methodSignature = new ClassMethodSignature("class", "method", "desc");
        InvocationHandler resultHandler = InvocationPoint.getInvocationPoint(factory, null, methodSignature, true);

        Assert.assertNull(resultHandler.invoke(null, null, new Object[]{1, new Object[]{}}));
        Mockito.verify(dispatcher).setIgnoreApdex(true);
    }

    @Test
    public void test_invoke_withEntryInvocationHandler() throws Throwable {
        EntryInvocationHandler handler = Mockito.mock(EntryInvocationHandler.class);
        ClassMethodSignature methodSignature = new ClassMethodSignature("class", "method", "desc");
        InvocationHandler resultHandler = InvocationPoint.getInvocationPoint(handler, null, methodSignature, false);

        Assert.assertNull(resultHandler.invoke(null, null, new Object[]{1, new Object[]{}}));
    }

    @Test
    public void test_getInvocationPoint_withEntryInvocationHandler() {
        EntryInvocationHandler handler = Mockito.mock(EntryInvocationHandler.class);
        ClassMethodSignature methodSignature = new ClassMethodSignature("class", "method", "desc");
        InvocationHandler result = InvocationPoint.getInvocationPoint(handler, null, methodSignature, false);

        Assert.assertTrue(result instanceof InvocationHandler);
        Assert.assertFalse(result instanceof InvocationPoint);
    }

    @Test
    public void test_getInvocationPoint_withTracerFactory() {
        TracerFactory factory = Mockito.mock(TracerFactory.class);
        ClassMethodSignature methodSignature = new ClassMethodSignature("class", "method", "desc");
        InvocationHandler result = InvocationPoint.getInvocationPoint(factory, null, methodSignature, false);

        Assert.assertTrue(result instanceof InvocationPoint);
        Assert.assertEquals(factory, ((InvocationPoint) result).getTracerFactory());
        Assert.assertEquals(methodSignature, ((InvocationPoint) result).getClassMethodSignature());
    }

    @Test
    public void test_getInvocationPoint_badHandler_ignoreApdexFalse() {
        Assert.assertEquals(NoOpInvocationHandler.INVOCATION_HANDLER, InvocationPoint.getInvocationPoint(null, null, null, false));
    }

    @Test
    public void test_getInvocationPoint_badHandler_ignoreApdexTrue() {
        Assert.assertEquals(IgnoreApdexInvocationHandler.INVOCATION_HANDLER, InvocationPoint.getInvocationPoint(null, null, null, true));
    }
}
