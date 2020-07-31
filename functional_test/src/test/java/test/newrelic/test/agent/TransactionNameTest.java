/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.FakeRequest;
import com.newrelic.agent.FakeResponse;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.util.MockServletConfig;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;

public class TransactionNameTest {

    @Trace(dispatcher = true)
    @Test
    public void nameOverride() throws ServletException, IOException {

        Assert.assertFalse(NewRelic.getAgent().getTransaction().isTransactionNameSet());

        Assert.assertTrue(NewRelic.getAgent().getTransaction().setTransactionName(
                TransactionNamePriority.FRAMEWORK_LOW, true, "Custom", "First"));
        Assert.assertTrue(NewRelic.getAgent().getTransaction().setTransactionName(
                TransactionNamePriority.FRAMEWORK_LOW, true, "Custom", "Second"));

        Assert.assertEquals("OtherTransaction/Custom/Second",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void nameNoOverride() throws ServletException, IOException {

        Assert.assertFalse(NewRelic.getAgent().getTransaction().isTransactionNameSet());

        Assert.assertTrue(NewRelic.getAgent().getTransaction().setTransactionName(
                TransactionNamePriority.FRAMEWORK_LOW, false, "Custom", "First"));
        Assert.assertFalse(NewRelic.getAgent().getTransaction().setTransactionName(
                TransactionNamePriority.FRAMEWORK_LOW, false, "Custom", "Second"));

        Assert.assertEquals("OtherTransaction/Custom/First",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void servletName() throws ServletException, IOException {

        Assert.assertFalse(NewRelic.getAgent().getTransaction().isTransactionNameSet());
        invokeServlet();

        Assert.assertEquals("WebTransaction/Servlet/TheDude",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void servletName_AutoDisabled() throws ServletException, IOException {

        Transaction tx = Mockito.spy(Transaction.getTransaction());
        Transaction.setTransaction(tx);
        Mockito.when(tx.isTransactionNamingEnabled()).thenReturn(false);

        invokeServlet();

        Assert.assertNull(tx.getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void emptyCategoryString() throws ServletException, IOException {

        NewRelic.setTransactionName("", "Test");

        Assert.assertEquals("OtherTransaction/Custom/Test",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Trace(dispatcher = true)
    @Test
    public void apiName_AutoDisabled() throws ServletException, IOException {

        Transaction tx = Mockito.spy(Transaction.getTransaction());
        Transaction.setTransaction(tx);
        Mockito.when(tx.isTransactionNamingEnabled()).thenReturn(false);

        NewRelic.setTransactionName("Test", "Test");
        invokeServlet();

        Assert.assertEquals("WebTransaction/Test/Test",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    private void invokeServlet() throws ServletException, IOException {
        GenericServlet servlet = new GenericServlet() {

            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            }
        };
        // servlet = Mockito.spy(servlet);
        ServletConfig servletConfig = new MockServletConfig().setServletName("TheDude");
        servlet.init(servletConfig);
        // Mockito.when(servlet.getServletConfig()).thenReturn(servletConfig);

        servlet.service(new FakeRequest("/", "", "/", "", "", ""), new FakeResponse());
    }
}
