/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.agent.util.asm.Variables;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.sql.Connection;
import java.util.Iterator;
import java.util.Map;

public class TraceMethodVisitor extends AdviceAdapter {

    protected final Method method;
    private final int tracerLocal;
    private final Label startFinallyLabel;
    private final TraceDetails traceDetails;
    private final int access;
    private final boolean customTracer;
    private final boolean noticeSql;
    protected final String className;
    private final int signatureId;
    static final Type TRACER_TYPE = Type.getType(ExitTracer.class);
    static final Type THROWABLE_TYPE = Type.getType(Throwable.class);

    /**
     * @see NewRelic#ignoreApdex()
     */
    public static final Method IGNORE_APDEX_METHOD = new Method("ignoreApdex", Type.VOID_TYPE, new Type[0]);

    public TraceMethodVisitor(String className, MethodVisitor mv, int access, String name, String desc,
            TraceDetails trace, boolean customTracer, boolean noticeSql, Class<?> classBeingRedefined) {
        super(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc);

        this.className = className.replace('/', '.');
        this.method = new Method(name, desc);
        this.access = access;
        this.customTracer = customTracer;
        this.noticeSql = noticeSql;

        startFinallyLabel = new Label();
        tracerLocal = newLocal(TraceMethodVisitor.TRACER_TYPE);
        this.traceDetails = trace;

        int signatureId = -1;
        ClassMethodSignature signature = new ClassMethodSignature(this.className.intern(), method.getName().intern(),
                methodDesc.intern());
        if (classBeingRedefined != null) {
            signatureId = ClassMethodSignatures.get().getIndex(signature);
        }
        if (signatureId == -1) {
            signatureId = ClassMethodSignatures.get().add(signature);
        }
        this.signatureId = signatureId;
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        startTracer();
    }

    /**
     * Tracer t = null;<br/>
     * t = AgentBridge.instrumentation.createTracer(...);
     */
    protected void startTracer() {
        visitInsn(Opcodes.ACONST_NULL);
        storeLocal(tracerLocal, TraceMethodVisitor.TRACER_TYPE);

        visitLabel(startFinallyLabel);

        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();

        visitTryCatchBlock(start, end, handler, TraceMethodVisitor.THROWABLE_TYPE.getInternalName());
        visitLabel(start);

        super.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, BridgeUtils.INSTRUMENTATION_FIELD_NAME,
                BridgeUtils.INSTRUMENTATION_TYPE);

        String metricName = traceDetails.getFullMetricName(className, method.getName());

        String tracerFactory = traceDetails.tracerFactoryName();

        BytecodeGenProxyBuilder<Instrumentation> builder = BytecodeGenProxyBuilder.newBuilder(Instrumentation.class,
                this, true);
        Variables loader = builder.getVariables();
        Instrumentation instrumentation = builder.build();

        if (tracerFactory == null) {
            int tracerFlags = getTracerFlags();

            if (noticeSql) {
                instrumentation.createSqlTracer(loader.loadThis(this.access), signatureId, metricName, tracerFlags);
            } else {
                instrumentation.createTracer(loader.loadThis(this.access), signatureId, metricName, tracerFlags);
            }

        } else {

            Object[] loadArgs = loader.load(Object[].class, this::loadArgArray);

            instrumentation.createTracer(loader.loadThis(this.access), signatureId, traceDetails.dispatcher(),
                    metricName, tracerFactory, loadArgs);
        }

        storeLocal(tracerLocal, TraceMethodVisitor.TRACER_TYPE);

        goTo(end);

        visitLabel(handler);
        // toss the exception on the stack
        pop();

