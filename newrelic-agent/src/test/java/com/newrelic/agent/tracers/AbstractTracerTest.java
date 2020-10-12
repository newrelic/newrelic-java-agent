/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.google.common.base.Strings;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.tracers.DefaultTracer.DEFAULT_TRACER_FLAGS;
import static com.newrelic.agent.tracers.TracerFlags.TRANSACTION_TRACER_SEGMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractTracerTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @Test
    public void testTrackChildThreadsDefault() {
        AbstractTracer tracer = createTxnAndTracer();
        assertTrue(tracer.trackChildThreads());
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testTrackChildThreadsTrue() {
        AbstractTracer tracer = createTxnAndTracer();
        tracer.setTrackChildThreads(true);
        assertTrue(tracer.trackChildThreads());
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testTrackChildThreadsFalse() {
        AbstractTracer tracer = createTxnAndTracer();
        tracer.setTrackChildThreads(false);
        assertFalse(tracer.trackChildThreads());
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testTrackChildThreadsParent() {
        AbstractTracer tracer = createTxnAndTracer();
        tracer.setTrackChildThreads(false);
        assertFalse(tracer.trackChildThreads());

        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer kid = new DefaultTracer(tx, sig, this);
        tx.getTransactionActivity().tracerStarted(kid);

        assertFalse(kid.trackChildThreads());

        kid.finish(Opcodes.RETURN, null);
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testTrackCallbackRunnableDefault() {
        AbstractTracer tracer = createTxnAndTracer();
        assertFalse(tracer.isTrackCallbackRunnable());
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testTrackCallbackRunnableTrue() {
        AbstractTracer tracer = createTxnAndTracer();
        tracer.setTrackCallbackRunnable(true);
        assertTrue(tracer.isTrackCallbackRunnable());
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testTrackCallbackRunnableFalse() {
        AbstractTracer tracer = createTxnAndTracer();
        tracer.setTrackCallbackRunnable(false);
        assertFalse(tracer.isTrackCallbackRunnable());
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testEnabledTrackCallbackRunnableParent() {
        AbstractTracer tracer = createTxnAndTracer();
        tracer.setTrackCallbackRunnable(true);
        assertTrue(tracer.isTrackCallbackRunnable());

        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer kid = new DefaultTracer(tx, sig, this);
        tx.getTransactionActivity().tracerStarted(kid);

        assertTrue(kid.isTrackCallbackRunnable());

        kid.finish(Opcodes.RETURN, null);
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testDisabledTrackCallbackRunnableParent() {
        AbstractTracer tracer = createTxnAndTracer();
        tracer.setTrackCallbackRunnable(false);
        assertFalse(tracer.isTrackCallbackRunnable());

        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer kid = new DefaultTracer(tx, sig, this);
        tx.getTransactionActivity().tracerStarted(kid);

        assertFalse(kid.isTrackCallbackRunnable());

        kid.finish(Opcodes.RETURN, null);
        tracer.finish(Opcodes.RETURN, null);
    }

    @Test
    public void testGetParentTracerWithSpan() throws Exception {
        AbstractTracer childTracer = createTxnAndTracer(true, false);
        Tracer parentTracer = childTracer.getParentTracer();

        Tracer result = AbstractTracer.getParentTracerWithSpan(parentTracer);
        assertSame(result, parentTracer);

        Tracer result2 = AbstractTracer.getParentTracerWithSpan(childTracer);
        assertSame(parentTracer, result2);
    }

    @Test
    public void testGetParentTracerWithSpanTraverses() throws Exception {
        AbstractTracer childTracer = createTxnAndTracer(true, true);
        Tracer result = AbstractTracer.getParentTracerWithSpan(childTracer);
        assertSame(childTracer, result);
    }

    @Test
    public void testAddCustomAttributeNoCheckLimit() {
        // setup
        AbstractTracer target = createTxnAndTracer();

        // execution
        target.addCustomAttribute("hamburger", "bun");
        target.addCustomAttribute("numToppings", 4);
        target.addCustomAttribute("cheese", true);
        Map<String, Object> condiments = new HashMap<>();
        condiments.put("ketchup", null);
        condiments.put("mayonnaise", true);
        target.addCustomAttributes(condiments);
        target.addCustomAttribute(null, "Keys cant be null");
        // assertions
        assertEquals(4, target.getCustomAttributes().size());
        assertEquals("bun", target.getCustomAttribute("hamburger"));
        assertEquals(4, target.getCustomAttribute("numToppings"));
        assertTrue((Boolean) target.getCustomAttribute("cheese"));
        assertTrue((Boolean) target.getCustomAttribute("mayonnaise"));
    }

    @Test
    public void testAddCustomAttributeCheckLimit() {
        // setup
        Transaction txn = mock(Transaction.class, RETURNS_DEEP_STUBS);
        TransactionActivity txnActivity = mock(TransactionActivity.class);

        when(txn.getTransactionCounts().isOverTracerSegmentLimit()).thenReturn(true);
        when(txn.getTransactionActivity()).thenReturn(txnActivity);
        when(txnActivity.getTransaction()).thenReturn(txn);

        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer target = new OtherRootTracer(txn, sig, this, new SimpleMetricNameFormat("test"));

        // execution
        target.addCustomAttribute("hamburger", "bun");
        target.addCustomAttribute("numToppings", 4);
        target.addCustomAttribute("cheese", true);

        // assertions
        assertNull(target.getCustomAttribute("hamburger"));
        assertNull(target.getCustomAttribute("numToppings"));
        assertNull(target.getCustomAttribute("cheese"));
    }

    @Test
    public void testAddCustomAttributesNullMap() {
        AbstractTracer target = createTxnAndTracer();
        Map<String, Object> expected = Collections.emptyMap();

        target.addCustomAttributes(null);

        assertEquals(expected.size(), target.getCustomAttributes().size());
    }

    @Test
    public void testAttributeIsTruncated() {
        // setup
        AbstractTracer target = createTxnAndTracer();
        String beforeTruncate = Strings.padEnd("", 300, 'e');
        String afterTruncate = Strings.padEnd("", 255, 'e');

        // execution

        target.addCustomAttribute("truncate me", beforeTruncate);

        // assertions
        assertEquals(1, target.getCustomAttributes().size());
        assertEquals(afterTruncate, target.getCustomAttribute("truncate me"));

    }

    private AbstractTracer createTxnAndTracer() {
        return createTxnAndTracer(false, false);
    }

    private AbstractTracer createTxnAndTracer(boolean addChild, boolean childIsTransactionSegment) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        DefaultTracer rootTracer = new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("test"));
        tx.getTransactionActivity().tracerStarted(rootTracer);
        if (addChild) {
            ClassMethodSignature sig2 = new ClassMethodSignature(getClass().getName(), "dudechild", "()V");
            int flags = DEFAULT_TRACER_FLAGS;
            if (childIsTransactionSegment) {
                flags |= TRANSACTION_TRACER_SEGMENT;
            } else {
                flags &= (~TRANSACTION_TRACER_SEGMENT);
            }
            DefaultTracer childTracer = new DefaultTracer(tx.getTransactionActivity(), sig2, this, new SimpleMetricNameFormat("test"), flags);
            childTracer.setParentTracer(rootTracer);
            tx.getTransactionActivity().tracerStarted(childTracer);
            return childTracer;
        }
        return rootTracer;
    }
}
