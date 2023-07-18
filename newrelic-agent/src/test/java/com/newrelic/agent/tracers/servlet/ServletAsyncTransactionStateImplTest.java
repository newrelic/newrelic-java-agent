package com.newrelic.agent.tracers.servlet;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class ServletAsyncTransactionStateImplTest {

    @Before
    public void setup() {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Test
    public void test_variousStateChanges() throws NoSuchFieldException, IllegalAccessException {
        Transaction tx = Transaction.getTransaction(true);
        ServletAsyncTransactionStateImpl target = new ServletAsyncTransactionStateImpl(tx);
        TracerFactory factory = new AsyncTracerFactory();
        Tracer result = target.getTracer(tx, factory, null, null);
        Assert.assertNotNull(result);
        // make sure we've got one running
        assertState(target, "RUNNING");

        // do some things that shouldn't actually happen given the current state
        target.resume();
        assertState(target, "RUNNING"); // would be RESUMING if the call worked
        Assert.assertNull(target.getRootTracer());  // this shouldn't do anything but return null

        // on to some things that should work
        target.suspendRootTracer();
        assertState(target, "SUSPENDING");

        target.resume();
        assertState(target, "RESUMING");

        // and another thing that shoudn't work
        target.suspendRootTracer();
        assertState(target, "RESUMING"); // would be SUSPENDING if the call had worked
        target.complete();
        assertState(target, "RESUMING"); // would be RUNNING if the call had worked

        // and some more that should work
        target.getRootTracer();
        assertState(target, "RUNNING");

        target.suspendRootTracer();
        assertState(target, "SUSPENDING");
        target.complete();
        assertState(target, "RUNNING");

        target.suspendRootTracer();
        Assert.assertFalse(target.finish(tx, result));

        target.suspendRootTracer();
        target.complete();
        Assert.assertTrue(target.finish(tx, result));
    }

    private static class AsyncTracerFactory extends AbstractTracerFactory {
        @Override
        public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
            sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
            MockHttpRequest httpRequest = new MockHttpRequest();
            MockHttpResponse httpResponse = new MockHttpResponse();
            return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
        }
    }

    private void assertState(ServletAsyncTransactionStateImpl target, String desiredState) throws NoSuchFieldException, IllegalAccessException {
        Field stateField = target.getClass().getDeclaredField("state");
        stateField.setAccessible(true);
        Assert.assertEquals(desiredState, ((AtomicReference)stateField.get(target)).get().toString());
    }
}
