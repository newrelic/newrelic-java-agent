/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.jakarta;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RestTest {

    private final RestExample example = new RestExample();

    @Trace(dispatcher = true)
    @Test
    public void list_customOverride() {
        example.list(0, 6);
        NewRelic.setTransactionName("Test", "Override");

        Assert.assertEquals("OtherTransaction/Test/Override",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void list_AutoNameDisabled() {
        Transaction transaction = Mockito.spy(Transaction.getTransaction());
        Transaction.setTransaction(transaction);
        Mockito.when(transaction.isTransactionNamingEnabled()).thenReturn(false);

        example.list(0, 6);

        Assert.assertNull(Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void list() {
        example.list(0, 6);

        Assert.assertEquals("OtherTransaction/RestWebService/user/list (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void firstWins() {
        example.list(0, 6);
        example.create("", "", "");

        Assert.assertEquals("OtherTransaction/RestWebService/user/list (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void create() {
        example.create("", "", "");

        Assert.assertEquals("OtherTransaction/RestWebService/user/create (PUT)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void delete() {
        example.delete(666);

        Assert.assertEquals("OtherTransaction/RestWebService/user/delete/{id} (DELETE)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void find() {
        example.find(666);

        Assert.assertEquals("OtherTransaction/RestWebService/user/show/{id} (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void post() {
        example.update(6, null, null, null);

        Assert.assertEquals("OtherTransaction/RestWebService/user/update/{id} (POST)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void noPath() {
        example.dude();

        Assert.assertEquals("OtherTransaction/RestWebService/user (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/noPath",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Trace(dispatcher = true)
    @Test
    public void noPathPrefix() {
        new NoPathPrefix().get();

        Assert.assertEquals("OtherTransaction/RestWebService/resource (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());

        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/noPathPrefix",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Trace(dispatcher = true)
    @Test
    public void customOverride() {
        new CustomOverrides().get();

        // confirms that if a Trace annotation is on the method it wins
        Assert.assertEquals("OtherTransaction/Custom/Dude",
                Transaction.getTransaction().getPriorityTransactionName().getName());

        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/customOverride",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Trace(dispatcher = true)
    @Test
    public void exceptionCase() {
        example.exceptionCase();

        Assert.assertEquals("OtherTransaction/RestWebService/user/exception (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/exceptionCase",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Trace(dispatcher = true)
    @Test
    public void exceptionCaseWithVariable() {
        example.exceptionCaseWithVariable(5);

        Assert.assertEquals("OtherTransaction/RestWebService/user/exception/variable/{id} (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/exceptionCaseWithVariable",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Trace(dispatcher = true)
    @Test
    public void exceptionCaseWithVariables() {
        example.exceptionCaseWithVariables(5, 10, 20);

        Assert.assertEquals("OtherTransaction/RestWebService/user/exception/variables/{id}/{id2}/{id3} (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/exceptionCaseWithVariables",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Trace(dispatcher = true)
    @Test
    public void exceptionCaseWithVariableAndNormalTryCatch() {
        example.exceptionCaseWithVariableAndNormalTryCatch(10);

        Assert.assertEquals("OtherTransaction/RestWebService/user/exception/variable/trycatch/{id} (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/exceptionCaseWithVariableAndNormalTryCatch",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Trace(dispatcher = true)
    @Test
    public void staticMethod() {
        RestExample.nothing();

        Assert.assertEquals("OtherTransaction/RestWebService/user/static (GET)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
        Assert.assertEquals("Java/com.newrelic.agent.instrumentation.webservices.jakarta.RestTest/staticMethod",
                Transaction.getTransaction().getTransactionActivity().getLastTracer().getMetricName());
    }

    @Path("resource")
    public class NoPathPrefix {

        @GET
        public Object get() {
            return "get";
        }
    }

    public class CustomOverrides {

        @Trace(dispatcher = true, metricName = "Dude")
        @GET
        public Object get() {
            return "get";
        }
    }
}