        visitLabel(end);
    }

    private int getTracerFlags() {
        int tracerFlags = TracerFlags.GENERATE_SCOPED_METRIC;
        if (traceDetails.dispatcher()) {
            tracerFlags |= TracerFlags.DISPATCHER;
        }
        if (traceDetails.async()) {
            tracerFlags |= TracerFlags.ASYNC;
        }
        if (traceDetails.isLeaf()) {
            tracerFlags |= TracerFlags.LEAF;
        }
        if (!traceDetails.excludeFromTransactionTrace()) {
            tracerFlags |= TracerFlags.TRANSACTION_TRACER_SEGMENT;
        }
        if (customTracer) {
            tracerFlags |= TracerFlags.CUSTOM;
        }
        return tracerFlags;
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinallyLabel = new Label();
        super.visitTryCatchBlock(startFinallyLabel, endFinallyLabel, endFinallyLabel,
                TraceMethodVisitor.THROWABLE_TYPE.getInternalName());
        super.visitLabel(endFinallyLabel);

        onEveryExit(Opcodes.ATHROW);
        super.visitInsn(Opcodes.ATHROW);

        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != Opcodes.ATHROW) {
            onEveryExit(opcode);
        }
    }

    /**
     * This code is injected at every exit instruction, whether a return or an ATHROW.
     *
     * If opcode is a throw:
     *
     * Throwable t = <the exception>;<br/>
     * if (tracer != null) {<br/>
     * tracer.finish(opcode, t);<br/>
     * }<br/>
     *
     * Otherwise:
     *
     * if (tracer != null) {<br/>
     * tracer.finish(opcode, null);<br/>
     * }<br/>
     */
    protected void onEveryExit(int opcode) {
        Label isTracerNullLabel = new Label();

        loadLocal(tracerLocal);
        ifNull(isTracerNullLabel);

        if (Opcodes.ATHROW == opcode) {
            dup();
        }

        // Label tryToFinishTracer = new Label();
        // Label endTryToFinishTracer = new Label();
        // visitLabel(tryToFinishTracer);

        loadLocal(tracerLocal);

        ExitTracer tracer = BytecodeGenProxyBuilder.newBuilder(ExitTracer.class, this, false).build();
        if (Opcodes.ATHROW == opcode) {
            swap();
            tracer.finish(null);
        } else {
            push(opcode);
            visitInsn(Opcodes.ACONST_NULL);
            tracer.finish(0, null);
        }

        visitLabel(isTracerNullLabel);
    }

    /**
     * Replace references to any field on {@link TracedMethod} with a load local instruction that loads the tracer. This
     * field was removed from our api but we still rewrite references to it to support old weave modules written outside
     * of the agent.
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (owner.equals(BridgeUtils.TRACED_METHOD_TYPE.getInternalName())) {
            // instead of loading the field, load the tracer.
            loadTracer();
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    /**
     * Replaces calls to {@link com.newrelic.api.agent.Agent#getTracedMethod()} with a pop and a load local instruction
     * that loads the tracer. Also Replaces calls to:
     * {@link com.newrelic.agent.bridge.datastore.DatastoreMetrics#noticeSql(Connection, String, Object[])}
     * with instructions that store the values of the method call into the SqlTracer for use on tracer exit.
     *
     * @see NoOpTracedMethod#INSTANCE
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (BridgeUtils.isAgentType(owner) && BridgeUtils.GET_TRACED_METHOD_METHOD_NAME.equals(name)) {
            // pop the agent instance off the stack
            pop();
            // instead of invoking the method, load the tracer.
            loadTracer();
        } else if (NoticeSqlVisitor.isNoticeSqlMethod(owner, name, desc)) {
            // Replace calls to DatastoreMetrics.noticeSql() with  instructions to set parameter values on the SqlTracer
            rewriteNoticeSqlCall();
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private void rewriteNoticeSqlCall() {
        Label doWorkLabel = newLabel(), endLabel = newLabel();
        int parameterCount = NoticeSqlVisitor.getSqlTracerSettersCount();

        skipIfNotSqlTracer(parameterCount, doWorkLabel, endLabel);

        visitLabel(doWorkLabel);
        setSqlTracerData();

        visitLabel(endLabel);
    }

    private void skipIfNotSqlTracer(int parameterCount, Label doWorkLabel, Label endLabel) {
        loadTracer();
        instanceOf(Type.getType(SqlTracer.class));

        // If the tracer is an instance of SqlTracer jump to set the parameters on the tracer
        ifZCmp(NE, doWorkLabel);

        // Otherwise, pop the parameters off the stack and go to the end
        for (int i = 0; i < parameterCount; i++) {
            pop();
        }
        goTo(endLabel);
    }

    private void setSqlTracerData() {
        // Iterate in reverse order since parameters will be in reverse order on the stack
        Iterator<Map.Entry<String, Type>> iterator = NoticeSqlVisitor.getSqlTracerSettersInReverseOrder();
        while (iterator.hasNext()) {
            Map.Entry<String, Type> entry = iterator.next();

            // Load the SqlTracer and swap order with the parameter
            // value since we will be calling invoke on the SqlTracer
            loadTracer();
            swap();

            // Call the setter on the SqlTracer with the correct name & method type
            invokeInterface(Type.getType(SqlTracer.class), new Method(entry.getKey(), Type.VOID_TYPE,
                    new Type[] { entry.getValue() }));
        }
    }

    /**
     * Generate instructions to load the tracer. If it's null, load the {@link NoOpTracedMethod#INSTANCE}.
     */
    private void loadTracer() {
        Label isTracerNullLabel = new Label();
        Label end = new Label();

        // load the tracer for a null check
        loadLocal(tracerLocal);
        ifNull(isTracerNullLabel);

        // not null, so load the tracer
        loadLocal(tracerLocal);
        goTo(end);

        visitLabel(isTracerNullLabel);

        // load the no-op instance
        getStatic(Type.getType(NoOpTracedMethod.class), "INSTANCE", Type.getType(TracedMethod.class));

        visitLabel(end);
    }
}