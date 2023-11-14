/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.net;

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

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;

public class HttpURLConnectionTest {

    static final TransactionDataList transactions = new TransactionDataList();
    private static final String GET_OUTPUT_STREAM = "getOutputStream";
    private static final String GET_INPUT_STREAM = "getInputStream";
    private static final String GET_RESPONSE_CODE = "getResponseCode";
    private static final String GET_RESPONSE_MSG = "getResponseMessage";
    private static final String URL = "example.com";
    private static final String REAL_HOST = "https://example.com";
    private static final String FAKE_HOST = "http://www.thishostdoesnotexistbro.com"; // Better hope no one buys this domain...
    private static final String UNKNOWN_HOST = "UnknownHost";
    private static final String POST_DATA = "post_data";
    private static final String TEST_CLASS = "com.newrelic.agent.instrumentation.pointcuts.net.HttpURLConnectionTest";
    // This timeout is required if connect is the only HttpURLConnection API called
    private static final int TEST_SLEEP_TIME_MILLIS = 7_000;

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
        String scope = format("OtherTransaction/Custom/{0}/doTestConnect", TEST_CLASS);

        verifyMetrics(URL, scope, false, null);
    }

    @Trace(dispatcher = true)
    public void doTestConnect() {
        HttpURLConnection connection = null;
        try {
            connection = getHttpsExampleComConnection();
            connection.connect(); // No network I/O
            connection.connect(); // should be no-op
            // Wait long enough for the TimerTask in the HttpURLConnection instrumentation to end the segment timing when only connect is called
            Thread.sleep(TEST_SLEEP_TIME_MILLIS);
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
        String scope = format("OtherTransaction/Custom/{0}/doOutputStreamFirstTest", TEST_CLASS);

        verifyMetrics(URL, scope, false, GET_OUTPUT_STREAM);
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
                } catch (IOException ignored) {
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
        String scope = format("OtherTransaction/Custom/{0}/doConnectFirstTest", TEST_CLASS);

        verifyMetrics(URL, scope, false, GET_OUTPUT_STREAM);
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
                } catch (IOException ignored) {
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
        String scope = format("OtherTransaction/Custom/{0}/doInputStreamFirstTest", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_INPUT_STREAM);
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
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Test
    public void testHttpURLConnectionMetrics1() {
        run1();
        String scope = format("OtherTransaction/Custom/{0}/run1", TEST_CLASS);

        verifyMetrics(URL, scope, false, null);
    }

    @Trace(dispatcher = true)
    private void run1() {
        HttpURLConnection connection = null;
        try {
            // Test 1: connect()
            connection = getHttpsExampleComConnection();
            connection.connect(); // No network I/O
            // Wait long enough for the TimerTask in the HttpURLConnection instrumentation to end the segment timing when only connect is called
            Thread.sleep(TEST_SLEEP_TIME_MILLIS);
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
        String scope = format("OtherTransaction/Custom/{0}/run2", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_INPUT_STREAM);
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
        String scope = format("OtherTransaction/Custom/{0}/run3", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_RESPONSE_CODE);
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
        String scope = format("OtherTransaction/Custom/{0}/run4", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_INPUT_STREAM);
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
        String scope = format("OtherTransaction/Custom/{0}/run5", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_INPUT_STREAM);
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
        String scope = format("OtherTransaction/Custom/{0}/run6", TEST_CLASS);

        verifyMetrics(URL, scope, false, GET_OUTPUT_STREAM);
    }

    @Trace(dispatcher = true)
    private void run6() {
        HttpURLConnection connection = null;
        try {
            // Test 6: getOutputStream()
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            write(connection.getOutputStream(), POST_DATA);
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
        String scope = format("OtherTransaction/Custom/{0}/run7", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_RESPONSE_CODE);
    }

    @Trace(dispatcher = true)
    private void run7() {
        HttpURLConnection connection = null;
        try {
            // Test 7: getOutputStream() + getResponseCode()
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            write(connection.getOutputStream(), POST_DATA);

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
        String scope = format("OtherTransaction/Custom/{0}/run8", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_RESPONSE_CODE);
    }

    @Trace(dispatcher = true)
    private void run8() {
        HttpURLConnection connection = null;
        try {
            // Test 8: getOutputStream() + getResponseCode() + getInputStream()
            connection = getHttpsExampleComConnection();
            connection.setDoOutput(true);
            write(connection.getOutputStream(), POST_DATA);
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
        String scope = format("OtherTransaction/Custom/{0}/run9", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_RESPONSE_CODE);
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
    public void testHttpURLConnectionMetrics10() {
        run10();
        String scope = format("OtherTransaction/Custom/{0}/run10", TEST_CLASS);

        verifyMetrics(URL, scope, true, GET_RESPONSE_MSG);
    }

    @Trace(dispatcher = true)
    private void run10() {
        HttpURLConnection connection = null;
        try {
            // Test 9: getResponseMessage()
            connection = getHttpsExampleComConnection();
            // GETs URL and reads response message (Network I/O)
            System.out.println("Response message: " + connection.getResponseMessage());
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
        String scope = format("OtherTransaction/Custom/{0}/unresolvedHost", TEST_CLASS);

        verifyMetrics(UNKNOWN_HOST, scope, false, null);
    }

    @Trace(dispatcher = true)
    public void unresolvedHost() throws Exception {
        URL url = new URL(FAKE_HOST);
        URLConnection con = url.openConnection();

        try {
            con.getInputStream();
        } catch (IOException ignored) {
        }
    }

    private void read(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        System.out.println(out);
        reader.close();
    }

    private void write(OutputStream outputStream, String text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        writer.write(text);
        writer.flush();
        writer.close();
    }

    private HttpURLConnection getHttpsExampleComConnection() throws Exception {
        return (HttpURLConnection) new URL(REAL_HOST).openConnection();
    }

    private void verifyMetrics(String url, String scope, boolean reportedExternalCall, String methodInExternalMetric) {
        Set<String> metrics = AgentHelper.getMetrics();
        // Scoped external metrics
        String httpURLConnectionGetInputStreamMetric = format("External/{0}/HttpURLConnection/getInputStream", url);
        String httpURLConnectionGetResponseCodeMetric = format("External/{0}/HttpURLConnection/getResponseCode", url);
        String httpURLConnectionGetResponseMessageMetric = format("External/{0}/HttpURLConnection/getResponseMessage", url);
        // Unscoped external metrics
        String externalHostAllMetric = format("External/{0}/all", url);
        String externalAllMetric = "External/all";
        String externalAllOtherMetric = "External/allOther";

        int scopedMetricCount = 0;
        int unscopedMetricCount = 0;

        if (reportedExternalCall) {
            scopedMetricCount = 1;
            // One of these scoped metrics should be generated when an external call is reported
            Assert.assertTrue(metrics.toString(),
                    metrics.contains(format("External/{0}/HttpURLConnection/" + methodInExternalMetric, url)));

            unscopedMetricCount = 3;
            // All three of these unscoped metrics should be generated when an external call is reported
            Assert.assertTrue(metrics.toString(), metrics.contains(externalHostAllMetric));
            Assert.assertTrue(metrics.toString(), metrics.contains(externalAllMetric));
            Assert.assertTrue(metrics.toString(), metrics.contains(externalAllOtherMetric));
        } else {
            Assert.assertFalse(metrics.toString(),
                    metrics.contains(format("External/{0}/HttpURLConnection/" + methodInExternalMetric, url)));

            Assert.assertFalse(metrics.toString(), metrics.contains(externalHostAllMetric));
            Assert.assertFalse(metrics.toString(), metrics.contains(externalAllMetric));
            Assert.assertFalse(metrics.toString(), metrics.contains(externalAllOtherMetric));
        }

        Map<String, Integer> scopedMetricCounts = getMetricCounts(
                MetricName.create(httpURLConnectionGetInputStreamMetric, scope),
                MetricName.create(httpURLConnectionGetResponseCodeMetric, scope),
                MetricName.create(httpURLConnectionGetResponseMessageMetric, scope)
        );

        Map<String, Integer> unscopedMetricCounts = getMetricCounts(
                MetricName.create(externalHostAllMetric),
                MetricName.create(externalAllMetric),
                MetricName.create(externalAllOtherMetric));

        int actualHttpURLConnectionScopedMetricCount = scopedMetricCounts.get(httpURLConnectionGetInputStreamMetric) +
                scopedMetricCounts.get(httpURLConnectionGetResponseCodeMetric) +
                scopedMetricCounts.get(httpURLConnectionGetResponseMessageMetric);

        int actualHttpURLConnectionUnscopedMetricCount = unscopedMetricCounts.get(externalHostAllMetric) +
                unscopedMetricCounts.get(externalAllMetric) +
                unscopedMetricCounts.get(externalAllOtherMetric);

        assertEquals(scopedMetricCount, actualHttpURLConnectionScopedMetricCount);

        if (scopedMetricCount == 0) {
            assertEquals(0, (int) scopedMetricCounts.get(httpURLConnectionGetInputStreamMetric));
        } else {
            if (methodInExternalMetric != null) {
                if (methodInExternalMetric.equals(GET_INPUT_STREAM)) {
                    assertEquals(1, (int) scopedMetricCounts.get(httpURLConnectionGetInputStreamMetric));
                }
                if (methodInExternalMetric.equals(GET_RESPONSE_CODE)) {
                    assertEquals(1, (int) scopedMetricCounts.get(httpURLConnectionGetResponseCodeMetric));
                }
                if (methodInExternalMetric.equals(GET_RESPONSE_MSG)) {
                    assertEquals(1, (int) scopedMetricCounts.get(httpURLConnectionGetResponseMessageMetric));
                }
            }
        }

        assertEquals(unscopedMetricCount, actualHttpURLConnectionUnscopedMetricCount);

        if (unscopedMetricCount == 0) {
            assertEquals(0, (int) unscopedMetricCounts.get(externalHostAllMetric));
            assertEquals(0, (int) unscopedMetricCounts.get(externalAllMetric));
            assertEquals(0, (int) unscopedMetricCounts.get(externalAllOtherMetric));
        } else {
            assertEquals(1, (int) unscopedMetricCounts.get(externalHostAllMetric));
            assertEquals(1, (int) unscopedMetricCounts.get(externalAllMetric));
            assertEquals(1, (int) unscopedMetricCounts.get(externalAllOtherMetric));
        }
    }

    private Map<String, Integer> getMetricCounts(MetricName... responseTimeMetricNames) {
        Set<MetricName> metricNames = Sets.newHashSet(responseTimeMetricNames);
        return transactions.getMetricCounts(metricNames);
    }
}
