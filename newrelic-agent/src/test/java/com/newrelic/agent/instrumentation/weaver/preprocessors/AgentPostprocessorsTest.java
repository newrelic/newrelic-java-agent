/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.preprocessors;

import com.google.common.collect.ImmutableMultimap;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedActivity;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.internal.WeavePackageType;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest.TEST_INPUT_QUERY_CONVERTER;
import static com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest.TEST_QUERY_CONVERTER;
import static com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest.addToClassloader;
import static com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest.getClassBytesFromClassLoaderResource;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AgentPostprocessorsTest {

    @Test
    public void testFieldModuleApiCalls() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPostprocessorsTest$ApiTestClass";

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);

        ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> weaveTraceDetailsTrackers = new ConcurrentHashMap<>();

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, weaveTraceDetailsTrackers);
        AgentPostprocessors postprocessors = new AgentPostprocessors();
        preprocessors.setInstrumentationTitle("com.newrelic.some-field-module");

        // These are here so we can have additional test coverage even though we don't want these to be tracked for real
        postprocessors.addTrackedApiMethods(ImmutableMultimap
                .<String, Method>builder()
                .put(Type.getType(NewRelic.class).getInternalName(), new Method("recordResponseTimeMetric",
                        Type.VOID_TYPE, new Type[] { Type.getType(String.class), Type.LONG_TYPE }))
                .put(Type.getType(ApiTestClass.ExceptionClass.class).getInternalName(), new Method("throwException",
                        Type.VOID_TYPE, new Type[] {}))

                .build()
        );
        WeavePackageType weavePackageType = WeavePackageType.FIELD;
        postprocessors.setWeavePackageType(weavePackageType);

        cv = postprocessors.wrapApiCallsForSupportability(cv);
        source.accept(cv);

        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);
        testApiTestClass(clazz, weavePackageType);
    }

    private void testApiTestClass(Class<?> clazz, WeavePackageType weavePackageType) throws Exception {
        ThreadLocal<WeavePackageType> weavePackageTypeSpy = new ThreadLocal<WeavePackageType>() {
            @Override
            protected WeavePackageType initialValue() {
                return null;
            }
        };
        AgentBridge.currentApiSource = spy(weavePackageTypeSpy);

        ApiTestClass testClass = (ApiTestClass) clazz.newInstance();

        // Token API
        assertBeforeApi();
        Token token = testClass.getToken();
        assertNotNull(token);
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        com.newrelic.api.agent.Transaction transaction = testClass.getTransaction();
        assertNotNull(transaction);
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.getTokenNoReturn();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.getTokenNoReturnException();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.getTokenSeparateLinkExpireNoReturn();
        assertAfterApi(3, weavePackageType);

        assertBeforeApi();
        testClass.getTokenSeparateLinkExpireNoReturnException();
        assertAfterApi(3, weavePackageType);

        // Token API (Bridge)
        assertBeforeApi();
        com.newrelic.agent.bridge.Token tokenBridge = testClass.getTokenBridge();
        assertNotNull(tokenBridge);
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        Transaction transactionBridge = testClass.getTransactionBridge();
        assertNotNull(transactionBridge);
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.getTokenNoReturnBridge();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.getTokenNoReturnExceptionBridge();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.getTokenSeparateLinkExpireNoReturnBridge();
        assertAfterApi(3, weavePackageType);

        assertBeforeApi();
        testClass.getTokenSeparateLinkExpireNoReturnExceptionBridge();
        assertAfterApi(3, weavePackageType);

        // Segment API
        assertBeforeApi();
        testClass.startSegmentNoArgsWithTx(NewRelic.getAgent().getTransaction());
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentNoArgs();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentNoArgsNoReturn();
        assertAfterApi(3, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentNoArgsNoReturnException();
        assertAfterApi(3, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentArgs();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentArgsNoReturn();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentArgsNoReturnException();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentNoArgsWithGetToken();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentNoArgsNoReturnWithGetToken();
        assertAfterApi(3, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentNoArgsNoReturnExceptionWithGetToken();
        assertAfterApi(3, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentArgsWithGetToken();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentArgsNoReturnWithGetToken();
        assertAfterApi(4, weavePackageType);

        assertBeforeApi();
        testClass.startSegmentArgsNoReturnExceptionWithGetToken();
        assertAfterApi(3, weavePackageType);

        // TracedActivity API (Bridge)
        assertBeforeApi();
        testClass.createAndStartTracedActivity();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.createAndStartTracedActivityNoReturn();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.createAndStartTracedActivityNoReturnException();
        assertAfterApi(1, weavePackageType);

        // NewRelic API
        assertBeforeApi();
        testClass.noticeErrorWithStringAndEmptyMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.noticeErrorWithStringAndMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.noticeErrorWithThrowableAndEmptyMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.noticeErrorWithThrowableAndMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.noticeErrorExpectedWithStringAndEmptyMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.noticeErrorExpectedWithStringAndMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.noticeErrorExpectedWithThrowableAndEmptyMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.noticeErrorExpectedWithThrowableAndMap();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.addCustomParameter();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.ignoreApdexNewRelic();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.ignoreTransaction();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setAppServerPort();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setInstanceName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setServerInfo();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setTransactionNameNewRelic();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setUserName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setProductName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setAccountName();
        assertAfterApi(1, weavePackageType);

        // Transaction API
        assertBeforeApi();
        testClass.ignore();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.ignoreApdexTransaction();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.processRequestMetadata();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.processResponseMetadataWithURI();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.processResponseMetadata();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.setTransactionName();
        assertAfterApi(1, weavePackageType);

        // Transaction API (Bridge)
        assertBeforeApi();
        testClass.setTransactionNameBridge();
        assertAfterApi(1, weavePackageType);

        // TracedMethod API
        assertBeforeApi();
        testClass.reportAsExternal();
        assertAfterApi(1, weavePackageType);

        // TracedMethod API (Bridge)
        assertBeforeApi();
        testClass.reportAsExternalBridge();
        assertAfterApi(1, weavePackageType);

        // Insights API
        assertBeforeApi();
        testClass.recordCustomEvent();
        assertAfterApi(1, weavePackageType);

        // Public API
        assertBeforeApi();
        testClass.publicApiCustomParameter();
        assertAfterApi(2, weavePackageType);

        assertBeforeApi();
        testClass.publicApiBrowserHeaderFooter();
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.publicApiIgnoreApdex();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiIgnoreTransaction();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiNoticeError();
        assertAfterApi(8, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetAccountName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetAppServerPort();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetInstanceName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetProductName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetRequestAndResponse();
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetServerInfo();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetTransactionName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.publicApiSetUserName();
        assertAfterApi(1, weavePackageType);

        // Private API
        assertBeforeApi();
        testClass.privateApiCustomAttribute();
        assertAfterApi(3, weavePackageType);

        assertBeforeApi();
        testClass.privateApiMBeanServer();
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.privateApiSampler();
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.privateApiTracerParameter();
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.privateApiReportError();
        assertAfterApi(0, weavePackageType);

        assertBeforeApi();
        testClass.privateApiSetServerInfo();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.privateApiSetAppServerPort();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        testClass.privateApiSetInstanceName();
        assertAfterApi(1, weavePackageType);

        assertBeforeApi();
        boolean exceptionThrown = false;
        try {
            testClass.throwExceptionTest();
        } catch (IOException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            fail("Exception was expected but not thrown");
        }
        assertAfterApi(1, exceptionThrown ? 0 : 1, weavePackageType);
    }

    private void assertBeforeApi() {
        assertNull(AgentBridge.currentApiSource.get());
    }

    private void assertAfterApi(int times, WeavePackageType weavePackageType) {
        assertAfterApi(times, times, weavePackageType);
    }

    private void assertAfterApi(int setTimes, int removeTimes, WeavePackageType weavePackageType) {
        verify(AgentBridge.currentApiSource, times(setTimes)).set(eq(weavePackageType));
        verify(AgentBridge.currentApiSource, times(removeTimes)).remove();
        if (removeTimes != 0) {
            assertNull(AgentBridge.currentApiSource.get());
        }
        reset(AgentBridge.currentApiSource);
    }

    public static class ApiTestClass {

        public com.newrelic.api.agent.Transaction getTransaction() {
            return NewRelic.getAgent().getTransaction();
        }

        // Token API
        public Token getToken() {
            return NewRelic.getAgent().getTransaction().getToken();
        }

        public void getTokenNoReturn() {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }

        public void getTokenNoReturnException() throws IOException {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }

        public void getTokenSeparateLinkExpireNoReturn() {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            token.link();
            token.expire();
        }

        public void getTokenSeparateLinkExpireNoReturnException() throws IOException {
            Token token = NewRelic.getAgent().getTransaction().getToken();
            token.link();
            token.expire();
        }

        // Token API (Bridge)
        public Transaction getTransactionBridge() {
            return AgentBridge.getAgent().getTransaction();
        }

        public com.newrelic.agent.bridge.Token getTokenBridge() {
            return AgentBridge.getAgent().getTransaction().getToken();
        }

        public void getTokenNoReturnBridge() {
            Token token = AgentBridge.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }

        public void getTokenNoReturnExceptionBridge() throws IOException {
            Token token = AgentBridge.getAgent().getTransaction().getToken();
            token.linkAndExpire();
        }

        public void getTokenSeparateLinkExpireNoReturnBridge() {
            Token token = AgentBridge.getAgent().getTransaction().getToken();
            token.link();
            token.expire();
        }

        public void getTokenSeparateLinkExpireNoReturnExceptionBridge() throws IOException {
            Token token = AgentBridge.getAgent().getTransaction().getToken();
            token.link();
            token.expire();
        }

        // Segment API
        public Segment startSegmentNoArgsWithTx(com.newrelic.api.agent.Transaction transaction) {
            NewRelic.recordResponseTimeMetric("Hello", 100L);
            return transaction.startSegment("Segment Name");
        }

        public Segment startSegmentNoArgs() {
            return NewRelic.getAgent().getTransaction().startSegment("Segment Name");
        }

        public void startSegmentNoArgsNoReturn() {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Segment Name");
            segment.reportAsExternal(DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build()
            );
            segment.ignore();
        }

        public void startSegmentNoArgsNoReturnException() throws IOException {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Segment Name");
            segment.reportAsExternal(DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build()
            );
            segment.end();
        }

        public Segment startSegmentArgs() {
            return NewRelic.getAgent().getTransaction().startSegment("Category", "My Segment");
        }

        public void startSegmentArgsNoReturn() {
            String segmentName = "My Segment";
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Category", segmentName);
            segment.ignore();
        }

        private static String instanceSegmentName = "My Segment";

        public void startSegmentArgsNoReturnException() throws IOException {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Category", instanceSegmentName);
            segment.end();
        }

        public Segment startSegmentNoArgsWithGetToken() {
            com.newrelic.api.agent.Transaction tx = NewRelic.getAgent().getTransaction();
            Token token = tx.getToken();
            return tx.startSegment("Category", "Segment Name");
        }

        public void startSegmentNoArgsNoReturnWithGetToken() {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Segment Name");
            NewRelic.getAgent().getTransaction().getToken();
            segment.ignore();
        }

        public Token startSegmentNoArgsNoReturnExceptionWithGetToken() throws IOException {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("Category", "Segment Name");
            Token token = NewRelic.getAgent().getTransaction().getToken();
            segment.end();
            return token;
        }

        public Segment startSegmentArgsWithGetToken() {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment("My Segment");
            Token token = NewRelic.getAgent().getTransaction().getToken();
            return segment;
        }

        public void startSegmentArgsNoReturnWithGetToken() {
            String segmentName = "My Segment";
            Segment segment = NewRelic.getAgent().getTransaction().startSegment(segmentName);
            Token token = NewRelic.getAgent().getTransaction().getToken();
            segment.ignore();
            token.linkAndExpire();
        }

        public void startSegmentArgsNoReturnExceptionWithGetToken() throws IOException {
            Segment segment = NewRelic.getAgent().getTransaction().startSegment(instanceSegmentName);
            segment.end();
            Token token = NewRelic.getAgent().getTransaction().getToken();
        }

        // TracedActivity API (Bridge)
        public Segment createAndStartTracedActivity() {
            return AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
        }

        public void createAndStartTracedActivityNoReturn() {
            TracedActivity tracedActivity = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
            tracedActivity.ignoreIfUnfinished();
        }

        public void createAndStartTracedActivityNoReturnException() throws IOException {
            TracedActivity tracedActivity = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
            tracedActivity.finish(new RuntimeException());
        }

        // NewRelic API
        public void noticeErrorWithStringAndEmptyMap() {
            NewRelic.noticeError("Some error message", new HashMap<String, String>());
        }

        public void noticeErrorWithStringAndMap() {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");

            NewRelic.noticeError("Some error message", data);
        }

        public void noticeErrorWithThrowableAndEmptyMap() {
            NewRelic.noticeError(new OutOfMemoryError("OOM"), new HashMap<String, String>());
        }

        public void noticeErrorWithThrowableAndMap() {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");

            NewRelic.noticeError(new IOException("IO"), data);
        }

        public void noticeErrorExpectedWithStringAndEmptyMap() {
            NewRelic.noticeError("Some error message", new HashMap<String, String>(), false);
        }

        public void noticeErrorExpectedWithStringAndMap() {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");

            NewRelic.noticeError("Some error message", data, false);
        }

        public void noticeErrorExpectedWithThrowableAndEmptyMap() {
            NewRelic.noticeError(new OutOfMemoryError("OOM"), new HashMap<String, String>(), false);
        }

        public void noticeErrorExpectedWithThrowableAndMap() {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");

            NewRelic.noticeError(new IOException("IO"), data, false);
        }

        public void addCustomParameter() {
            NewRelic.addCustomParameter("key1", 2);
            NewRelic.addCustomParameter("key2", "value");
        }

        public void ignoreApdexNewRelic() {
            NewRelic.ignoreApdex();
        }

        public void ignoreTransaction() {
            NewRelic.ignoreTransaction();
        }

        public void setAppServerPort() {
            NewRelic.setAppServerPort(8081);
        }

        public void setInstanceName() {
            NewRelic.setInstanceName("instanceName");
        }

        public void setServerInfo() {
            NewRelic.setServerInfo("dispatcherName", "version");
        }

        public void setTransactionNameNewRelic() {
            NewRelic.setTransactionName("category", "name");
        }

        public void setUserName() {
            NewRelic.setUserName("name");
        }

        public void setProductName() {
            NewRelic.setProductName("name");
        }

        public void setAccountName() {
            NewRelic.setAccountName("accountName");
        }

        // Transaction API
        public void ignore() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.ignore();
        }

        public void ignoreApdexTransaction() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.ignoreApdex();
        }

        public void processRequestMetadata() {
            NewRelic.getAgent().getTransaction().processRequestMetadata("ResponseMetadata");
        }

        public void processResponseMetadataWithURI() {
            try {
                NewRelic.getAgent().getTransaction().processResponseMetadata("ResponseMetadata",
                        new URI("http://newrelic.com"));
            } catch (URISyntaxException e) {
                NewRelic.getAgent().getLogger().log(Level.INFO, e, "URI syntax is incorrect");
            }
        }

        public void processResponseMetadata() {
            NewRelic.getAgent().getTransaction().processResponseMetadata("ResponseMetadata");
        }

        public void setTransactionName() {
            com.newrelic.api.agent.Transaction txn = NewRelic.getAgent().getTransaction();
            txn.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", "foo", "bar");
        }

        // Transaction API (Bridge)
        public void setTransactionNameBridge() {
            com.newrelic.api.agent.Transaction txn = AgentBridge.getAgent().getTransaction();
            txn.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Custom", "foo", "bar");
        }

        // TracedMethod API
        public void reportAsExternal() {
            com.newrelic.api.agent.TracedMethod tracedMethod = NewRelic.getAgent().getTransaction().getTracedMethod();
            tracedMethod.reportAsExternal(DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build()
            );
        }

        // TracedMethod API (Bridge)
        public void reportAsExternalBridge() {
            com.newrelic.api.agent.TracedMethod tracedMethod = AgentBridge.getAgent().getTransaction().getTracedMethod();
            tracedMethod.reportAsExternal(DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build()
            );
        }

        // Insights API
        public void recordCustomEvent() {
            Map<String, Object> map = new HashMap<>();
            map.put("key", "value");

            NewRelic.getAgent().getInsights().recordCustomEvent("eventType", map);
        }

        // Public API
        public void publicApiCustomParameter() {
            AgentBridge.publicApi.addCustomParameter("key1", 10);
            AgentBridge.publicApi.addCustomParameter("key1", "value1");
        }

        public void publicApiBrowserHeaderFooter() {
            AgentBridge.publicApi.getBrowserTimingFooter();
            AgentBridge.publicApi.getBrowserTimingHeader();
        }

        public void publicApiIgnoreApdex() {
            AgentBridge.publicApi.ignoreApdex();
        }

        public void publicApiIgnoreTransaction() {
            AgentBridge.publicApi.ignoreTransaction();
        }

        public void publicApiNoticeError() {
            AgentBridge.publicApi.noticeError(new Throwable());
            AgentBridge.publicApi.noticeError(new Throwable(), true);
            AgentBridge.publicApi.noticeError(new Throwable(), Collections.<String, Object>emptyMap());
            AgentBridge.publicApi.noticeError(new Throwable(), Collections.<String, Object>emptyMap(), true);
            AgentBridge.publicApi.noticeError("some error");
            AgentBridge.publicApi.noticeError("some error", true);
            AgentBridge.publicApi.noticeError("some error", Collections.<String, Object>emptyMap());
            AgentBridge.publicApi.noticeError("some error", Collections.<String, Object>emptyMap(), true);
        }

        public void publicApiSetAccountName() {
            AgentBridge.publicApi.setAccountName("Account name");
        }

        public void publicApiSetAppServerPort() {
            AgentBridge.publicApi.setAppServerPort(12345);
        }

        public void publicApiSetInstanceName() {
            AgentBridge.publicApi.setInstanceName("Instance name");
        }

        public void publicApiSetProductName() {
            AgentBridge.publicApi.setProductName("Product name");
        }

        public void publicApiSetRequestAndResponse() {
            AgentBridge.publicApi.setRequestAndResponse(new MockHttpRequest(), new MockHttpResponse());
        }

        public void publicApiSetServerInfo() {
            AgentBridge.publicApi.setServerInfo("dispatcherName", "1.0");
        }

        public void publicApiSetTransactionName() {
            AgentBridge.publicApi.setTransactionName("Category", "Name");
        }

        public void publicApiSetUserName() {
            AgentBridge.publicApi.setUserName("User name");
        }

        // Private API
        public void privateApiCustomAttribute() {
            AgentBridge.privateApi.addCustomAttribute("key1", 10);
            AgentBridge.privateApi.addCustomAttribute("key1", Collections.<String, String>emptyMap());
            AgentBridge.privateApi.addCustomAttribute("key1", "value");
        }

        public void privateApiMBeanServer() {
            AgentBridge.privateApi.addMBeanServer(null);
            AgentBridge.privateApi.removeMBeanServer(null);
        }

        public void privateApiSampler() {
            AgentBridge.privateApi.addSampler(new Runnable() {
                @Override
                public void run() {

                }
            }, 10, TimeUnit.SECONDS);
        }

        public void privateApiTracerParameter() {
            AgentBridge.privateApi.addTracerParameter("key1", 10);
            AgentBridge.privateApi.addTracerParameter("key1", Collections.<String, String>emptyMap());
            AgentBridge.privateApi.addTracerParameter("key1", "value");
        }

        public void privateApiReportError() {
            AgentBridge.privateApi.reportException(new Throwable());
            AgentBridge.privateApi.reportHTTPError("message", 500, "/error");
        }

        public void privateApiSetServerInfo() {
            AgentBridge.privateApi.setServerInfo("serverInfo");
            AgentBridge.privateApi.setServerInfo("dispatcherName", "1.0");
        }

        public void privateApiSetAppServerPort() {
            AgentBridge.privateApi.setAppServerPort(12345);
        }

        public void privateApiSetInstanceName() {
            AgentBridge.privateApi.setInstanceName("Instance name");
        }

        // Other
        public void throwExceptionTest() throws IOException {
            ExceptionClass exceptionClass = new ExceptionClass();
            exceptionClass.throwException();
        }

        public static class ExceptionClass {

            public void throwException() throws IOException {
                throw new IOException("Error");
            }

        }
    }

}
