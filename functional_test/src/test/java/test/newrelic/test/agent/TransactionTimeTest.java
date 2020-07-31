/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Trace;
import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

public class TransactionTimeTest implements TransactionListener {

    private volatile TransactionStats stats;
    private volatile TransactionData data;

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        stats = transactionStats;
        data = transactionData;

    }

    @Before
    public void beforeTest() throws Exception {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        Transaction.clearTransaction();
    }

    @After
    public void afterTest() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);

    }

    @Test
    public void testTransactionTimesOtherTransaction() throws InterruptedException {
        doWork();

        // verify count on stats
        Assert.assertNotNull(stats);
        ResponseTimeStats stat = stats.getScopedStats().getOrCreateResponseTimeStats(
                "Custom/test.newrelic.test.agent.TransactionTimeTest/hello1");
        Assert.assertNotNull(stat);
        Assert.assertEquals(1, stat.getCallCount());

        stat = stats.getScopedStats().getOrCreateResponseTimeStats("Custom/test.newrelic.test.agent.TransactionTimeTest/hello2");
        Assert.assertNotNull(stat);
        Assert.assertEquals(1, stat.getCallCount());

        // verify total time
        double expected = data.getRootTracer().getDuration() / 1000000000.0;
        Assert.assertEquals(expected, stats.getUnscopedStats().getOrCreateResponseTimeStats(
                "OtherTransaction/Custom/test.newrelic.test.agent.TransactionTimeTest/doWork").getTotal(), .001);

        Assert.assertEquals(expected, stats.getUnscopedStats().getOrCreateResponseTimeStats(
                "OtherTransactionTotalTime/Custom/test.newrelic.test.agent.TransactionTimeTest/doWork").getTotal(),
                .001);

    }

    @Trace(dispatcher = true)
    public void doWork() throws InterruptedException {
        hello1();
    }

    @Trace
    public void hello1() throws InterruptedException {
        hello2();
    }

    @Trace
    public void hello2() throws InterruptedException {
        Thread.sleep(4);
    }

    @Test
    public void testTransactionTimesWebTransaction() throws InterruptedException {
        doWebWork();

        // verify count on stats
        Assert.assertNotNull(stats);
        ResponseTimeStats stat = stats.getScopedStats().getOrCreateResponseTimeStats(
                "Custom/test.newrelic.test.agent.TransactionTimeTest/hello1");
        Assert.assertNotNull(stat);
        Assert.assertEquals(1, stat.getCallCount());

        stat = stats.getScopedStats().getOrCreateResponseTimeStats("Custom/test.newrelic.test.agent.TransactionTimeTest/hello2");
        Assert.assertNotNull(stat);
        Assert.assertEquals(1, stat.getCallCount());

        // verify total time
        double expected = data.getRootTracer().getDuration() / 1000000000.0;
        Assert.assertEquals(expected,
                stats.getUnscopedStats().getOrCreateResponseTimeStats("WebTransaction/Uri/mytest").getTotal(), .0001);

        Assert.assertEquals(expected, stats.getUnscopedStats().getOrCreateResponseTimeStats(
                "WebTransactionTotalTime/Uri/mytest").getTotal(), .0001);

    }

    @Trace(dispatcher = true)
    public void doWebWork() throws InterruptedException {
        Request request = new RequestWrapper(new MockHttpServletRequest("/", "mytest", "", "&test=dude"));
        Response response = new ResponseWrapper(new MockHttpServletResponse());
        NewRelic.setRequestAndResponse(request, response);
        hello1();
    }

    private class ResponseWrapper implements Response {
        private final HttpServletResponse response;

        public ResponseWrapper(HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public int getStatus() throws Exception {
            return 0;
        }

        @Override
        public String getStatusMessage() throws Exception {
            return null;
        }

        @Override
        public void setHeader(String name, String value) {
            response.setHeader(name, value);
        }

        @Override
        public String getContentType() {
            return response.getContentType();
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }

    private class RequestWrapper implements Request {
        private final HttpServletRequest request;

        public RequestWrapper(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getRequestURI() {
            return request.getRequestURI();
        }

        @Override
        public String getHeader(String name) {
            // our mock doesn't implement this method
            // return request.getHeader(name);
            return null;
        }

        @Override
        public String getRemoteUser() {
            return request.getRemoteUser();
        }

        @Override
        public Enumeration getParameterNames() {
            return request.getParameterNames();
        }

        @Override
        public String[] getParameterValues(String name) {
            return request.getParameterValues(name);
        }

        @Override
        public Object getAttribute(String name) {
            return request.getAttribute(name);
        }

        @Override
        public String getCookieValue(String name) {
            for (Cookie c : request.getCookies()) {
                if (name.equals(c.getName())) {
                    return c.getValue();
                }
            }
            return null;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }

}
