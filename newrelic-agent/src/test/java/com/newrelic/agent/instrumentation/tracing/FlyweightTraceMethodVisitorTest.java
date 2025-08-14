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
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.bridge.TracedMethod;

public class FlyweightTraceMethodVisitorTest {

    @Test
    public void verifyTracedMethodStitching() {
        Set<Method> excludes = ImmutableSet.of(
                // this method is overridden in the bridge with a different signature. ignore it
                new Method("getParentTracedMethod",
                "()Lcom/newrelic/api/agent/TracedMethod;"),
                // these methods have default implementations which are fine because flyweight tracers are leaves
                new Method("getTraceId",
                        "()Ljava/lang/String;"),
                new Method("getSpanId",
                        "()Ljava/lang/String;"));

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
    @Test
    public void testVisitFieldInsn() {
        TraceDetails trace = TraceDetailsBuilder.newBuilder().build();
        Class<?> classBeingRedefined = Object.class;

        FlyweightTraceMethodVisitor ftmv = new FlyweightTraceMethodVisitor(
                "com/example/MyClass", null, Opcodes.ACC_PUBLIC, "myMethod", "(I)V", trace, classBeingRedefined
        );

        ftmv.visitFieldInsn(Opcodes.GETSTATIC, BridgeUtils.TRACED_METHOD_TYPE.getInternalName(), "fieldName", "I");


        ftmv.visitFieldInsn(Opcodes.GETSTATIC, "com/example/OtherClass", "fieldName", "I");

    }


}
