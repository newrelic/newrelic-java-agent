/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.servlet;

import com.newrelic.agent.MockTransactionActivity;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.TransactionStateImpl;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

public class ServletUserTest {

    private static final Principal principal = new Principal() {

        @Override
        public String getName() {
            return "Saxon";
        }
    };

    @Test
    public void getUser() throws Exception {
        Transaction transaction = Mockito.mock(Transaction.class);

        // Set the transaction on the private static ThreadLocal in the Transaction class.
        Field f = Transaction.class.getDeclaredField("transactionHolder");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Transaction> threadLocal = (ThreadLocal<Transaction>) f.get(null);
        threadLocal.set(transaction);

        Map<String, Object> agentAttributes = new HashMap<>();
        Mockito.when(transaction.getAgentAttributes()).thenReturn(agentAttributes);
        TransactionActivity txa = new MockTransactionActivity();
        TransactionState txs = new TransactionStateImpl();
        Mockito.when(transaction.getTransactionActivity()).thenReturn(txa);
        Mockito.when(transaction.getTransactionState()).thenReturn(txs);

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        @SuppressWarnings("serial")
        final Servlet servlet = new HttpServlet() {

            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) {
                // NO OP
            }

        };
        new UserFilter().doFilter(request, response, new FilterChain() {

            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                servlet.service(request, response);
            }
        });

        Assert.assertEquals("Saxon", agentAttributes.get("user"));

    }

    private static class UserFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                ServletException {
            chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {

                @Override
                public Principal getUserPrincipal() {
                    return principal;
                }

            }, response);
        }

        @Override
        public void destroy() {
        }

    }
}
