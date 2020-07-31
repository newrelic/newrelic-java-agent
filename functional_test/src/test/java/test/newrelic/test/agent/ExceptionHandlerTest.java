/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.transaction.TransactionThrowable;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class ExceptionHandlerTest {
    @Test
    public void noError() throws ServletException, IOException {
        final AtomicReference<TransactionThrowable> throwable = new AtomicReference<>();
        HttpServlet servlet = new HttpServlet() {


            private static final long serialVersionUID = 1L;

            @Override
            public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                throwable.set(Transaction.getTransaction().getThrowable());
            }

        };
        AgentHelper.invokeServlet(servlet, "", "Test", "/clean");
        Assert.assertNull(throwable.get());
    }

    @Test
    public void exceptionHandlerSig1() throws ServletException, IOException {
        final AtomicReference<TransactionThrowable> throwable = new AtomicReference<>();
        HttpServlet servlet = new HttpServlet() {

            private static final long serialVersionUID = 1L;

            @Override
            public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                new ExceptionHandler().handleException(new Exception("dude"));
                throwable.set(Transaction.getTransaction().getThrowable());
            }

        };
        AgentHelper.invokeServlet(servlet, "", "Test", "/error");
        Assert.assertNotNull(throwable.get());
    }

    @Test
    public void exceptionHandlerSig2() throws ServletException, IOException {
        final AtomicReference<TransactionThrowable> throwable = new AtomicReference<>();
        HttpServlet servlet = new HttpServlet() {

            private static final long serialVersionUID = 1L;

            @Override
            public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                new ExceptionHandler().handleException("Test", new Exception("whoah!"));
                throwable.set(Transaction.getTransaction().getThrowable());
            }

        };
        AgentHelper.invokeServlet(servlet, "", "Test", "/man");
        Assert.assertNotNull(throwable.get());
    }

    @Test
    public void exceptionHandlerSig3() throws ServletException, IOException {
        final AtomicReference<TransactionThrowable> throwable = new AtomicReference<>();
        HttpServlet servlet = new HttpServlet() {

            private static final long serialVersionUID = 1L;

            @Override
            public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                new ExceptionHandler().handleException2("Test", new Exception("whoah!"));
                throwable.set(Transaction.getTransaction().getThrowable());
            }

        };
        AgentHelper.invokeServlet(servlet, "", "Test", "/man");
        Assert.assertNotNull(throwable.get());
    }

    @Test
    public void specialExceptionHandler() throws ServletException, IOException {
        final AtomicReference<TransactionThrowable> throwable = new AtomicReference<>();
        HttpServlet servlet = new HttpServlet() {

            private static final long serialVersionUID = 1L;

            @Override
            public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                new ExceptionHandler().handleSpecialException(new RuntimeException("whoah!"), "special");
                throwable.set(Transaction.getTransaction().getThrowable());
            }

        };
        AgentHelper.invokeServlet(servlet, "", "Test", "/man");
        Assert.assertNotNull(throwable.get());
    }

    private class ExceptionHandler {
        public void handleException(Exception ex) {

        }

        public void handleException(String message, Throwable ex) {
        }

        public void handleException2(String message, Throwable ex) {
        }

        public void handleSpecialException(RuntimeException ex, String message) {
        }
    }
}
