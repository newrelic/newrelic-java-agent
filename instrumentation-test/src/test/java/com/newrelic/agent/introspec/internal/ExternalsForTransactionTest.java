/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class ExternalsForTransactionTest {

    private IntrospectorImpl impl = IntrospectorImpl.createIntrospector(Collections.<String, Object>emptyMap());

    @Before
    public void setup() {
        impl.clear();
    }

    @After
    public void afterTest() {
        Transaction.clearTransaction();
    }

    @Test
    public void testHasExternals() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Unscoped/other").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Another/another").recordResponseTime(10, TimeUnit.MILLISECONDS);

        Assert.assertFalse(ExternalsForTransaction.hasExternals(stats));

        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ExternalsForTransaction.hasExternals(stats));
    }

    @Test
    public void testcheckExternalsOne() {
        // web
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        Collection<Tracer> tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myOp"));
        Collection<ExternalRequestImpl> actual = ExternalsForTransaction.checkExternals(true, stats, tracers);
        Assert.assertEquals(1, actual.size());
        ExternalRequestImpl impl = actual.iterator().next();
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("myhost", impl.getHostname());
        Assert.assertEquals("mylib", impl.getLibrary());
        Assert.assertEquals("myOp", impl.getOperation());

        // other
        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allOther").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myOp"));

        actual = ExternalsForTransaction.checkExternals(false, stats, tracers);
        Assert.assertEquals(1, actual.size());
        impl = actual.iterator().next();
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("myhost", impl.getHostname());
        Assert.assertEquals("mylib", impl.getLibrary());
        Assert.assertEquals("myOp", impl.getOperation());
    }

    @Test
    public void testcheckExternalsTwo() {
        // web
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/secondhost/all").recordResponseTime(5,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/secondhost/secondlib").recordResponseTime(5,
                TimeUnit.MILLISECONDS);

        Collection<Tracer> tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myOp"));
        tracers.add(createTracer("External/secondhost/secondlib", "External/secondhost/secondlib/secondOp"));

        Collection<ExternalRequestImpl> actual = ExternalsForTransaction.checkExternals(true, stats, tracers);
        Assert.assertEquals(2, actual.size());
        Iterator<ExternalRequestImpl> it = actual.iterator();
        while (it.hasNext()) {
            ExternalRequestImpl impl = it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("External/myhost/mylib")) {
                Assert.assertEquals("myhost", impl.getHostname());
                Assert.assertEquals("mylib", impl.getLibrary());
                Assert.assertEquals("myOp", impl.getOperation());
            } else {
                Assert.assertEquals("secondhost", impl.getHostname());
                Assert.assertEquals("secondlib", impl.getLibrary());
                Assert.assertEquals("secondOp", impl.getOperation());
            }
        }

        // other
        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allOther").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allOther").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/secondhost/all").recordResponseTime(5,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/secondhost/secondlib").recordResponseTime(5,
                TimeUnit.MILLISECONDS);

        tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myOp"));
        tracers.add(createTracer("External/secondhost/secondlib", "External/secondhost/secondlib/secondOp"));

        actual = ExternalsForTransaction.checkExternals(false, stats, tracers);
        Assert.assertEquals(2, actual.size());
        it = actual.iterator();
        while (it.hasNext()) {
            ExternalRequestImpl impl = it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("External/myhost/mylib")) {
                Assert.assertEquals("myhost", impl.getHostname());
                Assert.assertEquals("mylib", impl.getLibrary());
                Assert.assertEquals("myOp", impl.getOperation());
            } else {
                Assert.assertEquals("secondhost", impl.getHostname());
                Assert.assertEquals("secondlib", impl.getLibrary());
                Assert.assertEquals("secondOp", impl.getOperation());
            }
        }
    }

    @Test
    public void testcheckExternalsSameHost() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(5,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/secondlib").recordResponseTime(5,
                TimeUnit.MILLISECONDS);

        Collection<Tracer> tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myOp"));
        tracers.add(createTracer("External/myhost/secondlib", "External/myhost/secondlib/thirdOp"));

        Collection<ExternalRequestImpl> actual = ExternalsForTransaction.checkExternals(true, stats, tracers);
        Assert.assertEquals(2, actual.size());
        Iterator<ExternalRequestImpl> it = actual.iterator();
        while (it.hasNext()) {
            ExternalRequestImpl impl = it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("External/myhost/mylib")) {
                Assert.assertEquals("myhost", impl.getHostname());
                Assert.assertEquals("mylib", impl.getLibrary());
                Assert.assertEquals("myOp", impl.getOperation());
            } else {
                Assert.assertEquals("myhost", impl.getHostname());
                Assert.assertEquals("secondlib", impl.getLibrary());
                Assert.assertEquals("thirdOp", impl.getOperation());
            }
        }
    }

    @Test
    public void testAddExternalsOneCall() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(5,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/secondlib").recordResponseTime(5,
                TimeUnit.MILLISECONDS);

        Collection<Tracer> tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myop"));
        tracers.add(createTracer("External/myhost/secondlib", "External/myhost/secondlib/seconop"));

        ExternalsForTransaction exts = new ExternalsForTransaction();
        exts.addExternals(true, stats, tracers);
        Collection<ExternalRequest> actual = exts.getExternals();
        Assert.assertEquals(2, actual.size());
        Iterator<ExternalRequest> it = actual.iterator();
        while (it.hasNext()) {
            ExternalRequestImpl impl = (ExternalRequestImpl) it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("External/myhost/mylib")) {
                Assert.assertEquals("myhost", impl.getHostname());
                Assert.assertEquals("mylib", impl.getLibrary());
                Assert.assertEquals("myop", impl.getOperation());
            } else {
                Assert.assertEquals("myhost", impl.getHostname());
                Assert.assertEquals("secondlib", impl.getLibrary());
                Assert.assertEquals("seconop", impl.getOperation());
            }
        }
    }

    @Test
    public void testAddExternalsTwoCallsSameOperation() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        Collection<Tracer> tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myop"));
        ExternalsForTransaction exts = new ExternalsForTransaction();
        exts.addExternals(true, stats, tracers);

        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/myop"));

        exts.addExternals(true, stats, tracers);

        Collection<ExternalRequest> actual = exts.getExternals();
        Assert.assertEquals(1, actual.size());
        Iterator<ExternalRequest> it = actual.iterator();
        while (it.hasNext()) {
            ExternalRequestImpl impl = (ExternalRequestImpl) it.next();
            Assert.assertEquals(2, impl.getCount());
            Assert.assertEquals("myhost", impl.getHostname());
            Assert.assertEquals("mylib", impl.getLibrary());
            Assert.assertEquals("myop", impl.getOperation());
        }
    }

    @Test
    public void testcheckExternalsTwoSameMetric() {
        // web
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/all").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/allWeb").recordResponseTime(5, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("External/myhost/all").recordResponseTime(5,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("External/myhost/mylib").recordResponseTime(5,
                TimeUnit.MILLISECONDS);

        Collection<Tracer> tracers = new ArrayList<>();
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/firstOp"));
        tracers.add(createTracer("External/myhost/mylib", "External/myhost/mylib/secondOp"));
        Collection<ExternalRequestImpl> actual = ExternalsForTransaction.checkExternals(true, stats, tracers);
        Assert.assertEquals(2, actual.size());
        Iterator<ExternalRequestImpl> it = actual.iterator();
        while (it.hasNext()) {
            ExternalRequestImpl impl = it.next();
            Assert.assertEquals(1, impl.getCount());
            Assert.assertEquals("myhost", impl.getHostname());
            Assert.assertEquals("mylib", impl.getLibrary());
            boolean firstOp = impl.getOperation().contains("firstOp");
            boolean secondOp = impl.getOperation().contains("secondOp");
            // one should be true and one should be false
            Assert.assertFalse(firstOp && secondOp);
            Assert.assertTrue(firstOp || secondOp);
        }

    }

    private DefaultTracer createTracer(String metricName, String txSegName) {
        Transaction tx = Transaction.getTransaction();
        SimpleMetricNameFormat format = new SimpleMetricNameFormat(metricName, txSegName);
        DefaultTracer tracer = new DefaultTracer(tx, new ClassMethodSignature("myClass", "myMethod", "()V"), this,
                format);
        return tracer;
    }
}
