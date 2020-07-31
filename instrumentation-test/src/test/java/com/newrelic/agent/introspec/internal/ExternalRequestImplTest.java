/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

import java.util.Collections;

public class ExternalRequestImplTest {

    private IntrospectorImpl impl = IntrospectorImpl.createIntrospector(Collections.<String, Object>emptyMap());

    @Before
    public void setup() {
        impl.clear();
    }

    @After
    public void afterTest() {
        Transaction.clearTransaction();
    }

    private DefaultTracer getTracer(String metricName, String transactionSegName) {
        MetricNameFormat format = new SimpleMetricNameFormat(metricName, transactionSegName);
        DefaultTracer tracer = new DefaultTracer(Transaction.getTransaction(), new ClassMethodSignature("class",
                "method", "()V"), new Object(), format);
        return tracer;
    }

    @Test
    public void testExternal() {

        ExternalRequestImpl impl1 = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/hostname/library",
                "External/hostname/library/op"));
        Assert.assertNotNull(impl1);
        Assert.assertEquals(1, impl1.getCount());
        Assert.assertEquals("hostname", impl1.getHostname());
        Assert.assertEquals("library", impl1.getLibrary());
        Assert.assertEquals("op", impl1.getOperation());

        ExternalRequestImpl impl2 = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/hostname/library",
                "External/hostname/library/op"));
        Assert.assertTrue(impl1.wasMerged(impl2));
        Assert.assertEquals(2, impl1.getCount());

        ExternalRequestImpl impl = ExternalRequestImpl.checkAndMakeExternal(getTracer(
                "External/hos*&^%$#@tname/libr123$.,?!@#ary",
                "External/hos*&^%$#@tname/libr123$.,?!@#ary/OP$%^ER!@#ION123"));
        Assert.assertNotNull(impl);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("hos*&^%$#@tname", impl.getHostname());
        Assert.assertEquals("libr123$.,?!@#ary", impl.getLibrary());
        Assert.assertEquals("OP$%^ER!@#ION123", impl.getOperation());

        impl = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/hos!@#$%^&*(\\)-+tname/l",
                "External/hos!@#$%^&*(\\)-+tname/l/o"));
        Assert.assertNotNull(impl);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("hos!@#$%^&*(\\)-+tname", impl.getHostname());
        Assert.assertEquals("l", impl.getLibrary());
        Assert.assertEquals("o", impl.getOperation());

        impl = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/host/lib/operation",
                "External/host/lib/operation"));
        Assert.assertNotNull(impl);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("host", impl.getHostname());
        Assert.assertEquals("lib", impl.getLibrary());
        Assert.assertEquals("operation", impl.getOperation());

        impl = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/host/lib////", "External/host/lib////"));
        Assert.assertNotNull(impl1);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("host", impl.getHostname());
        Assert.assertEquals("lib", impl.getLibrary());
        Assert.assertEquals("///", impl.getOperation());

        impl = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/host/", "External/host/"));
        Assert.assertNull(impl);

        // I am okay with this. We are creating the metrics. They should not have accidental spaces.
        impl = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/host/  ", "External/host/lib/  "));
        Assert.assertNotNull(impl);
        Assert.assertEquals("host", impl.getHostname());
        Assert.assertEquals("  ", impl.getOperation());
    }

    /**
     * Operation is optional. If it is not provided it is null.
     */
    @Test
    public void testOptionalOp() {
        ExternalRequestImpl ext = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/hostname/library",
                "External/hostname/library/op"));
        Assert.assertEquals("hostname", ext.getHostname());
        Assert.assertEquals("library", ext.getLibrary());
        Assert.assertEquals("op", ext.getOperation());

        ext = ExternalRequestImpl.checkAndMakeExternal(getTracer("External/hostname/library",
                "External/hostname/library"));
        Assert.assertEquals("hostname", ext.getHostname());
        Assert.assertEquals("library", ext.getLibrary());
        Assert.assertNull(ext.getOperation());
    }
}
