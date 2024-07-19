package com.newrelic.agent.tracers;

import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionSegment;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodExitTracerNoSkipTest {
    ClassMethodSignature classMethodSignature;
    Transaction mockTxn;
    TransactionActivity mockTxnActivity;
    Tracer mockTracer;

    @Before
    public void setup() {
        classMethodSignature = new ClassMethodSignature("java.lang.String", "toString", "desc");
        mockTxn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        mockTxnActivity = mock(TransactionActivity.class, RETURNS_DEEP_STUBS);
        mockTracer = mock(Tracer.class);

        when(mockTxn.getTransactionActivity().getLastTracer()).thenReturn(mockTracer);
        when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
        when(mockTxnActivity.getLastTracer()).thenReturn(mockTracer);
        when(mockTxnActivity.getTransaction()).thenReturn(mockTxn);

        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null)));
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Test
    public void constructors_assignParentTracerAndMethodSignature() {
        MethodExitTracerNoSkip instance1 = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        MethodExitTracerNoSkip instance2 = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxnActivity);

        assertEquals(mockTracer, instance1.getParentTracer());
        assertEquals(mockTracer, instance2.getParentTracer());
        assertEquals(classMethodSignature, instance1.getClassMethodSignature());
        assertEquals(classMethodSignature, instance2.getClassMethodSignature());
    }

    @Test
    public void getChildCount_returnsZero() {
        MethodExitTracerNoSkip instance = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        assertEquals(0, instance.getChildCount());
    }

    @Test
    public void finish_invokesTracerFinishedOnTxn() {
        MethodExitTracerNoSkip instance = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        instance.finish(999, new Object());

        verify(mockTxnActivity, times(1)).tracerFinished(instance, 999);
    }

    @Test
    public void timingMethods_allReturnZero() {
        MethodExitTracerNoSkip instance = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        assertEquals(0, instance.getDurationInMilliseconds());
        assertEquals(0, instance.getRunningDurationInNanos());
        assertEquals(0, instance.getDuration());
        assertEquals(0, instance.getExclusiveDuration());
        assertEquals(0, instance.getStartTime());
        assertEquals(0, instance.getStartTimeInMilliseconds());
        assertEquals(0, instance.getEndTime());
        assertEquals(0, instance.getEndTimeInMilliseconds());
        assertNull(instance.getMetricName());
        assertNull(instance.getTransactionSegmentName());
        assertNull(instance.getTransactionSegmentUri());
    }
    @Test
    public void fetchingAgentAttributes_returnsNull() {
        MethodExitTracerNoSkip instance = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        assertEquals(0, instance.getAgentAttributes().size());
        assertNull(instance.getAgentAttribute("foo"));
    }

    @Test
    public void fetchingAgentAttributeNamesMarkedForSpans_returnsEmptySet() {
        MethodExitTracerNoSkip instance = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        assertEquals(0, instance.getAgentAttributeNamesForSpans().size());
        assertFalse(instance.getAgentAttributeNamesForSpans().contains("foo"));
    }

    @Test
    public void getTransactionSegment_returns_TxnSegment() {
        MethodExitTracerNoSkip instance = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        TransactionSegment txnSegment = instance.getTransactionSegment(mock(TransactionTracerConfig.class), mock(SqlObfuscator.class),
                123456, mock(TransactionSegment.class));
        assertNotNull(txnSegment);
        assertEquals("com.newrelic.agent.tracers.MethodExitTracerNoSkipTest$TestMethodExitTracerNoSkip*", txnSegment.getMetricName());
    }

    @Test
    public void noopMethods() {
        MethodExitTracerNoSkip instance = new TestMethodExitTracerNoSkip(classMethodSignature, mockTxn);
        instance.setAgentAttribute("foo", "bar");
        instance.isTransactionSegment();
        instance.removeAgentAttribute("foo");
        instance.isMetricProducer();
        instance.isParent();
        instance.removeTransactionSegment();
        instance.setMetricName("foo");
        instance.setMetricNameFormatInfo("foo", "bar", "baz");
        instance.getGuid();
    }

    private static class TestMethodExitTracerNoSkip extends MethodExitTracerNoSkip {

        public TestMethodExitTracerNoSkip(ClassMethodSignature signature, Transaction transaction) {
            super(signature, transaction);
        }

        public TestMethodExitTracerNoSkip(ClassMethodSignature signature, TransactionActivity activity) {
            super(signature, activity);
        }

        @Override
        protected void doFinish(int opcode, Object returnValue) {
            //noop
        }
    }
}
