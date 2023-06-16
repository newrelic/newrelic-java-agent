package com.newrelic.agent.bridge.datastore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.MessageFormat;

import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TracedMethod;

public class LegacyDatabaseMetricsTest {

    Transaction mockTransaction;
    TracedMethod mockTracedMethod;

    @Before
    public void setUp() {
        mockTransaction = Mockito.mock(Transaction.class);
        mockTracedMethod = Mockito.mock(TracedMethod.class);
    }

    @Test
    public void doDatabaseMetrics_shouldCall_setupMethods(){
        LegacyDatabaseMetrics.doDatabaseMetrics(mockTransaction, mockTracedMethod, "lazy", "dog");

        Mockito.verify(mockTracedMethod).setMetricName(MessageFormat.format(LegacyDatabaseMetrics.STATEMENT, "lazy", "dog"));
        Mockito.verify(mockTracedMethod).addRollupMetricName(MessageFormat.format(LegacyDatabaseMetrics.STATEMENT, "lazy", "dog"));
        Mockito.verify(mockTracedMethod).addRollupMetricName(MessageFormat.format(LegacyDatabaseMetrics.OPERATION, "dog"));
        Mockito.verify(mockTracedMethod).addRollupMetricName(LegacyDatabaseMetrics.ALL);
    }

    @Test
    public void doDatabaseMetrics_shouldHandle_webTransactions(){
        Mockito.when(mockTransaction.isWebTransaction()).thenReturn(true);
        LegacyDatabaseMetrics.doDatabaseMetrics(mockTransaction, mockTracedMethod, "brown", "cow");
        Mockito.verify(mockTracedMethod).addRollupMetricName(LegacyDatabaseMetrics.ALL_WEB);
    }

    @Test
    public void doDatabaseMetrics_shouldHandle_nonWebTransactions(){
        Mockito.when(mockTransaction.isWebTransaction()).thenReturn(false);
        LegacyDatabaseMetrics.doDatabaseMetrics(mockTransaction, mockTracedMethod, "jumping", "mouse");
        Mockito.verify(mockTracedMethod).addRollupMetricName(LegacyDatabaseMetrics.ALL_OTHER);
    }
}