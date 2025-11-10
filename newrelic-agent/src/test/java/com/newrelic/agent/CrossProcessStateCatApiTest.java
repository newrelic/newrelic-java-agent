/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.config.CrossProcessConfigImpl;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.DefaultMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.test.marker.RequiresFork;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@Category(RequiresFork.class)
public class CrossProcessStateCatApiTest {
    private final String ENCODING_KEY = "abc";

    private CrossProcessState cps;
    private MockServiceManager serviceManager;

    @Before
    public void setup() throws Exception {
        setUpServiceManager();
    }

    private CrossProcessConfig getCrossProcessConfig(String crossProcessID, String trustedAccounts) {
        ImmutableMap<String, Object> settings = ImmutableMap.<String, Object>builder()
                .put(CrossProcessConfigImpl.ENCODING_KEY, ENCODING_KEY)
                .put(CrossProcessConfigImpl.ENABLED, Boolean.TRUE)
                .put(CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS, trustedAccounts)
                .put(CrossProcessConfigImpl.CROSS_PROCESS_ID, crossProcessID)
                .build();

        return CrossProcessConfigImpl.createCrossProcessConfig(settings);
    }

    @Test
    public void getRequestMetadata() throws ParseException {
        CatTestCustomTracer tracer = trustAppAndGetTracer("1234");
        String reqMetadata = cps.getRequestMetadata();
        Assert.assertNotNull(reqMetadata);

        String result = Obfuscator.deobfuscateNameUsingKey(reqMetadata, ENCODING_KEY);
        JSONParser parser = new JSONParser();
        Map<String, String> data = (Map<String, String>) parser.parse(result);
        assertEquals("1234#878", data.get("NewRelicID"));

        ArrayList<Object> nrTransactionData = (ArrayList<Object>) parser.parse(data.get("NewRelicTransaction"));
        assertEquals(tracer.getTransaction().getGuid(), nrTransactionData.get(0));
        assertEquals(Boolean.FALSE, nrTransactionData.get(1));
        assertEquals(tracer.getTransaction().getCrossProcessTransactionState().getTripId(), nrTransactionData.get(2));
    }

    @Test
    public void processRequestMetadata() {
        String accountID = "123";
        String appID = "456";
        String crossProcessID = accountID + "#" + appID;
        String pathHash = "PathHash1293";
        String guid = "GUI123";

        String obfuscatedMetadata = constructRequestMetadata(accountID, appID, guid, pathHash);

        CatTestCustomTracer tracer = trustAppAndGetTracer(accountID);
        cps.processRequestMetadata(obfuscatedMetadata);

        InboundHeaderState inboundHeaderState = tracer.getTransaction().getInboundHeaderState();
        assertEquals(crossProcessID, inboundHeaderState.getClientCrossProcessId());
        assertEquals(pathHash, inboundHeaderState.getInboundTripId());
        assertEquals(-1, inboundHeaderState.getRequestContentLength());
        assertEquals(guid, inboundHeaderState.getReferrerGuid());
    }

    @Test
    public void getResponseMetadata() throws ParseException {
        String accountID = "9876";
        CatTestCustomTracer tracer = trustAppAndGetTracer(accountID);
        String requestMetadata = constructRequestMetadata(accountID, "123", "myguid", "hash123");

        cps.processRequestMetadata(requestMetadata);

        // Force request parsing.
        tracer.getTransaction().getInboundHeaderState();

        String responseMetaData = cps.getResponseMetadata();

        JSONParser parser = new JSONParser();
        Map<String, String> deobfuscatedResponseMetadata = (Map<String, String>) parser.parse(
                Obfuscator.deobfuscateNameUsingKey(responseMetaData, ENCODING_KEY));
        String newRelicAppData = deobfuscatedResponseMetadata.get("NewRelicAppData");
        ArrayList<Object> appDataValues = (ArrayList<Object>) parser.parse(newRelicAppData);

        assertEquals("9876#878", appDataValues.get(0));
        assertEquals("OtherTransaction/something", appDataValues.get(1));
        assertEquals(0.0, appDataValues.get(2));
        // Ignoring response time for now
        assertEquals(-1L, appDataValues.get(4));
        assertEquals(tracer.getTransaction().getGuid(), appDataValues.get(5));
        assertEquals(Boolean.FALSE, appDataValues.get(6));
    }

