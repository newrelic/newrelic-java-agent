/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.commons.Method;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.bridge.TracedMethod;

public class FlyweightTraceMethodVisitorTest {

    @Test
    public void verifyTracedMethodStitching() {
        // these methods are overridden in the bridge with a different signature. ignore them
        Set<Method> excludes = ImmutableSet.of(new Method("getParentTracedMethod",
                "()Lcom/newrelic/api/agent/TracedMethod;"));

        TraceDetails trace = TraceDetailsBuilder.newBuilder().build();
        FlyweightTraceMethodVisitor mv = new FlyweightTraceMethodVisitor("", null, 0, "go", "()V", trace, null);

        Map<Method, ?> map = mv.tracedMethodMethodHandlers;
        java.lang.reflect.Method[] methods = TracedMethod.class.getMethods();
        for (java.lang.reflect.Method m : methods) {
            Method method = Method.getMethod(m);
            if (!map.containsKey(method) && !excludes.contains(method)) {
                Assert.fail("There is no method handler for TracedMethod." + method);
            }
        }
    }
}
