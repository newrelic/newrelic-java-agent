/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.google.common.base.Joiner;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.agent.util.asm.Variables;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Flyweight timers can be created when an instrumented method doesn't participate in transaction traces and is a leaf
 * (no children). Instead of creating a {@link Tracer} object, we create local variables to store the timing information
 * and the details of the traced method.
 *
 * @see Transaction#startFlyweightTracer()
 * @see Transaction#finishFlyweightTracer(TracedMethod, long, long, String, String, String, String, String[])
 */
public class FlyweightTraceMethodVisitor extends AdviceAdapter {

    private static final Type JOINER_TYPE = Type.getType(Joiner.class);

    final Map<Method, Handler> tracedMethodMethodHandlers;

    private final Method method;
    private final int startTimeLocal;
    private final Label startFinallyLabel;
    private final TraceDetails traceDetails;
    private final String className;
    private final int parentTracerLocal;
    private final int metricNameLocal;

    private final int rollupMetricNamesCacheId;

    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);

    public FlyweightTraceMethodVisitor(String className, MethodVisitor mv, int access, String name, String desc,
            TraceDetails trace, Class<?> classBeingRedefined) {
        super(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc);

        this.className = className.replace('/', '.');
        this.method = new Method(name, desc);

        startFinallyLabel = new Label();
        startTimeLocal = newLocal(Type.LONG_TYPE);
        parentTracerLocal = newLocal(BridgeUtils.TRACED_METHOD_TYPE);
        metricNameLocal = newLocal(Type.getType(String.class));
        if (trace.rollupMetricName().length > 0) {
            rollupMetricNamesCacheId = AgentBridge.instrumentation.addToObjectCache(trace.rollupMetricName());
        } else {
            rollupMetricNamesCacheId = -1;
        }

        this.traceDetails = trace;

        tracedMethodMethodHandlers = getTracedMethodMethodHandlers();
    }

    /**
     * Creates handlers responsible for generating the bytecode that replaces invocations of all {@link TracedMethod}
     * methods.
     *
     * @see TracedMethod
     */
    private Map<Method, Handler> getTracedMethodMethodHandlers() {
        Map<Method, Handler> map = new HashMap<>();

        map.put(new Method("getMetricName", "()Ljava/lang/String;"), mv -> mv.loadLocal(metricNameLocal));

        map.put(new Method("setMetricName", "([Ljava/lang/String;)V"), new Handler() {

            @Override
            public void handle(AdviceAdapter mv) {
                // this one is a little tricky because an array is on the stack. We need to concat it to a string
                // Joiner.on("").join(parts);

                mv.checkCast(Type.getType(Object[].class));
                push("");
                mv.invokeStatic(JOINER_TYPE, new Method("on", JOINER_TYPE, new Type[] { Type.getType(String.class) }));

                mv.swap();
                mv.invokeVirtual(JOINER_TYPE, new Method("join", Type.getType(String.class),
                        new Type[] { Type.getType(Object[].class) }));
                mv.storeLocal(metricNameLocal);
            }
        });

        addUnsupportedMethod(map, new Method("nameTransaction", Type.VOID_TYPE,
                new Type[] { Type.getType(TransactionNamePriority.class) }));
        addUnsupportedMethod(map, new Method("setRollupMetricNames", "([Ljava/lang/String;)V"));
        addUnsupportedMethod(map, new Method("addRollupMetricName", "([Ljava/lang/String;)V"));

        addUnsupportedMethod(map, new Method("setMetricNameFormatInfo",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"));
        addUnsupportedMethod(map, new Method("addExclusiveRollupMetricName", "([Ljava/lang/String;)V"));
        addUnsupportedMethod(map, new Method("isMetricProducer", "()Z"));
        addUnsupportedMethod(map, new Method("setCustomMetricPrefix", "(Ljava/lang/String;)V"));
        addUnsupportedMethod(map, new Method("setTrackChildThreads", "(Z)V"));
        addUnsupportedMethod(map, new Method("trackChildThreads", "()Z"));
        addUnsupportedMethod(map, new Method("setTrackCallbackRunnable", "(Z)V"));
        addUnsupportedMethod(map, new Method("isTrackCallbackRunnable", "()Z"));
        addUnsupportedMethod(map, new Method("excludeLeaf", "()V"));
        addUnsupportedMethod(map, new Method("reportAsDatastore",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"));
        addUnsupportedMethod(map, new Method("addOutboundRequestHeaders",
                "(Lcom/newrelic/api/agent/OutboundHeaders;)V"));
        addUnsupportedMethod(map, new Method("readInboundResponseHeaders",
                "(Lcom/newrelic/api/agent/InboundHeaders;)V"));
        addUnsupportedMethod(map, new Method("reportAsExternal",
                "(Lcom/newrelic/agent/bridge/external/ExternalParameters;)V"));
        addUnsupportedMethod(map, new Method("reportAsExternal",
                "(Lcom/newrelic/api/agent/ExternalParameters;)V"));

        addUnsupportedMethod(map, new Method("addCustomAttribute", "(Ljava/lang/String;Ljava/lang/String;)V"));
        addUnsupportedMethod(map, new Method("addCustomAttribute", "(Ljava/lang/String;Ljava/lang/Number;)V"));
        addUnsupportedMethod(map, new Method("addCustomAttribute", "(Ljava/lang/String;Z)V"));
        addUnsupportedMethod(map, new Method("addCustomAttributes", "(Ljava/util/Map;)V"));
        addUnsupportedMethod(map, new Method("addSpanLink", "(Lcom/newrelic/agent/bridge/opentelemetry/SpanLink;)V"));
        addUnsupportedMethod(map, new Method("getSpanLinks", "()Ljava/util/List;"));
        addUnsupportedMethod(map, new Method("addSpanEvent", "(Lcom/newrelic/agent/bridge/opentelemetry/SpanEvent;)V"));
        addUnsupportedMethod(map, new Method("getSpanEvents", "()Ljava/util/List;"));
        map.put(new Method("getParentTracedMethod", "()Lcom/newrelic/agent/bridge/TracedMethod;"), mv -> mv.loadLocal(parentTracerLocal));

        return map;
    }

    private void addUnsupportedMethod(Map<Method, Handler> map, Method method) {
        map.put(method, new UnsupportedHandler(method));
    }

    /**
     * @see Transaction#startFlyweightTracer()
     */
    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();

        // initialize local variables
        push(0l);
        super.storeLocal(startTimeLocal, Type.LONG_TYPE);

        visitInsn(Opcodes.ACONST_NULL);
        super.storeLocal(parentTracerLocal, BridgeUtils.TRACED_METHOD_TYPE);

        visitInsn(Opcodes.ACONST_NULL);
        super.storeLocal(metricNameLocal);

        Label start = new Label();
        Label end = new Label();

        visitLabel(start);
        BridgeUtils.getCurrentTransactionOrNull(this);
        super.ifNull(end);

        // System.nanoTime();
        super.invokeStatic(Type.getType(System.class), new Method("nanoTime", Type.LONG_TYPE, new Type[0]));
        super.storeLocal(startTimeLocal, Type.LONG_TYPE);

        // load the current transaction (this gets rewritten later)
        BridgeUtils.getCurrentTransaction(this);

        Transaction transactionApi = BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true).build();
        transactionApi.startFlyweightTracer();

        super.storeLocal(parentTracerLocal, BridgeUtils.TRACED_METHOD_TYPE);

        String fullMetricName = traceDetails.getFullMetricName(className, method.getName());
        if (fullMetricName == null) {
            fullMetricName = Strings.join(MetricNames.SEGMENT_DELIMITER, MetricNames.JAVA, className, method.getName());
        }
        push(fullMetricName);
        super.storeLocal(metricNameLocal);

        goTo(end);

        Label handler = new Label();
        visitLabel(handler);
        // toss the exception on the stack
        pop();

        visitLabel(end);
        visitTryCatchBlock(start, end, handler, TraceMethodVisitor.THROWABLE_TYPE.getInternalName());
        super.visitLabel(startFinallyLabel);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinallyLabel = new Label();
        super.visitTryCatchBlock(startFinallyLabel, endFinallyLabel, endFinallyLabel,
                FlyweightTraceMethodVisitor.THROWABLE_TYPE.getInternalName());
        super.visitLabel(endFinallyLabel);

        onEveryExit();
        super.visitInsn(ATHROW);

        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            onEveryExit();
        }
    }

    /**
     * This code is injected at every exit instruction, whether a return or an ATHROW.
     *
     * @see Transaction#finishFlyweightTracer(TracedMethod, long, long, String, String, String, String, String[])
     */
    private void onEveryExit() {

        Label skip = super.newLabel();

        super.loadLocal(parentTracerLocal);
        super.ifNull(skip);

        BridgeUtils.getCurrentTransactionOrNull(this);
        super.ifNull(skip);

        BridgeUtils.getCurrentTransaction(this);

        BytecodeGenProxyBuilder<Transaction> builder = BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true);
        Variables loader = builder.getVariables();
        String[] rollupMetricNames;
        if (rollupMetricNamesCacheId >= 0) {
            rollupMetricNames = loader.load(String[].class, new Runnable() {

                @Override
                public void run() {
                    getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, BridgeUtils.INSTRUMENTATION_FIELD_NAME,
                            BridgeUtils.INSTRUMENTATION_TYPE);

                    BytecodeGenProxyBuilder.newBuilder(Instrumentation.class, FlyweightTraceMethodVisitor.this, true).build().getCachedObject(
                            rollupMetricNamesCacheId);
                    checkCast(Type.getType(String[].class));
                }
            });
        } else {
            rollupMetricNames = null;
        }

        // -1 and -2 are identifiers that tell the ApiBuilder's proxy to (-1) load a local variable and (-2) call
        // System.nanoTime when those values are
        // encountered
        long startTime = loader.loadLocal(startTimeLocal, Type.LONG_TYPE, -1L);
        long loadEndTime = loader.load(-2l, () -> invokeStatic(Type.getType(System.class), new Method("nanoTime", Type.LONG_TYPE, new Type[0])));

        Transaction transactionApi = builder.build();

        transactionApi.finishFlyweightTracer(loader.loadLocal(parentTracerLocal, TracedMethod.class), startTime,
                loadEndTime, className, this.method.getName(), this.methodDesc, loader.loadLocal(metricNameLocal,
                        String.class), rollupMetricNames);

        super.visitLabel(skip);
    }

    /**
     * Remove references to any fields in {@link TracedMethod}. This field was removed from our api so we remove
     * references to it to support old weave modules written outside of the agent.
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (owner.equals(BridgeUtils.TRACED_METHOD_TYPE.getInternalName())) {
            // ignore this get static instruction
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    /**
     * Replace invocations of TracerMethod methods with instructions with store and load local variables. Also ignore
     * calls to {@link com.newrelic.api.agent.Agent#getTracedMethod()}.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (BridgeUtils.isAgentType(owner) && BridgeUtils.GET_TRACED_METHOD_METHOD_NAME.equals(name)) {
            // pop the agent off the stack
            pop();
        } else if (BridgeUtils.isTracedMethodType(owner)) {
            Method method = new Method(name, desc);

            Handler handler = tracedMethodMethodHandlers.get(method);

            if (handler != null) {
                handler.handle(this);
            }

        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private interface Handler {
        void handle(AdviceAdapter mv);
    }

    private static class UnsupportedHandler implements Handler {

        private final Method method;

        public UnsupportedHandler(Method method) {
            this.method = method;
        }

        @Override
        public void handle(AdviceAdapter mv) {
            Agent.LOG.log(Level.FINER, "{0}.{1} is unsupported in flyweight tracers",
                    TracedMethod.class.getSimpleName(), method);
        }

    }

}
