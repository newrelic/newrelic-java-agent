/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;

import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockServletContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.FakeRequest;
import com.newrelic.agent.FakeResponse;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Trace;

public class ServletFilterTest {

    @Before
    public void setup() {
        Transaction.clearTransaction();
        ServiceFactory.getStatsService().getStatsEngineForHarvest(null).clear();
    }

    @Trace(dispatcher = true)
    @Test
    public void testMethodSplitBetweenClasses() throws ServletException {
        MockServletContext context = new MockServletContext() {

            @Override
            public String getServerInfo() {
                return "Awesome Dude/3.13";
            }

        };
        FilterConfigImpl config = new FilterConfigImpl(context);
        Filter filter = new ChildFilter();
        filter.init(config);

        try {
            filter.doFilter(new FakeRequest("/", "dude", "/", "", "", ""), new FakeResponse(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("WebTransaction/Filter/DUMMY",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

    @Test
    public void testInit() throws ServletException {
        MockServletContext context = new MockServletContext() {

            @Override
            public String getServerInfo() {
                return "Awesome Dude/3.13";
            }

        };
        FilterConfigImpl config = new FilterConfigImpl(context);
        Filter filter = new MyFilter();
        filter.init(config);

        Environment env = ServiceFactory.getEnvironmentService().getEnvironment();
        Assert.assertEquals("Awesome Dude", env.getAgentIdentity().getDispatcher());
        Assert.assertEquals("3.13", env.getAgentIdentity().getDispatcherVersion());
    }

    @Test
    public void testDoFilter() throws Exception {

        MockServletContext context = new MockServletContext() {

            @Override
            public String getServerInfo() {
                return "Awesome Dude/3.13";
            }

        };
        MyFilter filter = new MyFilter();
        filter.init(new FilterConfigImpl(context));

        MockHttpServletRequest request = new MockHttpServletRequest() {

            @Override
            public Cookie[] getCookies() {
                return new Cookie[] { new Cookie("dude", "true") };
            }

        };
        filter.doFilter(request, new MockHttpServletResponse(), null);

        Assert.assertNotNull(filter.dispatcher);
        Assert.assertNotNull(filter.response);
    }

    private static class FilterConfigImpl implements FilterConfig {

        private final ServletContext servletContext;

        public FilterConfigImpl(ServletContext servletContext) {
            super();
            this.servletContext = servletContext;
        }

        public String getFilterName() {
            return "DUMMY";
        }

        public String getInitParameter(String arg0) {
            return null;
        }

        public Enumeration getInitParameterNames() {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        public ServletContext getServletContext() {
            return servletContext;
        }

    }

    private abstract static class BaseFilter implements Filter {
        public void destroy() {
        }

        public void init(FilterConfig arg0) throws ServletException {

        }
    }

    private static class ChildFilter extends BaseFilter implements Filter {
        public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException,
                ServletException {

        }
    }

    private static class MyFilter implements Filter {

        private volatile Dispatcher dispatcher;
        private volatile Response response;

        public void destroy() {
        }

        public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException,
                ServletException {
            this.dispatcher = Transaction.getTransaction().getDispatcher();
            response = dispatcher.getResponse();

            Assert.assertEquals("true", dispatcher.getCookieValue("dude"));
            Assert.assertNull(dispatcher.getCookieValue("test"));
        }

        public void init(FilterConfig arg0) throws ServletException {
        }

    }
}
