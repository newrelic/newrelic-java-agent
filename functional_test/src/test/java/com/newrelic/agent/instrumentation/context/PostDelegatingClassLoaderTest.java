/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.fake.third.party.resources.classloader.MockHttpServletRequest;
import com.fake.third.party.resources.classloader.MockHttpServletResponse;
import com.fake.third.party.resources.classloader.PostDelegatingClassLoader;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.Callable;

public class PostDelegatingClassLoaderTest {

    private static final boolean agentEnabled = true;
    private static final String theName = "Zoidberg";// why not?

    private static FilterConfig getFilterConfig() {
        FilterConfig config = new FilterConfig() {

            @Override
            public String getFilterName() {
                return theName;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public String getInitParameter(String name) {
                return null;
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return null;
            }

        };
        return config;
    }

    @Test
    @Trace(dispatcher = true)
    // Load NRRequest with the system classloader.
    public void testSystemClassloader() {
        Transaction transaction = getTransaction();
        try {
            Filter mockFilter = new UserFilter();
            mockFilter.init(getFilterConfig());
            final Servlet servlet = new HttpServlet() {
                @Override
                protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
                        IOException {
                }
            };
            HttpServletRequest request = new MockHttpServletRequest();
            HttpServletResponse response = new MockHttpServletResponse();
            mockFilter.doFilter(request, response, new FilterChain() {
                @Override
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException,
                        ServletException {
                    servlet.service(request, response);
                }
            });
            String txPartialName = "";
            try {
                txPartialName = transaction.getPriorityTransactionName().getPartialName();
            } catch (NullPointerException npe) {
            }
            Assert.assertEquals("/Filter/Zoidberg", txPartialName);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    @Trace(dispatcher = true)
    public void testPDClassloader() throws Exception {
        // Now we create our post-delegating classloader
        // Can no longer cast AppClassLoader to URLClassLoader in Java 9+, instead iterate through classpath to get URLs
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);
        URL[] urls = new URL[classpathEntries.length];

        try {
            for (int i = 0; i < classpathEntries.length; i++) {
                urls[i] = new File(classpathEntries[i]).toURI().toURL();
            }
        } catch (MalformedURLException ex) {
        }

        // pdLoad HttpServletRequest
        ClassLoader pdLoader = new PostDelegatingClassLoader(urls);
        Callable<Void> r = (Callable<Void>) pdLoader.loadClass(RunOnAnotherLoader.class.getName()).newInstance();

        Assert.assertNotSame(HttpServletRequest.class, r.getClass().getClassLoader().loadClass(
                HttpServletRequest.class.getName()));

        Class<?> helper = ClassLoader.getSystemClassLoader().loadClass(
                "com.nr.instrumentation.servlet24.ServletHelper");

        r.call();
        Assert.assertNotSame(helper, r.getClass().getClassLoader().loadClass(
                "com.nr.instrumentation.servlet24.ServletHelper"));
    }

    private static Transaction getTransaction() {
        if (agentEnabled) {
            return Transaction.getTransaction();
        }
        Transaction tx = Mockito.mock(Transaction.class);

        PriorityTransactionName name = Mockito.mock(PriorityTransactionName.class);
        Mockito.when(tx.getPriorityTransactionName()).thenReturn(name);
        Mockito.when(name.getPartialName()).thenReturn("/Filter/Zoidberg");

        return tx;
    }

    public static class RunOnAnotherLoader implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            Transaction transaction = getTransaction();
            final Servlet servlet = new HttpServlet() {
                @Override
                protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                }
            };

            Filter pdMockFilter = new UserFilter();
            pdMockFilter.init(getFilterConfig());
            pdMockFilter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new FilterChain() {
                @Override
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                    servlet.service(request, response);
                }
            });
            String txPartialName = "";
            try {
                txPartialName = transaction.getPriorityTransactionName().getPartialName();
            } catch (NullPointerException npe) {
            }
            Assert.assertEquals("/Filter/Zoidberg", txPartialName);
            return null;
        }

    }

    public static class UserFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            // Instrumentation happens here
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
        }

        @Override
        public void destroy() {
        }

    }

}
