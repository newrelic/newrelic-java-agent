/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.ServerProp;
import com.newrelic.agent.config.SqlTraceConfig;
import com.newrelic.agent.config.SqlTraceConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SlowQueryInfoTest {

    @Test
    public void testTxnUrl() {
        setupServiceManager(new HashMap<String, Object>());

        Transaction transaction = Transaction.getTransaction();

        SqlTraceConfig sqlTraceConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getSqlTraceConfig();

        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setUri("http://jvm.agent.uri");
        transaction.setDispatcher(dispatcher);

        TransactionData data = new TransactionData(transaction, 100);
        Tracer tracer = new DefaultTracer(transaction, new ClassMethodSignature("ClassName", "methodName",
                "methodDesc"), null, null, TracerFlags.DISPATCHER);
        SlowQueryInfo slowQueryInfo = new SlowQueryInfo(data, tracer, "select * from person", "select ? from ?", sqlTraceConfig);

        SqlTrace sqlTrace = slowQueryInfo.asSqlTrace();
        assertEquals("http://jvm.agent.uri", sqlTrace.getUri());
    }

    @Test
    public void testEmptyTxnUrlRequestUriDisabled() {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("exclude", "request.*");

        Map<String, Object> settings = new HashMap<>();
        settings.put("attributes", attributeMap);

        setupServiceManager(settings);

        SqlTraceConfig sqlTraceConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getSqlTraceConfig();

        Transaction transaction = Transaction.getTransaction();
        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setUri("http://jvm.agent.uri");
        transaction.setDispatcher(dispatcher);
        TransactionData data = new TransactionData(transaction, 100);
        Tracer tracer = new DefaultTracer(transaction, new ClassMethodSignature("ClassName", "methodName",
                "methodDesc"), null, null, TracerFlags.DISPATCHER);


        SlowQueryInfo slowQueryInfo = new SlowQueryInfo(data, tracer, "select * from person", "select ? from ?", sqlTraceConfig);
        SqlTrace sqlTrace = slowQueryInfo.asSqlTrace();
        assertEquals(null, sqlTrace.getUri());
    }

    @Test
    public void testLongerSqlId() {
        setupServiceManager(new HashMap<String, Object>());

        Transaction transaction = Transaction.getTransaction();

        HashMap<String, Object> sqlMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(!SqlTraceConfigImpl.DEFAULT_USE_LONGER_SQL_ID);
        sqlMap.put(SqlTraceConfigImpl.USE_LONGER_SQL_ID, serverProp);
        SqlTraceConfig sqlTraceConfig = SqlTraceConfigImpl.createSqlTraceConfig(sqlMap);

        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setUri("http://jvm.agent.uri");
        transaction.setDispatcher(dispatcher);

        String obfuscatedQuery = "select ? from ?";

        TransactionData data = new TransactionData(transaction, 100);
        Tracer tracer = new DefaultTracer(transaction, new ClassMethodSignature("ClassName", "methodName",
                "methodDesc"), null, null, TracerFlags.DISPATCHER);
        SlowQueryInfo slowQueryInfo = new SlowQueryInfo(data, tracer, "select * from person", obfuscatedQuery, sqlTraceConfig);
        SqlTrace sqlTrace = slowQueryInfo.asSqlTrace();

        long hashedQuery = (long) obfuscatedQuery.hashCode();
        long longerHash = SlowQueryInfo.createLongerHashCode(hashedQuery);

        assertEquals(longerHash, sqlTrace.getId());
    }

    @Test
    public void testStandardSqlId() {
        setupServiceManager(new HashMap<String, Object>());

        Transaction transaction = Transaction.getTransaction();

        HashMap<String, Object> sqlMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(SqlTraceConfigImpl.DEFAULT_USE_LONGER_SQL_ID);
        sqlMap.put(SqlTraceConfigImpl.USE_LONGER_SQL_ID, serverProp);
        SqlTraceConfig sqlTraceConfig = SqlTraceConfigImpl.createSqlTraceConfig(sqlMap);

        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setUri("http://jvm.agent.uri");
        transaction.setDispatcher(dispatcher);

        String obfuscatedQuery = "select ? from ?";

        TransactionData data = new TransactionData(transaction, 100);
        Tracer tracer = new DefaultTracer(transaction, new ClassMethodSignature("ClassName", "methodName",
                "methodDesc"), null, null, TracerFlags.DISPATCHER);
        SlowQueryInfo slowQueryInfo = new SlowQueryInfo(data, tracer, "select * from person", obfuscatedQuery, sqlTraceConfig);
        SqlTrace sqlTrace = slowQueryInfo.asSqlTrace();

        long hashedObfuscatedQuery = (long) obfuscatedQuery.hashCode();

        assertEquals(hashedObfuscatedQuery, sqlTrace.getId());
    }

    private boolean longHashCorrectLength(String hashString) {
        long hash = Long.parseLong(hashString);
        if(hash < 0) {
            hash = hash * -1;
        }
        int hashLength = Long.toString(hash).length();
        if(hashLength == 9 || hashLength <= 0 || hashLength > 18) {
            return false;
        }
        return true;
    }

    @Test
    public void consistentLongSqlIds() {
        String queryOne = "SELECT name from USERS WHERE id = 123";
        assertEquals("Long sql Id is not consistent", SlowQueryInfo.createLongerHashCode(queryOne.hashCode()),
                SlowQueryInfo.createLongerHashCode(queryOne.hashCode()));

        String queryTwo = "";
        assertEquals("Long sql Id is not consistent", SlowQueryInfo.createLongerHashCode(queryTwo.hashCode()),
                SlowQueryInfo.createLongerHashCode(queryTwo.hashCode()));

        assertEquals("Long sql Id is not consistent", SlowQueryInfo.createLongerHashCode(-123),
                SlowQueryInfo.createLongerHashCode(-123));

        assertEquals("Long sql Id is not consistent", SlowQueryInfo.createLongerHashCode(0),
                SlowQueryInfo.createLongerHashCode(0));
    }

    @Test
    public void testGenerateLongSqlId() {
        long longHashNegativeInput = SlowQueryInfo.createLongerHashCode(-124567878);
        long emptyStringInput = SlowQueryInfo.createLongerHashCode("".hashCode());
        long longHashSmallInput = SlowQueryInfo.createLongerHashCode(123);
        long longHashStandardInput = SlowQueryInfo.createLongerHashCode(123456789);
        long longHashLongInput = SlowQueryInfo.createLongerHashCode(12345678999L);
        long longHashSmallNegativeInput = SlowQueryInfo.createLongerHashCode(-1);
        long longHashLongNegativeInput = SlowQueryInfo.createLongerHashCode(-123455768967L);

        assertTrue(longHashCorrectLength(String.valueOf(longHashNegativeInput)));
        assertTrue(longHashCorrectLength(String.valueOf(emptyStringInput)));
        assertTrue(longHashCorrectLength(String.valueOf(longHashSmallInput)));
        assertTrue(longHashCorrectLength(String.valueOf(longHashStandardInput)));
        assertTrue(longHashCorrectLength(String.valueOf(longHashLongInput)));
        assertTrue(longHashCorrectLength(String.valueOf(longHashSmallNegativeInput)));
        assertTrue(longHashCorrectLength(String.valueOf(longHashLongNegativeInput)));
    }

    @Test
    public void testDTAttributes() {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> dtMap = new HashMap<>();
        dtMap.put("enabled", true);
        settings.put("distributed_tracing", dtMap);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        settings.put("span_events", spanConfig);
        setupServiceManager(settings);

        DistributedTraceServiceImpl dts = (DistributedTraceServiceImpl) ServiceFactory.getServiceManager().getDistributedTraceService();

        Map<String, Object> configMap = ImmutableMap.<String, Object>builder().put("distributed_tracing",
                ImmutableMap.builder().put("primary_application_id", "1app23")
                        .put("trusted_account_key", "1tak23")
                        .put("account_id", "1acct234").build())
                .build();
        dts.connected(null, AgentConfigFactory.createAgentConfig(configMap, null, null));

        Transaction.clearTransaction();
        Transaction transaction = Transaction.getTransaction();
        transaction.getTransactionActivity().tracerStarted(new OtherRootTracer(transaction, new ClassMethodSignature("class", "method", "desc"), null, new SimpleMetricNameFormat("test")));
        String inboundPayload =
                "{\n" +
                        "  \"v\": [\n" +
                        "    0,\n" +
                        "    2\n" +
                        "  ],\n" +
                        "  \"d\": {\n" +
                        "    \"ty\": \"App\",\n" +
                        "    \"ac\": \"1acct789\",\n" +
                        "    \"ap\": \"1app23\",\n" +
                        "    \"id\": \"27856f70d3d314b7\",\n" +
                        "    \"tr\": \"3221bf09aa0bcf0d\",\n" +
                        "    \"tk\": \"1tak23\",\n" +
                        "    \"pr\": 1.0002,\n" +
                        "    \"sa\": true,\n" +
                        "    \"tx\": \"123456\",\n" +
                        "    \"ti\": 1482959525577\n" +
                        "  }\n" +
                        "}";
        transaction.acceptDistributedTracePayload(inboundPayload);
        TransactionData data = new TransactionData(transaction, 100);
        Tracer tracer = new DefaultTracer(transaction, new ClassMethodSignature("ClassName", "methodName",
                "methodDesc"), null, null, TracerFlags.DISPATCHER);
        String obfuscatedQuery = "select ? from ?";

        HashMap<String, Object> sqlMap = new HashMap<>();
        ServerProp serverProp = ServerProp.createPropObject(SqlTraceConfigImpl.DEFAULT_USE_LONGER_SQL_ID);
        sqlMap.put(SqlTraceConfigImpl.USE_LONGER_SQL_ID, serverProp);
        SqlTraceConfig sqlTraceConfig = SqlTraceConfigImpl.createSqlTraceConfig(sqlMap);

        SlowQueryInfo slowQueryInfo = new SlowQueryInfo(data, tracer, "select * from person", obfuscatedQuery, sqlTraceConfig);

        assertNotNull(transaction.getSpanProxy().getInboundDistributedTracePayload());
        Map<String, Object> parameters = slowQueryInfo.getParameters();

        assertEquals("App", parameters.get("parent.type"));
        assertEquals("1app23", parameters.get("parent.app"));
        assertEquals("1acct789", parameters.get("parent.account"));
        assertNotNull(parameters.get("parent.transportType"));
        assertNotNull(parameters.get("parent.transportDuration"));
        assertNotNull(parameters.get("guid"));
        assertEquals("3221bf09aa0bcf0d", parameters.get("traceId"));
        assertNotNull(parameters.get("priority"));
        assertEquals(true, parameters.get("sampled"));
    }


    private void setupServiceManager(Map<String, Object> settings) {
        MockServiceManager serviceManager = new MockServiceManager();
        settings.put("app_name", "Unit Test");
        serviceManager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(settings));
        serviceManager.setTransactionTraceService(Mockito.mock(TransactionTraceService.class));
        serviceManager.setTransactionService(Mockito.mock(TransactionService.class));
        serviceManager.setTransactionEventsService(Mockito.mock(TransactionEventsService.class));
        serviceManager.setHarvestService(Mockito.mock(HarvestService.class));
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.setDistributedTraceService(new DistributedTraceServiceImpl());

        serviceManager.setAttributesService(new AttributesService());
        serviceManager.setRPMServiceManager(new MockRPMServiceManager());
    }
}