    @Test
    public void processResponseMetadata() {
        String accountID = "123";
        String appID = "878";
        String crossProcessID = accountID + "#" + appID;
        String metadata = "{\"NewRelicAppData\":\"[\\\"" + crossProcessID
                + "\\\",\\\"Something\\\\\\/Or\\\\\\/other\\\",0.0,0.0,-1,\\\"GUID191838\\\"]\"}";
        String responseMetadata = Obfuscator.obfuscateNameUsingKey(metadata, ENCODING_KEY);

        CatTestCustomTracer tracer = trustAppAndGetTracer(accountID);
        cps.processResponseMetadata(responseMetadata, null);
        assertEquals("GUID191838", tracer.getAgentAttributes().get("transaction_guid"));
    }

    @Test
    public void processResponseMetadataHostUri() throws URISyntaxException {
        String accountID = "123";
        String appID = "878";
        String crossProcessID = accountID + "#" + appID;
        String metadata = "{\"NewRelicAppData\":\"[\\\"" + crossProcessID
                + "\\\",\\\"TransactionName\\\",0.0,0.0,-1,\\\"GUID191838\\\"]\"}";
        String responseMetadata = Obfuscator.obfuscateNameUsingKey(metadata, ENCODING_KEY);

        CatTestCustomTracer tracer = trustAppAndGetTracer(accountID);
        URI uri = new URI("http://not-a-real.host/?parms=1&ssn=838838878");

        cps.processResponseMetadata(responseMetadata, uri);
        assertEquals("GUID191838", tracer.getAgentAttributes().get("transaction_guid"));

        Set<String> rollupMetricNames = tracer.getRollupMetricNames();
        // CAT response metric
        assertTrue(rollupMetricNames.contains("ExternalApp/" + uri.getHost() + "/" + crossProcessID + "/all"));

        // External metrics
        assertTrue(rollupMetricNames.contains("External/allOther"));
        assertTrue(rollupMetricNames.contains("External/" + uri.getHost() + "/all"));
        assertTrue(rollupMetricNames.contains("External/all"));
    }

    @Test
    public void testCalculatePathHash() {
        assertEquals("3ff723aa", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash(null, null, null)));

