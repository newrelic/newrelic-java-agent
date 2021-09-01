/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.TraceInformation;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class TraceClassVisitor extends ClassVisitor {
    private final String className;

    private final InstrumentationContext instrumentationContext;
    private final TraceInformation traceInfo;
    private final Set<Method> noticeSqlMethods;
    private final Set<Method> tracedMethods = new HashSet<>();

    public TraceClassVisitor(ClassVisitor cv, String className, InstrumentationContext context, Set<Method> noticeSqlMethods) {
        super(WeaveUtils.ASM_API_LEVEL, cv);

        this.className = className;
        this.instrumentationContext = context;
        this.traceInfo = context.getTraceInformation();
        this.noticeSqlMethods = noticeSqlMethods;
    }

    /**
     * Log what we did.
     */
    @Override
    public void visitEnd() {
        super.visitEnd();

        if (!traceInfo.getTraceAnnotations().isEmpty()) {
            Agent.LOG.finer("Traced " + className + " methods " + tracedMethods);
            if (tracedMethods.size() != traceInfo.getTraceAnnotations().size()) {
                Set<Method> expected = new HashSet<>(traceInfo.getTraceAnnotations().keySet());
                expected.removeAll(tracedMethods);

                Agent.LOG.finer("While tracing " + className + " the following methods were not traced: " + expected);
            }
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        // skip bridge methods. Tracing will occur on impl method.
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            /*
             * Some versions of the Agent (mid 3.20.* -> low 3.30.*) had a bug here that caused us to trace bridge
             * methods, resulting in double counting of @Trace'd methods. This bug only affected late versions of JDK7
             * and all versions of JDK8 due to annotations being automatically copied by the JVM over to bridge methods.
             */
            return mv;
        }

        Method method = new Method(name, desc);

        if (traceInfo.getIgnoreTransactionMethods().contains(method)) {
            instrumentationContext.markAsModified();
            return new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {

                @Override
                protected void onMethodEnter() {
                    BridgeUtils.getCurrentTransaction(this);

                    BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true).build().ignore();
                }

            };
        } else {
            final TraceDetails trace = traceInfo.getTraceAnnotations().get(method);

            if (null != trace) {
                tracedMethods.add(method);
                PointCut pointCut = instrumentationContext.getOldStylePointCut(method);
                if (pointCut == null) {
                    boolean custom = trace.isCustom();
                    boolean noticeSql = noticeSqlMethods.contains(method);

                    if (trace.excludeFromTransactionTrace() && trace.isLeaf()) {
                        mv = new FlyweightTraceMethodVisitor(className, mv, access, name, desc, trace,
                                instrumentationContext.getClassBeingRedefined());
                    } else {
                        mv = new TraceMethodVisitor(className, mv, access, name, desc, trace, custom, noticeSql,
                                instrumentationContext.getClassBeingRedefined());

                        if (!trace.getParameterAttributeNames().isEmpty()) {
                            for (ParameterAttributeName attr : trace.getParameterAttributeNames()) {
                                final ParameterAttributeName param = attr;
                                if (param.getMethodMatcher().matches(access, name, desc, null)) {
                                    try {
                                        final Type type = method.getArgumentTypes()[param.getIndex()];
                                        if (type.getSort() == Type.ARRAY) {
                                            Agent.LOG.log(Level.FINE, "Unable to record an attribute value for {0}.{1} because it is an array",
                                                    className, method);
                                        } else {
                                            mv = new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {

                                                @Override
                                                protected void onMethodEnter() {
                                                    super.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE,
                                                            BridgeUtils.PUBLIC_API_FIELD_NAME,
                                                            BridgeUtils.PUBLIC_API_TYPE);

                                                    PublicApi api = BytecodeGenProxyBuilder.newBuilder(PublicApi.class,
                                                            this, false).build();

                                                    push(param.getAttributeName());
                                                    loadArg(param.getIndex());
                                                    // if this is a primitive value we need to box it to an object
                                                    if (type.getSort() != Type.OBJECT) {
                                                        box(type);
                                                    }

                                                    Label objectLabel = newLabel();
                                                    Label skipLabel = newLabel();
                                                    Label popStackLabel = newLabel();

                                                    // dup the value for null check
                                                    dup();
                                                    ifNull(popStackLabel);

                                                    // dup the value for instanceOf check
                                                    dup();
                                                    instanceOf(Type.getType(Number.class));
                                                    ifZCmp(EQ, objectLabel);

                                                    // if this is a number, cast it to a number and call the
                                                    // addCustomParameter api that takes a number
                                                    checkCast(Type.getType(Number.class));
                                                    api.addCustomParameter("", 0);
                                                    goTo(skipLabel);

                                                    visitLabel(objectLabel);

                                                    // otherwise, it's a non-null object so call toString
                                                    invokeVirtual(Type.getType(Object.class), new Method("toString",
                                                            Type.getType(String.class), new Type[0]));
                                                    api.addCustomParameter("", "");
                                                    goTo(skipLabel);

                                                    // the values originally on the stack won't be consumed by either
                                                    // of the api calls above if the attribute was null
                                                    visitLabel(popStackLabel);
                                                    pop();
                                                    pop();
                                                    pop();

                                                    visitLabel(skipLabel);
                                                }
                                            };
                                        }
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        Agent.LOG.log(Level.FINEST, e, e.toString());
                                    }
                                }
                            }

                        }

                        if (trace.rollupMetricName().length > 0) {

                            final int cacheId = AgentBridge.instrumentation.addToObjectCache(trace.rollupMetricName());
                            mv = new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {

                                @Override
                                protected void onMethodEnter() {
                                    getStatic(BridgeUtils.TRACED_METHOD_TYPE, BridgeUtils.CURRENT_TRANSACTION_FIELD_NAME, BridgeUtils.TRACED_METHOD_TYPE);
                                    super.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, BridgeUtils.INSTRUMENTATION_FIELD_NAME, BridgeUtils.INSTRUMENTATION_TYPE);

                                    Instrumentation instrumentation = BytecodeGenProxyBuilder.newBuilder(Instrumentation.class, this, true).build();
                                    instrumentation.getCachedObject(cacheId);

                                    super.checkCast(Type.getType(String[].class));

                                    TracedMethod tracedMethod = BytecodeGenProxyBuilder.newBuilder(TracedMethod.class, this, false).build();
                                    tracedMethod.setRollupMetricNames((String[]) null);
                                }
                            };
                        }

                        if (TransactionName.isSimpleTransactionName(trace.transactionName())) {
                            mv = new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {

                                @Override
                                protected void onMethodEnter() {
                                    TracedMethod tracedMethod = BytecodeGenProxyBuilder.newBuilder(TracedMethod.class, this, true).build();
                                    getStatic(BridgeUtils.TRACED_METHOD_TYPE, BridgeUtils.CURRENT_TRANSACTION_FIELD_NAME, BridgeUtils.TRACED_METHOD_TYPE);
                                    tracedMethod.nameTransaction(trace.transactionName().transactionNamePriority);
                                }

                            };
                        } else if (trace.transactionName() != null) {
                            mv = new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {

                                @Override
                                protected void onMethodEnter() {
                                    BridgeUtils.getCurrentTransaction(this);

                                    Transaction transaction = BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true).build();
                                    TransactionName transactionName = trace.transactionName();
                                    transaction.setTransactionName(transactionName.transactionNamePriority, transactionName.override, transactionName.category,
                                            transactionName.path);

                                    // the method returns a boolean. Discard it.
                                    pop();
                                }
                            };
                        }

                        if (trace.isWebTransaction()) {
                            mv = new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {
                                @Override
                                protected void onMethodExit(int opcode) {
                                    getStatic(BridgeUtils.TRANSACTION_TYPE, BridgeUtils.CURRENT_TRANSACTION_FIELD_NAME, BridgeUtils.TRANSACTION_TYPE);
                                    BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true).build().convertToWebTransaction();
                                }
                            };
                        }
                        if (null != trace.metricPrefix()) {
                            mv = new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {
                                @Override
                                protected void onMethodExit(int opcode) {
                                    // write: AgentBridge.getAgent().getTracedMethod.setMetricPrefix("usersPrefix")
                                    this.invokeStatic(BridgeUtils.AGENT_BRIDGE_TYPE, new Method(BridgeUtils.AGENT_BRIDGE_GET_AGENT_METHOD,
                                            BridgeUtils.AGENT_BRIDGE_AGENT_TYPE, new Type[0]));
                                    this.invokeVirtual(BridgeUtils.AGENT_BRIDGE_AGENT_TYPE, new Method(
                                            BridgeUtils.GET_TRACED_METHOD_METHOD_NAME, BridgeUtils.TRACED_METHOD_TYPE, new Type[0]));
                                    BytecodeGenProxyBuilder.newBuilder(TracedMethod.class, this, true).build().setCustomMetricPrefix(trace.metricPrefix());
                                }
                            };
                        }
                    }

                    instrumentationContext.addTimedMethods(method);
                } else {
                    Agent.LOG.warning(className + '.' + method
                            + " is matched to trace, but it was already instrumented by " + pointCut.toString());
                }
            }

            if (traceInfo.getIgnoreApdexMethods().contains(method)) {
                instrumentationContext.markAsModified();
                mv = new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {

                    @Override
                    protected void onMethodEnter() {
                        invokeStatic(BridgeUtils.NEW_RELIC_API_TYPE, TraceMethodVisitor.IGNORE_APDEX_METHOD);
                    }

                };

            }
        }

        return mv;
    }
}
