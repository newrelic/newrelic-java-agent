package com.newrelic.agent.tracers;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

public class NewExternalComponentTracerTest {

    private String UNKNOWN_HOST = "UnknownHost"; //declared private static in AbstractExternalComponentTracer
    private Transaction mockTransaction;
    private TransactionActivity mockTransactionActivity;
    @Before
    public void setup(){
        this.mockTransaction = Mockito.mock(Transaction.class);
        this.mockTransactionActivity = Mockito.mock(TransactionActivity.class);

        Mockito.when(mockTransaction.getTransactionActivity()).thenReturn(mockTransactionActivity);
    }

    @Test
    public void constructor_shouldHandle_metricNameFormat(){
        ExternalComponentTracer tracer = createTracerByMetricNameFormat();
        //does anything break?
        assertEquals("tambourine", tracer.getHost());
    }
    @Test
    public void finish_shouldHandle_UnknownHostException(){
        ExternalComponentTracer tracer = createTracer();
        Throwable mockException = Mockito.mock(UnknownHostException.class);

        tracer.finish(mockException);

        assertEquals(UNKNOWN_HOST, tracer.getHost());
    }

    @Test
    public void finish_shouldHandle_opCode(){
        ExternalComponentTracer tracer = createTracer();
        tracer.finish(123, "kangaroo");

    }

    @Test
    public void testDoRecordMetrics_isWebTransaction_false(){
        ExternalComponentTracer tracer = createTracer();
        Mockito.when(mockTransactionActivity.getTransaction()).thenReturn(mockTransaction);

        TransactionStats mockTransactionStats = Mockito.mock(TransactionStats.class);
        SimpleStatsEngine mockSimpleStatsEngine = Mockito.mock(SimpleStatsEngine.class);
        ResponseTimeStats mockResponseTimeStats = Mockito.mock(ResponseTimeStats.class);

        Mockito.when(mockTransactionStats.getUnscopedStats()).thenReturn(mockSimpleStatsEngine);
        Mockito.when(mockSimpleStatsEngine.getOrCreateResponseTimeStats(anyString())).thenReturn(mockResponseTimeStats);
        Mockito.when(mockTransaction.isWebTransaction()).thenReturn(false);

        tracer.doRecordMetrics(mockTransactionStats);

        //three batches of stats should be sent
        Mockito.verify(mockTransactionStats, times(3)).getUnscopedStats();

        //should send three rounds of stats with the expected names
        Mockito.verify(mockSimpleStatsEngine).getOrCreateResponseTimeStats(MetricNames.EXTERNAL_ALL);
        Mockito.verify(mockSimpleStatsEngine).getOrCreateResponseTimeStats(MetricNames.OTHER_TRANSACTION_EXTERNAL_ALL);
        String hostRollupMetricName = Strings.join('/', MetricNames.EXTERNAL_PATH, "tambourine", "all");
        Mockito.verify(mockSimpleStatsEngine).getOrCreateResponseTimeStats(hostRollupMetricName);
    }

    @Test
    public void testDoRecordMetrics_isWebTransaction_true(){
        ExternalComponentTracer tracer = createTracer();
        Mockito.when(mockTransactionActivity.getTransaction()).thenReturn(mockTransaction);

        TransactionStats mockTransactionStats = Mockito.mock(TransactionStats.class);
        SimpleStatsEngine mockSimpleStatsEngine = Mockito.mock(SimpleStatsEngine.class);
        ResponseTimeStats mockResponseTimeStats = Mockito.mock(ResponseTimeStats.class);

        Mockito.when(mockTransactionStats.getUnscopedStats()).thenReturn(mockSimpleStatsEngine);
        Mockito.when(mockSimpleStatsEngine.getOrCreateResponseTimeStats(anyString())).thenReturn(mockResponseTimeStats);
        Mockito.when(mockTransaction.isWebTransaction()).thenReturn(true);

        tracer.doRecordMetrics(mockTransactionStats);

        //three batches of stats should be sent
        Mockito.verify(mockTransactionStats, times(3)).getUnscopedStats();

        //should send three rounds of stats with the expected names
        Mockito.verify(mockSimpleStatsEngine).getOrCreateResponseTimeStats(MetricNames.EXTERNAL_ALL);
        Mockito.verify(mockSimpleStatsEngine).getOrCreateResponseTimeStats(MetricNames.WEB_TRANSACTION_EXTERNAL_ALL);
        String hostRollupMetricName = Strings.join('/', MetricNames.EXTERNAL_PATH, "tambourine", "all");
        Mockito.verify(mockSimpleStatsEngine).getOrCreateResponseTimeStats(hostRollupMetricName);
    }

    private ExternalComponentTracer createTracer(){
        ClassMethodSignature sig = new ClassMethodSignature("fake.class", "fakeMethod", "this is a fake method");
        Object object = new Object();
        String host = "tambourine";
        String library = "fizzy";
        String uri = "definitely/fake/uri";
        String[] operations = {"a", "b", "c"};
        return new ExternalComponentTracer(mockTransaction, sig, object, host, library, uri, operations);
    }

    private ExternalComponentTracer createTracerByMetricNameFormat(){
        ClassMethodSignature sig = new ClassMethodSignature("fake.class", "fakeMethod", "this is a fake method");
        Object object = new Object();
        String host = "tambourine";
        MetricNameFormat metricNameFormat = Mockito.mock(MetricNameFormat.class);
        return new ExternalComponentTracer(mockTransaction, sig, object, host, metricNameFormat);
    }

}