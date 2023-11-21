/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.google.common.collect.Sets;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpCommonsTest {

    static final TransactionDataList transactions = new TransactionDataList();

    @ClassRule
    public static HttpServerRule SERVER = new HttpServerRule();

    private static URI ENDPOINT;
    private static String HOST;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServiceFactory.getTransactionService().addTransactionListener(transactions);
        ENDPOINT = SERVER.getEndPoint();
        HOST = ENDPOINT.getHost();
    }

    @Before
    public void setup() {
        transactions.clear();
    }

    @Test
    public void httpURLConnection() throws Exception {
        InstrumentationProxy instrProxy = ServiceFactory.getCoreService().getInstrumentation();
        if (!instrProxy.isBootstrapClassInstrumentationEnabled()) {
            return;
        }
        httpURLConnectionTx();

        Set<String> metrics = AgentHelper.getMetrics();
        assertTrue(metrics.toString(), metrics.contains("External/" + HOST + "/HttpURLConnection/getResponseCode"));

        Map<String, Integer> metricCounts = getMetricCounts(
                MetricName.create("External/" + HOST + "/HttpURLConnection/getResponseCode",
                        "OtherTransaction/Custom/test.newrelic.test.agent.HttpCommonsTest/httpURLConnectionTx"));

        assertEquals(1, (int) metricCounts.get("External/" + HOST + "/HttpURLConnection/getResponseCode"));
    }

    @Trace(dispatcher = true)
    private void httpURLConnectionTx() throws Exception {
        HttpURLConnection httpConn = (HttpURLConnection) ENDPOINT.toURL().openConnection();

        httpConn.setRequestMethod("PUT");
        httpConn.setRequestProperty(HttpTestServer.DO_CAT, "false");

        httpConn.getResponseCode();
    }

    @Test
    public void httpMethod() throws IOException {
        httpMethodImpl();

        Set<String> metrics = AgentHelper.getMetrics();
        assertTrue(metrics.toString(), metrics.contains("External/localhost/CommonsHttp"));
        assertTrue(metrics.toString(), metrics.contains("External/localhost/CommonsHttp/execute"));
        assertTrue(metrics.toString(), metrics.contains("External/localhost/all"));
        assertTrue(metrics.toString(), metrics.contains("External/all"));
        assertTrue(metrics.toString(), metrics.contains("External/allOther"));

        Map<String, Integer> metricCounts = getMetricCounts(
                MetricName.create("External/localhost/CommonsHttp",
                        "OtherTransaction/Custom/test.newrelic.test.agent.HttpCommonsTest/httpMethodImpl"),
                MetricName.create("External/localhost/CommonsHttp/execute",
                        "OtherTransaction/Custom/test.newrelic.test.agent.HttpCommonsTest/httpMethodImpl"),
                MetricName.create("External/localhost/all"),
                MetricName.create("External/all"),
                MetricName.create("External/allOther"));

        assertEquals(3, (int) metricCounts.get("External/localhost/all"));
        assertEquals(3, (int) metricCounts.get("External/all"));
        assertEquals(3, (int) metricCounts.get("External/allOther"));

        // This is 3 because the loop executes 3 times and each loop calls
        // execute() and releaseConnection(), both of which are instrumented
        assertEquals(3, (int) metricCounts.get("External/localhost/CommonsHttp")); // releaseConnection
        assertEquals(3, (int) metricCounts.get("External/localhost/CommonsHttp/execute"));
    }

    @Trace(dispatcher = true)
    private void httpMethodImpl() throws IOException {
        for (int i = 0; i < 3; i++) {
            HttpState state = new HttpState();
            HttpConnection connection = new HttpConnection(ENDPOINT.getHost(), ENDPOINT.getPort());
            connection.open();
            HttpMethod method = new GetMethod();
            method.setQueryString(String.format("%s=1", HttpTestServer.NO_TRANSACTION));
            method.execute(state, connection);
            method.releaseConnection();
        }
    }

    private Map<String, Integer> getMetricCounts(MetricName... responseTimeMetricNames) {
        Set<MetricName> metricNames = Sets.newHashSet(responseTimeMetricNames);
        return transactions.getMetricCounts(metricNames);
    }
}
