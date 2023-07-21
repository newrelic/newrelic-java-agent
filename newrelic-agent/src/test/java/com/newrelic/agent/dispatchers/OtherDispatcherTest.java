package com.newrelic.agent.dispatchers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OtherDispatcherTest {
    Transaction mockTxn;
    MetricNameFormat mockNameFormat;
    TransactionStats mockTxnStats;

    @Before
    public void setup() {
        mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        mockNameFormat = mock(MetricNameFormat.class);
        mockTxnStats = mock(TransactionStats.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void transactionFinished_withProperTxnName_returnsCorrectStatsMap() {
        String [] TXN_KEYS = {"CPU/OtherTransaction/Txn", "OtherTransaction/Txn", "OtherTransaction/all", "OtherTransactionTotalTime",
                "CPU/OTherTransaction", "OtherTransactionTotalTime/Txn", "ApdexOther/Transaction/Txn", "ApdexOther"};
        OtherDispatcher otherDispatcher = new OtherDispatcher(mockTxn, mockNameFormat);
        TransactionStats txnStats = new TransactionStats();
        when(mockTxn.getTransactionTimer().getResponseTimeInNanos()).thenReturn(5L, 10L, 15L);
        when(mockTxn.getTransactionTimer().getTotalSumTimeInNanos()).thenReturn(20L, 25L, 30L);
        when(mockTxn.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME)).thenReturn(20L, 25L, 30L);
        when(mockTxn.getAgentConfig().isApdexTSet("OtherTransaction/Txn")).thenReturn(true);
        when(mockTxn.isErrorReportableAndNotIgnored()).thenReturn(true);
        when(mockTxn.isErrorNotExpected()).thenReturn(true);

        otherDispatcher.transactionFinished("OtherTransaction/Txn", txnStats);

        assertEquals(TXN_KEYS.length, txnStats.getUnscopedStats().getStatsMap().size());
        for (String key : TXN_KEYS) {
            assertTrue(txnStats.getUnscopedStats().getStatsMap().containsKey(key));
            assertTrue(txnStats.getUnscopedStats().getStatsMap().get(key).hasData());
        }

        assertEquals(0, txnStats.getScopedStats().getSize());
    }

    @Test
    public void transactionFinished_withIncorrectTxnName_returnsCorrectStatsMap() {
        String [] TXN_KEYS = {"CPU/foo/Txn", "OtherTransaction/all", "OtherTransactionTotalTime", "CPU/OTherTransaction", "foo/Txn"};
        OtherDispatcher otherDispatcher = new OtherDispatcher(mockTxn, mockNameFormat);
        TransactionStats txnStats = new TransactionStats();
        when(mockTxn.getTransactionTimer().getResponseTimeInNanos()).thenReturn(5L, 10L, 15L);
        when(mockTxn.getTransactionTimer().getTotalSumTimeInNanos()).thenReturn(20L, 25L, 30L);
        when(mockTxn.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME)).thenReturn(20L, 25L, 30L);
        when(mockTxn.getAgentConfig().isApdexTSet("foo/Txn")).thenReturn(true);
        when(mockTxn.isErrorReportableAndNotIgnored()).thenReturn(false);
        when(mockTxn.isErrorNotExpected()).thenReturn(false);

        otherDispatcher.transactionFinished("foo/Txn", txnStats);

        assertEquals(TXN_KEYS.length, txnStats.getUnscopedStats().getStatsMap().size());
        for (String key : TXN_KEYS) {
            assertTrue(txnStats.getUnscopedStats().getStatsMap().containsKey(key));
            assertTrue(txnStats.getUnscopedStats().getStatsMap().get(key).hasData());
        }

        assertEquals(0, txnStats.getScopedStats().getSize());
    }

    @Test
    public void getResponse_returnsNull() {
        assertNull(new OtherDispatcher(mockTxn, mockNameFormat).getResponse());
    }

    @Test
    public void getCookieValue_returnsNull() {
        assertNull(new OtherDispatcher(mockTxn, mockNameFormat).getCookieValue("foo"));

    }

    @Test
    public void getHeader_returnsNull() {
        assertNull(new OtherDispatcher(mockTxn, mockNameFormat).getHeader("foo"));
    }

    @Test
    public void coverNoOps() {
        OtherDispatcher otherDispatcher = new OtherDispatcher(mockTxn, mockNameFormat);
        otherDispatcher.setRequest(mock(Request.class));
        otherDispatcher.setResponse(mock(Response.class));
        otherDispatcher.transactionActivityWithResponseFinished();
    }
}
