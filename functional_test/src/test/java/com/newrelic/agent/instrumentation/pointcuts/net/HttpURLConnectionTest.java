/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.net;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpURLConnectionTest {

    static final TransactionDataList transactions = new TransactionDataList();

    @BeforeClass
    public static void beforeClass() {
        ServiceFactory.getTransactionService().addTransactionListener(transactions);
    }

    @Before
    public void setup() {
        transactions.clear();
    }

    /**
     * Test multiple calls to connect()
     */
    @Test
    public void testConnect() {
        doTestConnect();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/doTestConnect", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 0);
    }

    @Trace(dispatcher = true)
    public void doTestConnect() {
        HttpURLConnection connection = null;
        try {
            connection = getHttpsExampleComConnection();
            connection.connect();
            connection.connect();// should be no-op
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
    }

    /**
     * Test calling getOutputStream() before connect()
     */
    @Test
    public void outputStreamFirstTest() {
        doOutputStreamFirstTest();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/doOutputStreamFirstTest", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 0);
    }

    @Trace(dispatcher = true)
    private void doOutputStreamFirstTest() {
        HttpURLConnection connection = null;
        OutputStream os = null;
        try {
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            connection.getOutputStream();
            connection.connect();// should be no-op
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Test calling connect() before getOutputStream().
     */
    @Test
    public void connectFirstTest() {
        doConnectFirstTest();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/doConnectFirstTest", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 0);
    }

    @Trace(dispatcher = true)
    private void doConnectFirstTest() {
        HttpURLConnection connection = null;
        OutputStream os = null;
        try {
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            connection.connect();
            connection.getOutputStream();
            connection.connect();// should be no-op
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Test calling getInputStream() before connect()
     */
    @Test
    public void inputStreamFirstTest() {
        doInputStreamFirstTest();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/doInputStreamFirstTest", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void doInputStreamFirstTest() {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            connection = getHttpsExampleComConnection();
            try {
                connection.setDoInput(true);
                System.out.println("Not yet Connected");
            } catch (Exception e) {
                System.out.println("Connected");
            }

            is = connection.getInputStream();
            connection.connect();// should be no-op
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics1() {
        run1();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run1", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 0);
    }

    @Trace(dispatcher = true)
    private void run1() {
        HttpURLConnection connection = null;
        try {
            // Test 1: connect()
            connection = getHttpsExampleComConnection();
            connection.connect(); // No network I/O
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics2() {
        run2();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run2", testClass);

        verifyMetrics("example.com", scope, 2, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void run2() {
        HttpURLConnection connection = null;
        try {
            // Test 2: connect() + getInputStream()
            connection = getHttpsExampleComConnection();
            connection.connect(); // No network I/O
            read(connection.getInputStream()); // GETs URL and reads response (Network I/O)
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics3() {
        run3();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run3", testClass);

        verifyMetrics("example.com", scope, 2, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void run3() {
        HttpURLConnection connection = null;
        try {
            // Test 3: connect() + getResponseCode()
            connection = getHttpsExampleComConnection();
            connection.connect(); // No network I/O
            // GETs URL and reads response code (Network I/O)
            System.out.println("Response code: " + connection.getResponseCode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics4() {
        run4();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run4", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void run4() {
        HttpURLConnection connection = null;
        try {
            // Test 4: getInputStream()
            connection = getHttpsExampleComConnection();
            read(connection.getInputStream()); // GETs URL and reads response (Network I/O)
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics5() {
        run5();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run5", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void run5() {
        HttpURLConnection connection = null;
        try {
            // Test 5: getInputStream() + getResponseCode()
            connection = getHttpsExampleComConnection();
            read(connection.getInputStream()); // GETs URL and reads response (Network I/O)
            System.out.println("Response code: " + connection.getResponseCode()); // No network I/O
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics6() {
        run6();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run6", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 0);
    }

    @Trace(dispatcher = true)
    private void run6() {
        HttpURLConnection connection = null;
        try {
            // Test 6: getOutputStream()
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            write(connection.getOutputStream(), "post_data"); // No network I/O, No POST
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics7() {
        run7();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run7", testClass);

        verifyMetrics("example.com", scope, 2, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void run7() {
        HttpURLConnection connection = null;
        try {
            // Test 7: getOutputStream() + getResponseCode()
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            write(connection.getOutputStream(), "post_data"); // No network I/O, No POST

            // POSTs URL and reads response code (Network I/O)
            System.out.println("Response code: " + connection.getResponseCode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics8() {
        run8();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run8", testClass);

        verifyMetrics("example.com", scope, 2, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void run8() {
        HttpURLConnection connection = null;
        try {
            // Test 8: getOutputStream() + getResponseCode() + getInputStream()
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            write(connection.getOutputStream(), "post_data"); // No network I/O, No POST
            System.out.println("Response code: " + connection.getResponseCode()); // POSTs URL and reads response code (Network I/O)
            read(connection.getInputStream()); // Reads response body (No Network I/O)
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics9() {
        run9();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/run9", testClass);

        verifyMetrics("example.com", scope, 1, 0, 0, 1);
    }

    @Trace(dispatcher = true)
    private void run9() {
        HttpURLConnection connection = null;
        try {
            // Test 9: getResponseCode()
            connection = getHttpsExampleComConnection();
            // GETs URL and reads response code (Network I/O)
            System.out.println("Response code: " + connection.getResponseCode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testUnresolvedHost() throws Exception {
        unresolvedHost();

        String testClass = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
        String scope = format("OtherTransaction/Custom/{0}/unresolvedHost", testClass);

        verifyMetrics("UnknownHost", scope, 1, 0, 0, 0);
    }

    @Trace(dispatcher = true)
    public void unresolvedHost() throws Exception {
        URL url = new URL("http://www.thishostdoesnotexistbro.com"); // Better hope no one buys this domain...
        URLConnection con = url.openConnection();

        try {
            con.getInputStream();
        } catch (IOException ex) {
        }
    }

    private void read(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        System.out.println(out.toString());
        reader.close();
    }

    private void write(OutputStream outputStream, String text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        writer.write(text);
        writer.flush();
        writer.close();
    }

    private HttpURLConnection getHttpsExampleComConnection() throws Exception {
        return (HttpURLConnection) new URL("https://example.com").openConnection();
    }

    private void verifyMetrics(String url, String scope, int scopedHttpUrlCount, int unscopedHttpUrlCount,
            int scopedNetworkIOCount, int unscopedNetworkIOCount) {
        Set<String> metrics = AgentHelper.getMetrics();

        if (scopedHttpUrlCount > 0 || unscopedHttpUrlCount > 0) {
            Assert.assertTrue(metrics.toString(), metrics.contains(format("External/{0}/HttpURLConnection", url)));
        } else {
            Assert.assertFalse(metrics.toString(), metrics.contains(format("External/{0}/HttpURLConnection", url)));
        }

        if (scopedNetworkIOCount > 0 || unscopedNetworkIOCount > 0) {
            Assert.assertTrue(metrics.toString(), metrics.contains(format("External/{0}/all", url)));
            Assert.assertTrue(metrics.toString(), metrics.contains("External/all"));
            Assert.assertTrue(metrics.toString(), metrics.contains("External/allOther"));
        } else {
            Assert.assertFalse(metrics.toString(), metrics.contains(format("External/{0}/all", url)));
            Assert.assertFalse(metrics.toString(), metrics.contains("External/all"));
            Assert.assertFalse(metrics.toString(), metrics.contains("External/allOther"));
        }

        Map<String, Integer> scopedMetrics = getMetricCounts(
                MetricName.create(format("External/{0}/HttpURLConnection", url), scope),
                MetricName.create(format("External/{0}/all", url), scope),
                MetricName.create("External/all", scope),
                MetricName.create("External/allOther", scope));

        Map<String, Integer> unscopedMetrics = getMetricCounts(
                MetricName.create(format("External/{0}/HttpURLConnection", url)),
                MetricName.create(format("External/{0}/all", url)),
                MetricName.create("External/all"),
                MetricName.create("External/allOther"));

        assertEquals(scopedHttpUrlCount, (int) scopedMetrics.get(format("External/{0}/HttpURLConnection", url)));
        assertEquals(scopedNetworkIOCount, (int) scopedMetrics.get(format("External/{0}/all", url)));
        assertEquals(scopedNetworkIOCount, (int) scopedMetrics.get("External/all"));
        assertEquals(scopedNetworkIOCount, (int) scopedMetrics.get("External/allOther"));

        assertEquals(unscopedHttpUrlCount, (int) unscopedMetrics.get(format("External/{0}/HttpURLConnection", url)));
        assertEquals(unscopedNetworkIOCount, (int) unscopedMetrics.get(format("External/{0}/all", url)));
        assertEquals(unscopedNetworkIOCount, (int) unscopedMetrics.get("External/all"));
        assertEquals(unscopedNetworkIOCount, (int) unscopedMetrics.get("External/allOther"));
    }

    private Map<String, Integer> getMetricCounts(MetricName... responseTimeMetricNames) {
        Set<MetricName> metricNames = Sets.newHashSet(responseTimeMetricNames);
        return transactions.getMetricCounts(metricNames);
    }
}
