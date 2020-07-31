/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.Trace;

public class ServletContextListenerTest {

    @Test
    public void contextInitialized() {
        MyListener myListener = new MyListener();
        myListener.contextInitialized(null);

        Tracer tracer = myListener.transaction.getRootTracer();

        Assert.assertEquals(
                "Java/test.newrelic.test.agent.servlet.ServletContextListenerTest$MyListener/contextInitialized",
                tracer.getMetricName());
        Assert.assertEquals(
                "OtherTransaction/Initializer/ServletContextListener/test.newrelic.test.agent.servlet.ServletContextListenerTest$MyListener/contextInitialized",
                myListener.transaction.getPriorityTransactionName().getName());
    }

    @Test
    public void contextDestroyed() {
        MyListener myListener = new MyListener();
        myListener.contextDestroyed(null);

        Tracer tracer = myListener.transaction.getRootTracer();

        Assert.assertEquals(
                "Java/test.newrelic.test.agent.servlet.ServletContextListenerTest$MyListener/contextDestroyed",
                tracer.getMetricName());
        Assert.assertEquals(
                "OtherTransaction/Initializer/ServletContextListener/test.newrelic.test.agent.servlet.ServletContextListenerTest$MyListener/contextDestroyed",
                myListener.transaction.getPriorityTransactionName().getName());
    }

    private static class MyListener implements ServletContextListener {

        private Transaction transaction;

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            this.transaction = Transaction.getTransaction();
            dude();
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            this.transaction = Transaction.getTransaction();
            dude();
        }

        @Trace
        private void dude() {

        }

    }
}