        assertEquals("8cb0bd6a", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash(null,
                "WebTransaction/Servlet/ExternalCallServlet", null)));

        assertEquals("8a2e250d", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash(null,
                "WebTransaction/Servlet/ExternalCallServlet", ServiceUtils.hexStringToInt("834f4c33"))));

        assertEquals("a5424da1", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("agent test",
                "WebTransaction/Servlet/ExternalCallServlet", ServiceUtils.hexStringToInt("715f4c55"))));

        assertEquals("9be48a64", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("agent test",
                "WebTransaction/Servlet/CatServlet", ServiceUtils.hexStringToInt("715f4c55"))));

        assertEquals("7267a56b", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("agent test",
                "WebTransaction/Servlet/TestServlet", ServiceUtils.hexStringToInt("a5424da1"))));

        assertEquals("9de8df6b", ServiceUtils.intToHexString(ServiceUtils.calculatePathHash("agent test",
                "WebTransaction/Servlet/BackendServlet", ServiceUtils.hexStringToInt("9be48a64"))));
    }

    @Test
    public void testCatApiResponseExternalMetrics() throws UnsupportedEncodingException {
        Transaction transactionOne = Mockito.mock(Transaction.class);
        Mockito.when(transactionOne.getAgentConfig()).thenReturn(ServiceFactory.getConfigService().getDefaultAgentConfig());
        TransactionActivity txaOne = Mockito.mock(TransactionActivity.class);

        CrossProcessConfig cpsOneConfig = getCrossProcessConfig("CrossProcessId", "");
        setUpTransaction(transactionOne, txaOne, new Object(), new MockDispatcher(), cpsOneConfig, "guid");
        MetricNameFormat nameFormat = new ClassMethodMetricNameFormat(new ClassMethodSignature("className", "methodName", "()V"), null, "");
        CatTestCustomTracer requestTracer = new CatTestCustomTracer(transactionOne, new ClassMethodSignature("className", "methodName",
                "()V"), new Object(), nameFormat, TracerFlags.DISPATCHER);
        when(txaOne.getLastTracer()).thenReturn(requestTracer);
        CrossProcessTransactionStateImpl cpsOne = CrossProcessTransactionStateImpl.create(transactionOne);

        Transaction transactionTwo = Mockito.mock(Transaction.class);
        Mockito.when(transactionTwo.getAgentConfig()).thenReturn(ServiceFactory.getConfigService().getDefaultAgentConfig());
        TransactionActivity txaTwo = Mockito.mock(TransactionActivity.class);

        CrossProcessConfig cpsTwoConfig = getCrossProcessConfig("CrossProcessId", "");
        setUpTransaction(transactionTwo, txaTwo, new Object(), new MockDispatcher(), cpsTwoConfig, "guid");

        InboundHeaderState ihs = Mockito.mock(InboundHeaderState.class);
        when(ihs.isTrustedCatRequest()).thenReturn(true);
        when(transactionTwo.getInboundHeaderState()).thenReturn(ihs);

        CrossProcessTransactionStateImpl cpsTwo = CrossProcessTransactionStateImpl.create(transactionTwo);

        String requestMetadata = cpsOne.getRequestMetadata();
        // Transaction one generates requestMetadata. This metadata is embedded in payload and sent to transaction two.

        // Transaction two gets requestMetadata from payload and provides it to agent.
        cpsTwo.processRequestMetadata(requestMetadata);
        String responseMetadata = cpsTwo.getResponseMetadata();
        // Transaction two generates responseMetadata and sends it to transaction one.

        // Transaction one receives response.
        cpsOne.processResponseMetadata(responseMetadata, null);

        Set<String> rollupMetricNames = requestTracer.getRollupMetricNames();
        assertTrue(rollupMetricNames.contains("External/all"));
        assertTrue(rollupMetricNames.contains("External/allOther"));
        assertTrue(rollupMetricNames.contains("External/Unknown/all"));
    }

    private void setUpTransaction(Transaction tx, TransactionActivity txa, Object lock, Dispatcher dispatcher, CrossProcessConfig config,
            String guid) {
        when(txa.getTransaction()).thenReturn(tx);

        when(tx.getLock()).thenReturn(lock);
        when(tx.getDispatcher()).thenReturn(dispatcher);
        when(tx.getCrossProcessConfig()).thenReturn(config);
        DistributedTracePayloadImpl distributedTracePayload = DistributedTracePayloadImpl.createDistributedTracePayload("", "", "", 0f);
        when(tx.createDistributedTracePayload(guid)).thenReturn(distributedTracePayload);

        TransactionStats transactionStats = Mockito.mock(TransactionStats.class);
        SimpleStatsEngine stats = Mockito.mock(SimpleStatsEngine.class);
        when(stats.getOrCreateResponseTimeStats(anyString())).thenReturn(Mockito.mock(ResponseTimeStats.class));
        when(transactionStats.getUnscopedStats()).thenReturn(stats);
        when(txa.getTransactionStats()).thenReturn(transactionStats);

        when(tx.getTransactionActivity()).thenReturn(txa);

        InboundHeaders headers = Mockito.mock(InboundHeaders.class);
        InboundHeaderState inboundHeaderState = new InboundHeaderState(tx, headers);
        when(tx.getInboundHeaderState()).thenReturn(inboundHeaderState);

        PriorityTransactionName priorityTransactionName = PriorityTransactionName.create("Something/Or/other",
                "category", TransactionNamePriority.FRAMEWORK);
        when(tx.getPriorityTransactionName()).thenReturn(priorityTransactionName);

        TransactionCounts txnCounts = Mockito.mock(TransactionCounts.class);
        when(txnCounts.isOverTracerSegmentLimit()).thenReturn(false);
        when(tx.getTransactionCounts()).thenReturn(txnCounts);
    }

    private void setUpServiceManager() throws Exception {
        ImmutableMap<String, Object> distributedTracingSettings = ImmutableMap.<String, Object>builder()
                .put(DistributedTracingConfig.ENABLED, Boolean.FALSE)
                .build();

        Map<String, Object> configMap = ImmutableMap.<String, Object>builder()
                .put(AgentConfigImpl.APP_NAME, "TransactionAppNamingTest")
                .put(AgentConfigImpl.DISTRIBUTED_TRACING, distributedTracingSettings)
                .build();

        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(configMap);
        serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();
    }

    private CatTestCustomTracer trustAppAndGetTracer(String accountID) {
        ImmutableMap<String, Object> crossProcessSettings = ImmutableMap.<String, Object>builder()
                .put(CrossProcessConfigImpl.ENCODING_KEY, ENCODING_KEY)
                .put(CrossProcessConfigImpl.ENABLED, Boolean.TRUE)
                .put(CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS, accountID)
                .put(CrossProcessConfigImpl.CROSS_PROCESS_ID, accountID + "#878")
                .build();

        // Disable DT for CAT specific tests
        ImmutableMap<String, Object> distributedTracingSettings = ImmutableMap.<String, Object>builder()
                .put(DistributedTracingConfig.ENABLED, Boolean.FALSE)
                .build();

        Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.CROSS_APPLICATION_TRACER, crossProcessSettings);
        settings.put(AgentConfigImpl.APP_NAME, "TransactionAppNamingTest");
        settings.put(AgentConfigImpl.DISTRIBUTED_TRACING, distributedTracingSettings);

        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(settings);
        serviceManager.setConfigService(configService);

        Transaction.clearTransaction();
        Transaction transaction = Transaction.getTransaction(true);

        ClassMethodSignature classMethodSignature = new ClassMethodSignature("className", "methodName", "methodDesc");
        MetricNameFormat metricNameFormat = new DefaultMetricNameFormat(classMethodSignature, "", "something");

        CatTestCustomTracer tracer = new CatTestCustomTracer(transaction, classMethodSignature, null, metricNameFormat,
                TracerFlags.DISPATCHER | TracerFlags.GENERATE_SCOPED_METRIC);
        transaction.getTransactionActivity().tracerStarted(tracer);

        cps = transaction.getCrossProcessState();

        return tracer;
    }

    private String constructRequestMetadata(String accountID, String appID, String guid, String pathHash) {
        String crossProcessID = accountID + "#" + appID;
        String metadata = "{\"NewRelicTransaction\":\"[\\\"" + guid + "\\\", true,\\\"" + pathHash
                + "\\\",\\\"aabbccdd\\\"]\",\"NewRelicID\":\"" + crossProcessID + "\"}";
        return Obfuscator.obfuscateNameUsingKey(metadata, ENCODING_KEY);
    }

    // Exposes rollup metrics and attributes for testing
    private class CatTestCustomTracer extends OtherRootTracer {
        CatTestCustomTracer(Transaction transaction, ClassMethodSignature sig, Object object,
                MetricNameFormat metricNameFormatter, int tracerFlags) {
            super(transaction.getTransactionActivity(), sig, object, metricNameFormatter, tracerFlags, 0l);
        }

        public Set<String> getRollupMetricNames() {
            return super.getRollupMetricNames();
        }

        public Map<String, Object> getAgentAttributes() {
            return super.getAgentAttributes();
        }
    }

}
