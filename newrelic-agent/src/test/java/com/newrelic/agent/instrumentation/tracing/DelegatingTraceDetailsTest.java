package com.newrelic.agent.instrumentation.tracing;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class DelegatingTraceDetailsTest {

    private TraceDetails delegate;
    private DelegatingTraceDetails delegatingTraceDetails;

    @Before
    public void setUp() {
        delegate = mock(TraceDetails.class);
        delegatingTraceDetails = new DelegatingTraceDetails(delegate);
    }

    @Test
    public void testMetricName() {
        when(delegate.metricName()).thenReturn("testMetric");
        assertEquals("testMetric", delegatingTraceDetails.metricName());
        verify(delegate).metricName();
    }

    @Test
    public void testDispatcher() {
        when(delegate.dispatcher()).thenReturn(true);
        assertEquals(true, delegatingTraceDetails.dispatcher());
        verify(delegate).dispatcher();
    }

    @Test
    public void testAsync() {
        when(delegate.async()).thenReturn(true);
        assertEquals(true, delegatingTraceDetails.async());
        verify(delegate).async();
    }

    @Test
    public void testTracerFactoryName() {
        when(delegate.tracerFactoryName()).thenReturn("testTracerFactoryName");
        assertEquals("testTracerFactoryName", delegatingTraceDetails.tracerFactoryName());
        verify(delegate).tracerFactoryName();
    }

    @Test
    public void testExcludeFromTransactionTrace() {
        when(delegate.excludeFromTransactionTrace()).thenReturn(true);
        assertEquals(true, delegatingTraceDetails.excludeFromTransactionTrace());
        verify(delegate).excludeFromTransactionTrace();
    }

    @Test
    public void testMetricPrefix() {
        when(delegate.metricPrefix()).thenReturn("testMetricPrefix");
        assertEquals("testMetricPrefix", delegatingTraceDetails.metricPrefix());
        verify(delegate).metricPrefix();
    }

    @Test
    public void testGetFullMetricName() {
        when(delegate.getFullMetricName("testClassName", "testMethodName")).thenReturn("testFullMetricName");
        assertEquals("testFullMetricName", delegatingTraceDetails.getFullMetricName("testClassName", "testMethodName"));
        verify(delegate).getFullMetricName("testClassName", "testMethodName");
    }

    @Test
    public void testIgnoreTransaction() {
        when(delegate.ignoreTransaction()).thenReturn(true);
        assertEquals(true, delegatingTraceDetails.ignoreTransaction());
        verify(delegate).ignoreTransaction();
    }

    @Test
    public void testIsCustom() {
        when(delegate.isCustom()).thenReturn(true);
        assertEquals(true, delegatingTraceDetails.isCustom());
        verify(delegate).isCustom();
    }

    @Test
    public void testIsLeaf() {
        when(delegate.isLeaf()).thenReturn(true);
        assertEquals(true, delegatingTraceDetails.isLeaf());
        verify(delegate).isLeaf();
    }

    @Test
    public void testIsWebTransaction() {
        when(delegate.isWebTransaction()).thenReturn(true);
        assertEquals(true, delegatingTraceDetails.isWebTransaction());
        verify(delegate).isWebTransaction();
    }



    @Test
    public void testInstrumentationType() {
        List<InstrumentationType> instrumentationTypes = Collections.singletonList(InstrumentationType.RemoteCustomXml);
        when(delegate.instrumentationTypes()).thenReturn(instrumentationTypes);
        when(delegate.instrumentationTypes()).thenReturn(Collections.singletonList(InstrumentationType.RemoteCustomXml));
        assertEquals(instrumentationTypes.get(0), delegatingTraceDetails.instrumentationTypes().get(0));
        verify(delegate).instrumentationTypes();
    }




    @Test
    public void testGetParameterAttributeNames() {
        List<ParameterAttributeName> parameterAttributeNames = Collections.singletonList(
                new ParameterAttributeName(0, "param",mock(MethodMatcher.class) )
        );
        when(delegate.getParameterAttributeNames()).thenReturn(parameterAttributeNames);
        assertEquals(parameterAttributeNames, delegatingTraceDetails.getParameterAttributeNames());
        verify(delegate).getParameterAttributeNames();
    }

    @Test
    public void testRollupMetricName() {
        String[] rollupMetricNames = {"testRollupMetricName"};
        when(delegate.rollupMetricName()).thenReturn(rollupMetricNames);
        assertEquals(rollupMetricNames, delegatingTraceDetails.rollupMetricName());
        verify(delegate).rollupMetricName();
    }

    @Test
    public void testInstrumentationSourceNames() {
        List<String> instrumentationSourceNames = Collections.singletonList("testInstrumentationSourceName");
        when(delegate.instrumentationSourceNames()).thenReturn(instrumentationSourceNames);
        assertEquals(instrumentationSourceNames, delegatingTraceDetails.instrumentationSourceNames());
        verify(delegate).instrumentationSourceNames();
    }


}
