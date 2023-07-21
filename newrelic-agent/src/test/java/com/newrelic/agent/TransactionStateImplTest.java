/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.UltraLightTracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionCounts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.Returns;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionStateImplTest {

    private static final String APP_NAME = "Unit Test";
    private int simpleFlags = getTracerFlags(false, false, false);

    public static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put("apdex_t", 0.5f);
        return map;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        createServiceManager(createConfigMap());
    }

    @Before
    public void before() {
        Transaction.clearTransaction();
    }

    private static void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(map);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);

        configService.start();

        serviceManager.setNormalizationService(new NormalizationServiceImpl());

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);
        statsService.start();
    }

    private Tracer createRootTracer() throws Exception {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
    }

    private Tracer createTracer() throws Exception {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new DefaultTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
    }

    @Test
    public void defaultTransactionState() throws Exception {
        Transaction tx = Transaction.getTransaction();
        assertEquals(tx.getTransactionState().getClass().getName(), TransactionStateImpl.class.getName());
    }

    @Test
    public void getTracer() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));

        tx.getTransactionState().getTracer(tx, tracerFactory, null, null, (Object[]) null);
        assertEquals(rootTracer, tx.getTransactionActivity().getRootTracer());

        Tracer tracer = createTracer();
        tracerFactory = mock(TracerFactory.class, new Returns(tracer));

        tx.getTransactionState().getTracer(tx, tracerFactory, null, null, (Object[]) null);
        assertEquals(tracer, tx.getTransactionActivity().getLastTracer());
    }

    @Test
    public void finish() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));
        tx.getTransactionState().getTracer(tx, tracerFactory, null, null, (Object[]) null);

        Assert.assertTrue(tx.getTransactionState().finish(tx, rootTracer));
    }

    private int getTracerFlags(boolean dispatcher, boolean excludeTT, boolean custom) {
        int flags = TracerFlags.GENERATE_SCOPED_METRIC;
        if (dispatcher) {
            flags |= TracerFlags.DISPATCHER;
        }
        if (custom) {
            flags |= TracerFlags.CUSTOM;
        }
        if (!excludeTT) {
            flags |= TracerFlags.TRANSACTION_TRACER_SEGMENT;
        }

        return flags;
    }

    @Test
    public void tracerMetricName_Null() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));
        tx.getTransactionState().getTracer(tx, tracerFactory, null, null, (Object[]) null);
        assertEquals(rootTracer, tx.getTransactionActivity().getRootTracer());

        ClassMethodSignature sig = new ClassMethodSignature("com.test.Dude", "dude1", "()V");
        Tracer tracer = tx.getTransactionState().getTracer(tx, null, sig, null, simpleFlags);
        assertEquals(tracer, tx.getTransactionActivity().getLastTracer());
        assertEquals("Java/com.test.Dude/dude1", tracer.getMetricName());
    }

    @Test
    public void tracerGeneration_segmentClampInPlace() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TransactionCounts transactionCounts = tx.getTransactionCounts();
        transactionCounts.addTracers(3001);
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));
        ClassMethodSignature sig = new ClassMethodSignature("com.test.Dude", "dude1", "()V");

        tx.getTransactionState().getTracer(tx, tracerFactory, sig, null, (Object[]) null);
        Assert.assertNotNull(tx.getTransactionActivity().getRootTracer());

        Tracer tracer = tx.getTransactionState().getTracer(tx, null, sig, null, simpleFlags);
        assertNotEquals(UltraLightTracer.class, tracer.getClass());
        assertEquals("Java/com.test.Dude/dude1", tracer.getTransactionSegmentName());
        Tracer sqlTracer = tx.getTransactionState().getSqlTracer(tx, null, sig, null, simpleFlags);
        assertEquals(UltraLightTracer.class, sqlTracer.getClass());
        assertEquals("Clamped/com.test.Dude/dude1", sqlTracer.getTransactionSegmentName());
    }

    @Test
    public void tracerGeneration_segmentClampInPlace_afterRoot() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TransactionCounts transactionCounts = tx.getTransactionCounts();
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));
        tx.getTransactionState().getTracer(tx, tracerFactory, null, null, (Object[]) null);
        assertEquals(rootTracer, tx.getTransactionActivity().getRootTracer());

        transactionCounts.addTracers(3001);

        ClassMethodSignature sig = new ClassMethodSignature("com.test.Dude", "dude1", "()V");

        Tracer tracer = tx.getTransactionState().getTracer(tx, null, sig, null, simpleFlags);
        assertNotEquals(UltraLightTracer.class, tracer.getClass());
        assertEquals("Java/com.test.Dude/dude1", tracer.getTransactionSegmentName());
        Tracer sqlTracer = tx.getTransactionState().getSqlTracer(tx, null, sig, null, simpleFlags);
        assertEquals(UltraLightTracer.class, sqlTracer.getClass());
        assertEquals("Clamped/com.test.Dude/dude1", sqlTracer.getTransactionSegmentName());
    }

    @Test
    public void tracerMetricName_NullCustom() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));
        tx.getTransactionState().getTracer(tx, tracerFactory, new ClassMethodSignature("com.newrelic.test.Foo", "makeItSo", "()"), null, (Object[]) null);
        assertEquals(rootTracer, tx.getTransactionActivity().getRootTracer());

        ClassMethodSignature sig = new ClassMethodSignature("com.test.Dude", "dude2", "()V");
        Tracer tracer = tx.getTransactionState().getTracer(tx, null, sig, null, getTracerFlags(false, false, true));
        assertEquals(tracer, tx.getTransactionActivity().getLastTracer());
        assertEquals("Custom/com.test.Dude/dude2", tracer.getMetricName());
    }

    @Test
    public void tracerMetricName_NullInvocationTarget() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));
        tx.getTransactionState().getTracer(tx, tracerFactory, null, null, (Object[]) null);
        assertEquals(rootTracer, tx.getTransactionActivity().getRootTracer());

        ClassMethodSignature sig = new ClassMethodSignature("com.test.Dude", "dude3", "()V");
        Tracer tracer = tx.getTransactionState().getTracer(tx, null, sig, "Test/${className}/dude3",
                getTracerFlags(false, false, true));
        assertEquals(tracer, tx.getTransactionActivity().getLastTracer());
        assertEquals("Test/com.test.Dude/dude3", tracer.getMetricName());
    }

    @Test
    public void tracerMetricName() throws Exception {
        Tracer rootTracer = createRootTracer();
        Transaction tx = Transaction.getTransaction();
        TracerFactory tracerFactory = mock(TracerFactory.class, new Returns(rootTracer));
        tx.getTransactionState().getTracer(tx, tracerFactory, null, null, (Object[]) null);
        assertEquals(rootTracer, tx.getTransactionActivity().getRootTracer());

        ClassMethodSignature sig = new ClassMethodSignature("com.test.Dude", "dude4", "()V");
        Tracer tracer = tx.getTransactionState().getTracer(tx, new Object(), sig, "Test/${className}/dude4",
                TracerFlags.CUSTOM);
        assertEquals(tracer, tx.getTransactionActivity().getLastTracer());
        assertEquals("Test/java.lang.Object/dude4", tracer.getMetricName());
    }

    @Test
    public void getTracer_withTxnIgnoreOrTracerStartLocked_returnsNull() {
        Transaction mockTxn = mock(Transaction.class);
        TracerFactory mockFactory = mock(TracerFactory.class);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        ClassMethodSignature signature = new ClassMethodSignature("class", "method", "desc");
        TransactionState txnState = new TransactionStateImpl();

        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxn.isIgnore()).thenReturn(true);

        assertNull(txnState.getTracer(mockTxn, mockFactory, signature, new Object()));
        assertNull(txnState.getTracer(mockTxn, new Object(), signature, "metricName", 0));

        when(mockTxn.isIgnore()).thenReturn(false);
        when(mockTxnActivity.isTracerStartLocked()).thenReturn(true);

        assertNull(txnState.getTracer(mockTxn, mockFactory, signature, new Object()));
        assertNull(txnState.getTracer(mockTxn, new Object(), signature, "metricName", 0));
    }

    @Test
    public void getTracer_withDispatcherFlag_returnsOtherRootTracer() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        ClassMethodSignature signature = new ClassMethodSignature("class", "method", "desc");
        TransactionState txnState = new TransactionStateImpl();

        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxn.isIgnore()).thenReturn(false);
        when(mockTxn.getTransactionActivity().tracerStarted(any())).thenReturn(mock(Tracer.class));
        when(mockTxn.getRootTracer()).thenReturn(mock(Tracer.class));
        when(mockTxnActivity.isTracerStartLocked()).thenReturn(false);

        Tracer result = txnState.getTracer(mockTxn, new Object(), signature, "metricName", TracerFlags.DISPATCHER);
        assertNotNull(result);
    }

    @Test
    public void getSqlTracer_withTxnIgnoreOrTracerStartLocked_returnsNull() {
        Transaction mockTxn = mock(Transaction.class);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        ClassMethodSignature signature = new ClassMethodSignature("class", "method", "desc");
        TransactionState txnState = new TransactionStateImpl();

        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxn.isIgnore()).thenReturn(true);

        assertNull(txnState.getSqlTracer(mockTxn, new Object(), signature, "metricName", 0));

        when(mockTxn.isIgnore()).thenReturn(false);
        when(mockTxnActivity.isTracerStartLocked()).thenReturn(true);

        assertNull(txnState.getSqlTracer(mockTxn, new Object(), signature, "metricName", 0));
    }

    @Test
    public void getSqlTracer_withDispatcherFlag_returnsOtherRootTracer() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        ClassMethodSignature signature = new ClassMethodSignature("class", "method", "desc");
        TransactionState txnState = new TransactionStateImpl();

        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxn.isIgnore()).thenReturn(false);
        when(mockTxn.getTransactionActivity().tracerStarted(any())).thenReturn(mock(Tracer.class));
        when(mockTxn.getRootTracer()).thenReturn(mock(Tracer.class));
        when(mockTxnActivity.isTracerStartLocked()).thenReturn(false);

        Tracer result = txnState.getSqlTracer(mockTxn, new Object(), signature, "metricName", TracerFlags.DISPATCHER);
        assertNotNull(result);

        result = txnState.getSqlTracer(mockTxn, new Object(), signature, "metricName", 0);
        assertNotNull(result);
    }

    @Test
    public void noOp_methods() {
        TransactionState txnState = new TransactionStateImpl();
        txnState.getRootTracer();
        txnState.resume();
        txnState.suspend();
        txnState.complete();
        txnState.suspend();
        txnState.suspendRootTracer();
    }
}
