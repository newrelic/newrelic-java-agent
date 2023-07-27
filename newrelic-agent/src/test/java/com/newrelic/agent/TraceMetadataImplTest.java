package com.newrelic.agent;

import com.newrelic.agent.tracers.Tracer;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class TraceMetadataImplTest {
    @Test
    public void getTraceId_withTxnAndDistributedTracingEnabled_returnsTraceId() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(true);
        when(mockTxn.getSpanProxy().getOrCreateTraceId()).thenReturn("traceid");

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertEquals("traceid", TraceMetadataImpl.INSTANCE.getTraceId());
        }
    }

    @Test
    public void getTraceId_withTxnAndDistributedTracingDisabled_returnsEmptyString() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(false);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertEquals("", TraceMetadataImpl.INSTANCE.getTraceId());
        }
    }

    @Test
    public void getTraceId_withoutTxn_returnsEmptyString() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(null);
            assertEquals("", TraceMetadataImpl.INSTANCE.getTraceId());
        }
    }

    @Test
    public void getSpanId_withTxnAndDistributedTracingEnabled_returnsSpanId() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        Tracer mockTracer = mock(Tracer.class);
        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxnActivity.getLastTracer()).thenReturn(mockTracer);
        when(mockTracer.getGuid()).thenReturn("guid");

        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(true);
        when(mockTxn.getSpanProxy().getOrCreateTraceId()).thenReturn("traceid");

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertEquals("guid", TraceMetadataImpl.INSTANCE.getSpanId());
        }
    }

    @Test
    public void getSpanId_withTxnAndDistributedTracingEnabledAndNullSpanId_returnsEmptyString() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        Tracer mockTracer = mock(Tracer.class);
        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxnActivity.getLastTracer()).thenReturn(mockTracer);

        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(true);
        when(mockTxn.getSpanProxy().getOrCreateTraceId()).thenReturn("traceid");

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertEquals("", TraceMetadataImpl.INSTANCE.getSpanId());
        }
    }

    @Test
    public void getSpanId_withTxnAndDistributedTracingEnabledAndNullTracer_returnsEmptyString() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxnActivity.getLastTracer()).thenReturn(null);

        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(true);
        when(mockTxn.getSpanProxy().getOrCreateTraceId()).thenReturn("traceid");

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertEquals("", TraceMetadataImpl.INSTANCE.getSpanId());
        }
    }

    @Test
    public void getSpanId_withTxnAndDistributedTracingDisabled_returnsEmptyString() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        Tracer mockTracer = mock(Tracer.class);
        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxnActivity.getLastTracer()).thenReturn(mockTracer);

        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(false);
        when(mockTxn.getSpanProxy().getOrCreateTraceId()).thenReturn("traceid");

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertEquals("", TraceMetadataImpl.INSTANCE.getSpanId());
        }
    }

    @Test
    public void isSampled_withTxnAndDistributedTracingEnabled_returnsTxnSampledFlag() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(true);
        when(mockTxn.sampled()).thenReturn(true);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertTrue(TraceMetadataImpl.INSTANCE.isSampled());
        }
    }

    @Test
    public void isSampled_withTxnAndDistributedTracingDisabled_returnsFalse() {
        Transaction mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        when(mockTxn.getAgentConfig().getDistributedTracingConfig().isEnabled()).thenReturn(false);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertFalse(TraceMetadataImpl.INSTANCE.isSampled());
        }
    }

    @Test
    public void isSampled_withoutTxn_returnsFalse() {

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(null);
            assertFalse(TraceMetadataImpl.INSTANCE.isSampled());
        }
    }
}
