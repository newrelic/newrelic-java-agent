/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionStatsListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedActivity;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.instrumentation.InstrumentTestUtils.retransformClass;

public class APISupportabilityTest implements TransactionStatsListener {

    private TransactionData data;
    private TransactionStats stats;
    private static final String CONFIG_FILE = "configs/cross_app_tracing_test.yml";
    private static final ClassLoader CLASS_LOADER = APISupportabilityTest.class.getClassLoader();

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        ServiceFactory.getTransactionService().addTransactionStatsListener(this);
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionStatsListener(this);
    }

    public EnvironmentHolder setupEnvironmentHolder(String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    public static class TokenApiDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getTokenLinkAndExpire() {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }
    }

    public static class TokenApiNonDispatcherTestClass {
        public void getTokenLinkAndExpire() {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }
    }

    public static class TokenApiDispatcherBridgeTestClass {
        @Trace(dispatcher = true)
        public void getTokenLinkAndExpire() {
            Token token = AgentBridge.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }
    }

    public static class TokenApiNonDispatcherBridgeTestClass {
        public void getTokenLinkAndExpire() {
            Token token = AgentBridge.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }
    }

    public static class SegmentApiTestClass1 {
        @Trace(dispatcher = true)
        public void startAndEndSegment() {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Segment Name");
            segment.reportAsExternal(getGenericExternalParametersArg());
            segment.end();
        }
    }

    public static class SegmentApiTestClass2 {
        public void startAndEndSegment() {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Segment Name");
            segment.reportAsExternal(getGenericExternalParametersArg());
            segment.end();
        }
    }

    public static class SegmentApiTestClass3 {
        @Trace(dispatcher = true)
        public void startAndEndSegment() {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Category", "Segment Name");
            segment.reportAsExternal(getGenericExternalParametersArg());
            segment.end();
        }
    }

    public static class SegmentApiTestClass4 {
        public void startAndEndSegment() {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Category", "Segment Name");
            segment.reportAsExternal(getGenericExternalParametersArg());
            segment.ignore();
        }
    }

    public static class SegmentApiTestClass5 {
        @Trace(dispatcher = true)
        public void startAndEndSegment() {
            TracedActivity tracedActivity = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
            tracedActivity.reportAsExternal(getGenericExternalParametersArg());
            tracedActivity.finish();
        }
    }

    public static class SegmentApiTestClass6 {
        public void startAndEndSegment() {
            TracedActivity tracedActivity = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
            tracedActivity.reportAsExternal(getGenericExternalParametersArg());
            tracedActivity.ignore();
        }
    }

    public static class TxnIgnoreApdexDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getTxnIgnoreApdex() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.ignoreApdex();
        }
    }

    public static class TxnIgnoreApdexNonDispatcherTestClass {
        public void getTxnIgnoreApdex() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.ignoreApdex();
        }
    }

    public static class TxnIgnoreApdexDispatcherBridgeTestClass {
        @Trace(dispatcher = true)
        public void getTxnIgnoreApdex() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            txn.ignoreApdex();
        }
    }

    public static class TxnIgnoreApdexNonDispatcherBridgeTestClass {
        public void getTxnIgnoreApdex() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            txn.ignoreApdex();
        }
    }

    public static class NewRelicIgnoreApdexDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getNewRelicIgnoreApdex() {
            NewRelic.ignoreApdex();
        }
    }

    public static class NewRelicIgnoreApdexNonDispatcherTestClass {
        public void getNewRelicIgnoreApdex() {
            NewRelic.ignoreApdex();
        }
    }

    public static class TxnIgnoreDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getTxnIgnore() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.ignore();
        }
    }

    public static class TxnIgnoreNonDispatcherTestClass {
        public void getTxnIgnore() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.ignore();
        }
    }

    public static class TxnIgnoreDispatcherBridgeTestClass {
        @Trace(dispatcher = true)
        public void getTxnIgnore() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            txn.ignore();
        }
    }

    public static class TxnIgnoreNonDispatcherBridgeTestClass {
        public void getTxnIgnore() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            txn.ignore();
        }
    }

    public static class NewRelicIgnoreDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getNewRelicIgnore() {
            NewRelic.ignoreTransaction();
        }
    }

    public static class NewRelicIgnoreNonDispatcherTestClass {
        public void getNewRelicIgnore() {
            NewRelic.ignoreTransaction();
        }
    }

    public static class ProcessRequestMetadataDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getProcessRequestMetadata() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            String requestMetadata = txn.getRequestMetadata();
            txn.processRequestMetadata(requestMetadata);
        }
    }

    public static class ProcessRequestMetadataNonDispatcherTestClass {
        public void getProcessRequestMetadata() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            String requestMetadata = txn.getRequestMetadata();
            txn.processRequestMetadata(requestMetadata);
        }
    }

    public static class ProcessRequestMetadataDispatcherBridgeTestClass {
        @Trace(dispatcher = true)
        public void getProcessRequestMetadata() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            String requestMetadata = txn.getRequestMetadata();
            txn.processRequestMetadata(requestMetadata);
        }
    }

    public static class ProcessRequestMetadataNonDispatcherBridgeTestClass {
        public void getProcessRequestMetadata() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            String requestMetadata = txn.getRequestMetadata();
            txn.processRequestMetadata(requestMetadata);
        }
    }

    public static class ProcessResponseMetadataDispatcherTestClass1 {
        @Trace(dispatcher = true)
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata);
        }
    }

    public static class ProcessResponseMetadataNonDispatcherTestClass1 {
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata);
        }
    }

    public static class ProcessResponseMetadataDispatcherBridgeTestClass1 {
        @Trace(dispatcher = true)
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata);
        }
    }

    public static class ProcessResponseMetadataNonDispatcherBridgeTestClass1 {
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata);
        }
    }

    public static class ProcessResponseMetadataDispatcherTestClass2 {
        @Trace(dispatcher = true)
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata, getURI());
        }
    }

    public static class ProcessResponseMetadataNonDispatcherTestClass2 {
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata, getURI());
        }
    }

    public static class ProcessResponseMetadataDispatcherBridgeTestClass2 {
        @Trace(dispatcher = true)
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata, getURI());
        }
    }

    public static class ProcessResponseMetadataNonDispatcherBridgeTestClass2 {
        public void getProcessResponseMetadata() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            String fakeResponseMetadata = txn.getRequestMetadata();
            txn.processResponseMetadata(fakeResponseMetadata, getURI());
        }
    }

    public static class TxnSetTransactionNameDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getTxnSetTransactionName() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", "foo", "bar");
        }
    }

    public static class TxnSetTransactionNameNonDispatcherTestClass {
        public void getTxnSetTransactionName() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", "foo", "bar");
        }
    }

    public static class TxnSetTransactionNameDispatcherBridgeTestClass {
        @Trace(dispatcher = true)
        public void getTxnSetTransactionName() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            txn.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", "foo", "bar");
        }
    }

    public static class TxnSetTransactionNameNonDispatcherBridgeTestClass {
        public void getTxnSetTransactionName() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            txn.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", "foo", "bar");
        }
    }

    public static class NewRelicSetTransactionNameDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getNewRelicSetTransactionName() {
            NewRelic.setTransactionName("Category", "Name");
        }
    }

    public static class NewRelicSetTransactionNameNonDispatcherTestClass {
        public void getNewRelicSetTransactionName() {
            NewRelic.setTransactionName("Category", "Name");
        }
    }

    public static class TracedMethodReportAsExternalDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getTracedMethodReportAsExternal() {
            com.newrelic.api.agent.TracedMethod tracedMethod = NewRelic.getAgent().getTransaction().getTracedMethod();
            tracedMethod.reportAsExternal(getGenericExternalParametersArg());
        }
    }

    public static class TracedMethodReportAsExternalNonDispatcherTestClass {
        public void getTracedMethodReportAsExternal() {
            com.newrelic.api.agent.TracedMethod tracedMethod = NewRelic.getAgent().getTransaction().getTracedMethod();
            tracedMethod.reportAsExternal(getGenericExternalParametersArg());
        }
    }

    public static class TracedMethodReportAsExternalDispatcherBridgeTestClass {
        @Trace(dispatcher = true)
        public void getTracedMethodReportAsExternal() {
            com.newrelic.api.agent.TracedMethod tracedMethod = AgentBridge.getAgent().getTransaction().getTracedMethod();
            tracedMethod.reportAsExternal(getGenericExternalParametersArg());
        }
    }

    public static class TracedMethodReportAsExternalNonDispatcherBridgeTestClass {
        public void getTracedMethodReportAsExternal() {
            com.newrelic.api.agent.TracedMethod tracedMethod = AgentBridge.getAgent().getTransaction().getTracedMethod();
            tracedMethod.reportAsExternal(getGenericExternalParametersArg());
        }
    }

    public static class RecordCustomEventDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getRecordCustomEvent() {
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");

            NewRelic.getAgent().getInsights().recordCustomEvent("eventType", map);
        }
    }

    public static class RecordCustomEventNonDispatcherTestClass {
        public void getRecordCustomEvent() {
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");

            NewRelic.getAgent().getInsights().recordCustomEvent("eventType", map);
        }
    }

    public static class NewRelicAPIDispatcherTestClass {
        @Trace(dispatcher = true)
        public void getNewRelicAPI() {
            NewRelic.getAgent().getTransaction().convertToWebTransaction();

            // Set transaction name
            NewRelic.setTransactionName("Category", "Name");

            // reporting custom parameters
            NewRelic.addCustomParameter("key1", 2);
            NewRelic.addCustomParameter("key2", "value");

            // Reporting errors
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");

            NewRelic.noticeError("message1", false);
            NewRelic.noticeError("message2", map, false);
            NewRelic.noticeError(new Throwable("throwable1"), false);
            NewRelic.noticeError(new Throwable("throwable2"), map, false);
            NewRelic.noticeError("message1");
            NewRelic.noticeError("message2", map);
            NewRelic.noticeError(new Throwable("throwable1"));
            NewRelic.noticeError(new Throwable("throwable2"), map);

            // Web framework info
            NewRelic.setAppServerPort(42);
            NewRelic.setServerInfo("AwesomeDispatcher", "4.0");
            NewRelic.setInstanceName("AwesomeInstance");

            // RUM info
            NewRelic.setProductName("AwesomeProduct");
            NewRelic.setUserName("AwesomeUser");
            NewRelic.setAccountName("AwesomeAccount");
        }
    }

    public static class NewRelicAPINonDispatcherTestClass {
        public void getNewRelicAPI() {
            NewRelic.getAgent().getTransaction().convertToWebTransaction();

            // Set transaction name
            NewRelic.setTransactionName("Category", "Name");

            // reporting custom parameters
            NewRelic.addCustomParameter("key1", 2);
            NewRelic.addCustomParameter("key2", "value");

            // Reporting errors
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");

            NewRelic.noticeError("message1", false);
            NewRelic.noticeError("message2", map, false);
            NewRelic.noticeError(new Throwable("throwable1"), false);
            NewRelic.noticeError(new Throwable("throwable2"), map, false);
            NewRelic.noticeError("message1");
            NewRelic.noticeError("message2", map);
            NewRelic.noticeError(new Throwable("throwable1"));
            NewRelic.noticeError(new Throwable("throwable2"), map);

            // Web framework info
            NewRelic.setAppServerPort(42);
            NewRelic.setServerInfo("AwesomeDispatcher", "4.0");
            NewRelic.setInstanceName("AwesomeInstance");

            // RUM info
            NewRelic.setProductName("AwesomeProduct");
            NewRelic.setUserName("AwesomeUser");
            NewRelic.setAccountName("AwesomeAccount");
        }
    }

    @Test
    public void testTokenApiDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TokenApiDispatcherTestClass.class.getName());

        final String getTokenApiMetric = "Supportability/API/Token/API"; // Transaction.getToken
        final String tokenLinkApiMetric = "Supportability/API/Token/Link/API"; // Token.link
        final String tokenExpireApiMetric = "Supportability/API/Token/Expire/API"; // Token.expire
        new TokenApiDispatcherTestClass().getTokenLinkAndExpire();

        waitForTransaction();
        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull("getTokenApiMetric", metricData.get(getTokenApiMetric));
        Assert.assertEquals("getTokenApiMetric", Integer.valueOf(1), metricData.get(getTokenApiMetric));
        Assert.assertNotNull("tokenLinkApiMetric", metricData.get(tokenLinkApiMetric));
        Assert.assertEquals("tokenLinkApiMetric", Integer.valueOf(1), metricData.get(tokenLinkApiMetric));
        Assert.assertNotNull("tokenExpireApiMetric", metricData.get(tokenExpireApiMetric));
        Assert.assertEquals("tokenExpireApiMetric", Integer.valueOf(1), metricData.get(tokenExpireApiMetric));
    }

    @Test
    public void testTokenApiNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TokenApiNonDispatcherTestClass.class.getName());

        final String getTokenApiMetric = "Supportability/API/Token/API"; // Transaction.getToken
        final String tokenLinkApiMetric = "Supportability/API/Token/Link/API"; // Token.link
        final String tokenExpireApiMetric = "Supportability/API/Token/Expire/API"; // Token.expire
        new TokenApiNonDispatcherTestClass().getTokenLinkAndExpire();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(getTokenApiMetric));
        Assert.assertNull(metricData.get(tokenLinkApiMetric));
        Assert.assertNull(metricData.get(tokenExpireApiMetric));
    }

    @Test
    public void testBridgeTokenApiDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TokenApiDispatcherBridgeTestClass.class.getName());

        final String getTokenApiMetric = "Supportability/API/Token/API"; // Transaction.getToken
        final String tokenLinkApiMetric = "Supportability/API/Token/Link/API"; // Token.link
        final String tokenExpireApiMetric = "Supportability/API/Token/Expire/API"; // Token.expire
        new TokenApiDispatcherBridgeTestClass().getTokenLinkAndExpire();

        waitForTransaction();
        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull("getTokenApiMetric", metricData.get(getTokenApiMetric));
        Assert.assertEquals("getTokenApiMetric", Integer.valueOf(1), metricData.get(getTokenApiMetric));
        Assert.assertNotNull("tokenLinkApiMetric", metricData.get(tokenLinkApiMetric));
        Assert.assertEquals("tokenLinkApiMetric", Integer.valueOf(1), metricData.get(tokenLinkApiMetric));
        Assert.assertNotNull("tokenExpireApiMetric", metricData.get(tokenExpireApiMetric));
        Assert.assertEquals("tokenExpireApiMetric", Integer.valueOf(1), metricData.get(tokenExpireApiMetric));
    }

    @Test
    public void testBridgeTokenApiNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TokenApiNonDispatcherBridgeTestClass.class.getName());

        final String getTokenApiMetric = "Supportability/API/Token/API"; // Transaction.getToken
        final String tokenLinkApiMetric = "Supportability/API/Token/Link/API"; // Token.link
        final String tokenExpireApiMetric = "Supportability/API/Token/Expire/API"; // Token.expire
        new TokenApiNonDispatcherBridgeTestClass().getTokenLinkAndExpire();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(getTokenApiMetric));
        Assert.assertNull(metricData.get(tokenLinkApiMetric));
        Assert.assertNull(metricData.get(tokenExpireApiMetric));
    }

    @Test
    public void testSegmentApiSupportabilityTracking() throws Exception {
        retransformClass(SegmentApiTestClass1.class.getName());

        final String startSegmentApiMetric = "Supportability/API/Segment/API"; // Transaction.startSegment
        final String setMetricNameApiMetric = "Supportability/API/Segment/SetMetricName/API"; // Segment.setMetricName
        final String reportAsExternalApiMetric = "Supportability/API/ReportAsExternal/API"; // Segment.reportAsExternal
        final String endApiMetric = "Supportability/API/Segment/End/API"; // Segment.end
        new SegmentApiTestClass1().startAndEndSegment();
        waitForTransaction();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull("startSegmentApiMetric", metricData.get(startSegmentApiMetric));
        Assert.assertEquals("startSegmentApiMetric", Integer.valueOf(1), metricData.get(startSegmentApiMetric));
        Assert.assertNotNull("setMetricNameApiMetric", metricData.get(setMetricNameApiMetric));
        Assert.assertEquals("setMetricNameApiMetric", Integer.valueOf(1), metricData.get(setMetricNameApiMetric));
        Assert.assertNotNull("reportAsExternalApiMetric", metricData.get(reportAsExternalApiMetric));
        Assert.assertEquals("reportAsExternalApiMetric", Integer.valueOf(1), metricData.get(reportAsExternalApiMetric));
        Assert.assertNotNull("endApiMetric", metricData.get(endApiMetric));
        Assert.assertEquals("endApiMetric", Integer.valueOf(1), metricData.get(endApiMetric));
    }

    @Test
    public void testSegmentApiNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(SegmentApiTestClass2.class.getName());

        final String startSegmentApiMetric = "Supportability/API/Segment/API"; // Transaction.startSegment
        final String setMetricNameApiMetric = "Supportability/API/Segment/SetMetricName/API"; // Segment.setMetricName
        final String reportAsExternalApiMetric = "Supportability/API/ReportAsExternal/API"; // Segment.reportAsExternal
        final String endApiMetric = "Supportability/API/Segment/End/API"; // Segment.end
        new SegmentApiTestClass2().startAndEndSegment();
        waitForTransaction();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(startSegmentApiMetric));
        Assert.assertNull(metricData.get(setMetricNameApiMetric));
        Assert.assertNull(metricData.get(reportAsExternalApiMetric));
        Assert.assertNull(metricData.get(endApiMetric));
    }

    @Test
    public void testCategorySegmentApiSupportabilityTracking() throws Exception {
        retransformClass(SegmentApiTestClass3.class.getName());

        final String startSegmentApiMetric = "Supportability/API/Segment/API"; // Transaction.startSegment
        final String setMetricNameApiMetric = "Supportability/API/Segment/SetMetricName/API"; // Segment.setMetricName
        final String reportAsExternalApiMetric = "Supportability/API/ReportAsExternal/API"; // Segment.reportAsExternal
        final String endApiMetric = "Supportability/API/Segment/End/API"; // Segment.end
        new SegmentApiTestClass3().startAndEndSegment();
        waitForTransaction();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull("startSegmentApiMetric", metricData.get(startSegmentApiMetric));
        Assert.assertEquals("startSegmentApiMetric", Integer.valueOf(1), metricData.get(startSegmentApiMetric));
        Assert.assertNotNull("setMetricNameApiMetric", metricData.get(setMetricNameApiMetric));
        Assert.assertEquals("setMetricNameApiMetric", Integer.valueOf(1), metricData.get(setMetricNameApiMetric));
        Assert.assertNotNull("reportAsExternalApiMetric", metricData.get(reportAsExternalApiMetric));
        Assert.assertEquals("reportAsExternalApiMetric", Integer.valueOf(1), metricData.get(reportAsExternalApiMetric));
        Assert.assertNotNull("endApiMetric", metricData.get(endApiMetric));
        Assert.assertEquals("endApiMetric", Integer.valueOf(1), metricData.get(endApiMetric));
    }

    @Test
    public void testCategorySegmentApiNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(SegmentApiTestClass4.class.getName());

        final String startSegmentApiMetric = "Supportability/API/Segment/API"; // Transaction.startSegment
        final String setMetricNameApiMetric = "Supportability/API/Segment/SetMetricName/API"; // Segment.setMetricName
        final String reportAsExternalApiMetric = "Supportability/API/ReportAsExternal/API"; // Segment.reportAsExternal
        final String ignoreApiMetric = "Supportability/API/Segment/Ignore/API"; // Segment.ignore
        new SegmentApiTestClass4().startAndEndSegment();
        waitForTransaction();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(startSegmentApiMetric));
        Assert.assertNull(metricData.get(setMetricNameApiMetric));
        Assert.assertNull(metricData.get(reportAsExternalApiMetric));
        Assert.assertNull(metricData.get(ignoreApiMetric));
    }

    @Test
    public void testTracedActivityApiSupportabilityTracking() throws Exception {
        retransformClass(SegmentApiTestClass5.class.getName());

        final String createAndStartTracedActivityApiMetric = "Supportability/API/Segment/API"; // Transaction.createAndStartTracedActivity
        final String setMetricNameApiMetric = "Supportability/API/Segment/SetMetricName/API"; // TracedActivity.setMetricName
        final String reportAsExternalApiMetric = "Supportability/API/ReportAsExternal/API"; // TracedActivity.reportAsExternal
        final String finishApiMetric = "Supportability/API/Segment/End/API"; // TracedActivity.finish
        new SegmentApiTestClass5().startAndEndSegment();
        waitForTransaction();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull("createAndStartTracedActivityApiMetric", metricData.get(createAndStartTracedActivityApiMetric));
        Assert.assertEquals("createAndStartTracedActivityApiMetric", Integer.valueOf(1), metricData.get(createAndStartTracedActivityApiMetric));
        Assert.assertNotNull("setMetricNameApiMetric", metricData.get(setMetricNameApiMetric));
        Assert.assertEquals("setMetricNameApiMetric", Integer.valueOf(1), metricData.get(setMetricNameApiMetric));
        Assert.assertNotNull("reportAsExternalApiMetric", metricData.get(reportAsExternalApiMetric));
        Assert.assertEquals("reportAsExternalApiMetric", Integer.valueOf(1), metricData.get(reportAsExternalApiMetric));
        Assert.assertNotNull("finishApiMetric", metricData.get(finishApiMetric));
        Assert.assertEquals("finishApiMetric", Integer.valueOf(1), metricData.get(finishApiMetric));
    }

    @Test
    public void testTracedActivityApiNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(SegmentApiTestClass6.class.getName());

        final String createAndStartTracedActivityApiMetric = "Supportability/API/Segment/API"; // Transaction.createAndStartTracedActivity
        final String setMetricNameApiMetric = "Supportability/API/Segment/SetMetricName/API"; // TracedActivity.setMetricName
        final String reportAsExternalApiMetric = "Supportability/API/ReportAsExternal/API"; // TracedActivity.reportAsExternal
        final String ignoreApiMetric = "Supportability/API/Segment/Ignore/API"; // TracedActivity.ignore
        new SegmentApiTestClass6().startAndEndSegment();
        waitForTransaction();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(createAndStartTracedActivityApiMetric));
        Assert.assertNull(metricData.get(setMetricNameApiMetric));
        Assert.assertNull(metricData.get(reportAsExternalApiMetric));
        Assert.assertNull(metricData.get(ignoreApiMetric));
    }

    @Test
    public void testTxnIgnoreApdexDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreApdexDispatcherTestClass.class.getName());

        final String txnIgnoreApdexMetric = "Supportability/API/IgnoreApdex/API";
        new TxnIgnoreApdexDispatcherTestClass().getTxnIgnoreApdex();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(txnIgnoreApdexMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(txnIgnoreApdexMetric));
    }

    @Test
    public void testTxnIgnoreApdexNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreApdexNonDispatcherTestClass.class.getName());

        final String txnIgnoreApdexMetric = "Supportability/API/IgnoreApdex/API";
        new TxnIgnoreApdexNonDispatcherTestClass().getTxnIgnoreApdex();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(txnIgnoreApdexMetric));
    }

    @Test
    public void testBridgeTxnIgnoreApdexDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreApdexDispatcherBridgeTestClass.class.getName());

        final String txnIgnoreApdexMetric = "Supportability/API/IgnoreApdex/API";
        new TxnIgnoreApdexDispatcherBridgeTestClass().getTxnIgnoreApdex();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(txnIgnoreApdexMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(txnIgnoreApdexMetric));
    }

    @Test
    public void testBridgeTxnIgnoreApdexNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreApdexNonDispatcherBridgeTestClass.class.getName());

        final String txnIgnoreApdexMetric = "Supportability/API/IgnoreApdex/API";
        new TxnIgnoreApdexNonDispatcherBridgeTestClass().getTxnIgnoreApdex();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(txnIgnoreApdexMetric));
    }

    @Test
    public void testNewRelicIgnoreApdexDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicIgnoreApdexDispatcherTestClass.class.getName());

        final String newRelicIgnoreApdexMetric = "Supportability/API/IgnoreApdex/API";
        new NewRelicIgnoreApdexDispatcherTestClass().getNewRelicIgnoreApdex();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(newRelicIgnoreApdexMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(newRelicIgnoreApdexMetric));
    }

    @Test
    public void testNewRelicIgnoreApdexNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicIgnoreApdexNonDispatcherTestClass.class.getName());

        final String newRelicIgnoreApdexMetric = "Supportability/API/IgnoreApdex/API";
        new NewRelicIgnoreApdexNonDispatcherTestClass().getNewRelicIgnoreApdex();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(newRelicIgnoreApdexMetric));
    }

    @Test
    public void testTxnIgnoreDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreDispatcherTestClass.class.getName());

        final String txnIgnoreMetric = "Supportability/API/Ignore/API";
        new TxnIgnoreDispatcherTestClass().getTxnIgnore();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(txnIgnoreMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(txnIgnoreMetric));
    }

    @Test
    public void testTxnIgnoreNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreNonDispatcherTestClass.class.getName());

        final String txnIgnoreMetric = "Supportability/API/Ignore/API";
        new TxnIgnoreNonDispatcherTestClass().getTxnIgnore();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(txnIgnoreMetric));
    }

    @Test
    public void testBridgeTxnIgnoreDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreDispatcherBridgeTestClass.class.getName());

        final String txnIgnoreMetric = "Supportability/API/Ignore/API";
        new TxnIgnoreDispatcherBridgeTestClass().getTxnIgnore();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(txnIgnoreMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(txnIgnoreMetric));
    }

    @Test
    public void testBridgeTxnIgnoreNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnIgnoreNonDispatcherBridgeTestClass.class.getName());

        final String txnIgnoreMetric = "Supportability/API/Ignore/API";
        new TxnIgnoreNonDispatcherBridgeTestClass().getTxnIgnore();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(txnIgnoreMetric));
    }

    @Test
    public void testNewRelicIgnoreDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicIgnoreDispatcherTestClass.class.getName());

        final String newRelicIgnoreMetric = "Supportability/API/Ignore/API";
        new NewRelicIgnoreDispatcherTestClass().getNewRelicIgnore();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(newRelicIgnoreMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(newRelicIgnoreMetric));
    }

    @Test
    public void testNewRelicIgnoreNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicIgnoreNonDispatcherTestClass.class.getName());

        final String newRelicIgnoreMetric = "Supportability/API/Ignore/API";
        new NewRelicIgnoreNonDispatcherTestClass().getNewRelicIgnore();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(newRelicIgnoreMetric));
    }

    @Test
    public void testProcessRequestMetadataDispatcherSupportabilityTracking() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder("cat_enabled_dt_disabled_test");

        try {
            retransformClass(ProcessRequestMetadataDispatcherTestClass.class.getName());

            final String processRequestMetadataMetric = "Supportability/API/ProcessRequestMetadata/API";
            new ProcessRequestMetadataDispatcherTestClass().getProcessRequestMetadata();

            Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
            Assert.assertNotNull(metricData.get(processRequestMetadataMetric));
            Assert.assertEquals(Integer.valueOf(1), metricData.get(processRequestMetadataMetric));
        } finally {
            holder.close();
        }
    }

    @Test
    public void testProcessRequestMetadataNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(ProcessRequestMetadataNonDispatcherTestClass.class.getName());

        final String processRequestMetadataMetric = "Supportability/API/ProcessRequestMetadata/API";
        new ProcessRequestMetadataNonDispatcherTestClass().getProcessRequestMetadata();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(processRequestMetadataMetric));
    }

    @Test
    public void testBridgeProcessRequestMetadataDispatcherSupportabilityTracking() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder("cat_enabled_dt_disabled_test");

        try {
            retransformClass(ProcessRequestMetadataDispatcherBridgeTestClass.class.getName());

            final String processRequestMetadataMetric = "Supportability/API/ProcessRequestMetadata/API";
            new ProcessRequestMetadataDispatcherBridgeTestClass().getProcessRequestMetadata();

            Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
            Assert.assertNotNull(metricData.get(processRequestMetadataMetric));
            Assert.assertEquals(Integer.valueOf(1), metricData.get(processRequestMetadataMetric));
        } finally {
            holder.close();
        }
    }

    @Test
    public void testBridgeProcessRequestMetadataNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(ProcessRequestMetadataNonDispatcherBridgeTestClass.class.getName());

        final String processRequestMetadataMetric = "Supportability/API/ProcessRequestMetadata/API";
        new ProcessRequestMetadataNonDispatcherBridgeTestClass().getProcessRequestMetadata();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(processRequestMetadataMetric));
    }

    @Test
    public void testProcessResponseMetadataDispatcherSupportabilityTracking1() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder("cat_enabled_dt_disabled_test");

        try {
            retransformClass(ProcessResponseMetadataDispatcherTestClass1.class.getName());

            final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
            new ProcessResponseMetadataDispatcherTestClass1().getProcessResponseMetadata();

            Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
            Assert.assertNotNull(metricData.get(processResponseMetadataMetric));
            Assert.assertEquals(Integer.valueOf(1), metricData.get(processResponseMetadataMetric));
        } finally {
            holder.close();
        }
    }

    @Test
    public void testProcessResponseMetadataNonDispatcherSupportabilityTracking1() throws Exception {
        retransformClass(ProcessResponseMetadataNonDispatcherTestClass1.class.getName());

        final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
        new ProcessResponseMetadataNonDispatcherTestClass1().getProcessResponseMetadata();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(processResponseMetadataMetric));
    }

    @Test
    public void testBridgeProcessResponseMetadataDispatcherSupportabilityTracking1() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder("cat_enabled_dt_disabled_test");

        try {
            retransformClass(ProcessResponseMetadataDispatcherBridgeTestClass1.class.getName());

            final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
            new ProcessResponseMetadataDispatcherBridgeTestClass1().getProcessResponseMetadata();

            Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
            Assert.assertNotNull(metricData.get(processResponseMetadataMetric));
            Assert.assertEquals(Integer.valueOf(1), metricData.get(processResponseMetadataMetric));
        } finally {
            holder.close();
        }
    }

    @Test
    public void testBridgeProcessResponseMetadataNonDispatcherSupportabilityTracking1() throws Exception {
        retransformClass(ProcessResponseMetadataNonDispatcherBridgeTestClass1.class.getName());

        final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
        new ProcessResponseMetadataNonDispatcherBridgeTestClass1().getProcessResponseMetadata();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(processResponseMetadataMetric));
    }

    @Test
    public void testProcessResponseMetadataDispatcherSupportabilityTracking2() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder("cat_enabled_dt_disabled_test");

        try {
            retransformClass(ProcessResponseMetadataDispatcherTestClass2.class.getName());

            final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
            new ProcessResponseMetadataDispatcherTestClass2().getProcessResponseMetadata();

            Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
            Assert.assertNotNull(metricData.get(processResponseMetadataMetric));
            Assert.assertEquals(Integer.valueOf(1), metricData.get(processResponseMetadataMetric));
        } finally {
            holder.close();
        }
    }

    @Test
    public void testProcessResponseMetadataNonDispatcherSupportabilityTracking2() throws Exception {
        retransformClass(ProcessResponseMetadataNonDispatcherTestClass2.class.getName());

        final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
        new ProcessResponseMetadataNonDispatcherTestClass2().getProcessResponseMetadata();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(processResponseMetadataMetric));
    }

    @Test
    public void testBridgeProcessResponseMetadataDispatcherSupportabilityTracking2() throws Exception {
        // override default agent config to disabled distributed tracing and use CAT instead
        EnvironmentHolder holder = setupEnvironmentHolder("cat_enabled_dt_disabled_test");

        try {
            retransformClass(ProcessResponseMetadataDispatcherBridgeTestClass2.class.getName());

            final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
            new ProcessResponseMetadataDispatcherBridgeTestClass2().getProcessResponseMetadata();

            Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
            Assert.assertNotNull(metricData.get(processResponseMetadataMetric));
            Assert.assertEquals(Integer.valueOf(1), metricData.get(processResponseMetadataMetric));
        } finally {
            holder.close();
        }
    }

    @Test
    public void testBridgeProcessResponseMetadataNonDispatcherSupportabilityTracking2() throws Exception {
        retransformClass(ProcessResponseMetadataNonDispatcherBridgeTestClass2.class.getName());

        final String processResponseMetadataMetric = "Supportability/API/ProcessResponseMetadata/API";
        new ProcessResponseMetadataNonDispatcherBridgeTestClass2().getProcessResponseMetadata();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(processResponseMetadataMetric));
    }

    @Test
    public void testTxnSetTransactionNameDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnSetTransactionNameDispatcherTestClass.class.getName());

        final String txnSetTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        new TxnSetTransactionNameDispatcherTestClass().getTxnSetTransactionName();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(txnSetTransactionNameMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(txnSetTransactionNameMetric));
    }

    @Test
    public void testTxnSetTransactionNameNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnSetTransactionNameNonDispatcherTestClass.class.getName());

        final String txnSetTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        new TxnSetTransactionNameNonDispatcherTestClass().getTxnSetTransactionName();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(txnSetTransactionNameMetric));
    }

    @Test
    public void testBridgeTxnSetTransactionNameDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnSetTransactionNameDispatcherBridgeTestClass.class.getName());

        final String txnSetTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        new TxnSetTransactionNameDispatcherBridgeTestClass().getTxnSetTransactionName();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(txnSetTransactionNameMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(txnSetTransactionNameMetric));
    }

    @Test
    public void testBridgeTxnSetTransactionNameNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TxnSetTransactionNameNonDispatcherBridgeTestClass.class.getName());

        final String txnSetTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        new TxnSetTransactionNameNonDispatcherBridgeTestClass().getTxnSetTransactionName();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(txnSetTransactionNameMetric));
    }

    @Test
    public void testNewRelicSetTransactionNameDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicSetTransactionNameDispatcherTestClass.class.getName());

        final String newRelicSetTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        new NewRelicSetTransactionNameDispatcherTestClass().getNewRelicSetTransactionName();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(newRelicSetTransactionNameMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(newRelicSetTransactionNameMetric));
    }

    @Test
    public void testNewRelicSetTransactionNameNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicSetTransactionNameNonDispatcherTestClass.class.getName());

        final String newRelicSetTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        new NewRelicSetTransactionNameNonDispatcherTestClass().getNewRelicSetTransactionName();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(newRelicSetTransactionNameMetric));
    }

    @Test
    public void testTracedMethodReportAsExternalDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TracedMethodReportAsExternalDispatcherTestClass.class.getName());

        final String tracedMethodReportAsExternalMetric = "Supportability/API/ReportAsExternal/API";
        new TracedMethodReportAsExternalDispatcherTestClass().getTracedMethodReportAsExternal();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(tracedMethodReportAsExternalMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(tracedMethodReportAsExternalMetric));
    }

    @Test
    public void testTracedMethodReportAsExternalNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TracedMethodReportAsExternalNonDispatcherTestClass.class.getName());

        final String tracedMethodReportAsExternalMetric = "Supportability/API/ReportAsExternal/API";
        new TracedMethodReportAsExternalNonDispatcherTestClass().getTracedMethodReportAsExternal();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(tracedMethodReportAsExternalMetric));
    }

    @Test
    public void testBridgeTracedMethodReportAsExternalDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TracedMethodReportAsExternalDispatcherBridgeTestClass.class.getName());

        final String tracedMethodReportAsExternalMetric = "Supportability/API/ReportAsExternal/API";
        new TracedMethodReportAsExternalDispatcherBridgeTestClass().getTracedMethodReportAsExternal();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(tracedMethodReportAsExternalMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(tracedMethodReportAsExternalMetric));
    }

    @Test
    public void testBridgeTracedMethodReportAsExternalNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(TracedMethodReportAsExternalNonDispatcherBridgeTestClass.class.getName());

        final String tracedMethodReportAsExternalMetric = "Supportability/API/ReportAsExternal/API";
        new TracedMethodReportAsExternalNonDispatcherBridgeTestClass().getTracedMethodReportAsExternal();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull(metricData.get(tracedMethodReportAsExternalMetric));
    }

    @Test
    public void testRecordCustomEventDispatcherSupportabilityTracking() throws Exception {
        retransformClass(RecordCustomEventDispatcherTestClass.class.getName());

        final String recordCustomEventMetric = "Supportability/API/RecordCustomEvent/API"; // Insights.recordCustomEvent
        new RecordCustomEventDispatcherTestClass().getRecordCustomEvent();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull(metricData.get(recordCustomEventMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(recordCustomEventMetric));
    }

    @Test
    public void testRecordCustomEventNonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(RecordCustomEventNonDispatcherTestClass.class.getName());

        final String recordCustomEventMetric = "Supportability/API/RecordCustomEvent/API"; // Insights.recordCustomEvent
        new RecordCustomEventNonDispatcherTestClass().getRecordCustomEvent();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Insights API doesn't require @Trace(dispatcher = true)
        Assert.assertNotNull(metricData.get(recordCustomEventMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(recordCustomEventMetric));
    }

    @Test
    public void testNewRelicAPIDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicAPIDispatcherTestClass.class.getName());

        final String setTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        final String addCustomParameterMetric = "Supportability/API/AddCustomParameter/API";
        final String noticeErrorMetric = "Supportability/API/NoticeError/API";
        final String setAppServerPort = "Supportability/API/SetAppServerPort/API";
        final String setServerInfo = "Supportability/API/SetServerInfo/API";
        final String setInstanceName = "Supportability/API/SetInstanceName/API";
        final String setProductName = "Supportability/API/SetProductName/API";
        final String setUserName = "Supportability/API/SetUserName/API";
        final String setAccountName = "Supportability/API/SetAccountName/API";
        new NewRelicAPIDispatcherTestClass().getNewRelicAPI();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNotNull("setTransactionNameMetric", metricData.get(setTransactionNameMetric));
        Assert.assertEquals("setTransactionNameMetric", Integer.valueOf(2), metricData.get(setTransactionNameMetric));
        Assert.assertNotNull("addCustomParameterMetric", metricData.get(addCustomParameterMetric));
        Assert.assertEquals("addCustomParameterMetric", Integer.valueOf(5), metricData.get(addCustomParameterMetric));
        Assert.assertNotNull("noticeErrorMetric", metricData.get(noticeErrorMetric));
        Assert.assertEquals("noticeErrorMetric", Integer.valueOf(8), metricData.get(noticeErrorMetric));
        Assert.assertNotNull("setAppServerPort", metricData.get(setAppServerPort));
        Assert.assertEquals("setAppServerPort", Integer.valueOf(1), metricData.get(setAppServerPort));
        Assert.assertNotNull("setServerInfo", metricData.get(setServerInfo));
        Assert.assertEquals("setServerInfo", Integer.valueOf(1), metricData.get(setServerInfo));
        Assert.assertNotNull("setInstanceName", metricData.get(setInstanceName));
        Assert.assertEquals("setInstanceName", Integer.valueOf(1), metricData.get(setInstanceName));
        Assert.assertNotNull("setProductName", metricData.get(setProductName));
        Assert.assertEquals("setProductName", Integer.valueOf(1), metricData.get(setProductName));
        Assert.assertNotNull("setUserName", metricData.get(setUserName));
        Assert.assertEquals("setUserName", Integer.valueOf(1), metricData.get(setUserName));
        Assert.assertNotNull("setAccountName", metricData.get(setAccountName));
        Assert.assertEquals("setAccountName", Integer.valueOf(1), metricData.get(setAccountName));
    }

    @Test
    public void testNewRelicAPINonDispatcherSupportabilityTracking() throws Exception {
        retransformClass(NewRelicAPINonDispatcherTestClass.class.getName());

        final String setTransactionNameMetric = "Supportability/API/SetTransactionName/API";
        final String addCustomParameterMetric = "Supportability/API/AddCustomParameter/API";
        final String noticeErrorMetric = "Supportability/API/NoticeError/API";
        final String setAppServerPort = "Supportability/API/SetAppServerPort/API";
        final String setServerInfo = "Supportability/API/SetServerInfo/API";
        final String setInstanceName = "Supportability/API/SetInstanceName/API";
        final String setProductName = "Supportability/API/SetProductName/API";
        final String setUserName = "Supportability/API/SetUserName/API";
        final String setAccountName = "Supportability/API/SetAccountName/API";
        new NewRelicAPINonDispatcherTestClass().getNewRelicAPI();

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // Missing @Trace(dispatcher = true) means that we shouldn't capture API usage since the API did nothing
        Assert.assertNull("setTransactionNameMetric", metricData.get(setTransactionNameMetric));
        Assert.assertNull("addCustomParameterMetric", metricData.get(addCustomParameterMetric));
        Assert.assertNull("setProductName", metricData.get(setProductName));
        Assert.assertNull("setUserName", metricData.get(setUserName));
        Assert.assertNull("setAccountName", metricData.get(setAccountName));

        // These APIs don't require @Trace(dispatcher = true)
        Assert.assertNotNull("noticeErrorMetric", metricData.get(noticeErrorMetric));
        Assert.assertEquals("noticeErrorMetric", Integer.valueOf(8), metricData.get(noticeErrorMetric));
        Assert.assertNotNull("setAppServerPort", metricData.get(setAppServerPort));
        Assert.assertEquals("setAppServerPort", Integer.valueOf(1), metricData.get(setAppServerPort));
        Assert.assertNotNull("setServerInfo", metricData.get(setServerInfo));
        Assert.assertEquals("setServerInfo", Integer.valueOf(1), metricData.get(setServerInfo));
        Assert.assertNotNull("setInstanceName", metricData.get(setInstanceName));
        Assert.assertEquals("setInstanceName", Integer.valueOf(1), metricData.get(setInstanceName));
    }

    /*
     * Helper methods
     */
    private static URI getURI() {
        URI uri = null;
        try {
            uri = new URI("http://localhost:8088/test/this/path?name=Bob");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uri;
    }

    private static ExternalParameters getGenericExternalParametersArg() {
        String library = "HttpClient";
        URI uri = getURI();
        String operation = "execute";
        return GenericParameters.library(library).uri(uri).procedure(operation).build();
    }

    @Override
    public void dispatcherTransactionStatsFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;
    }

    private void waitForTransaction() {
        long start = System.currentTimeMillis();
        // Wait for data to be available
        while ((System.currentTimeMillis() - start) < 5000 && (data == null || stats == null)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }
}
