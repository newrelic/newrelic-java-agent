/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.text.MessageFormat;
import java.util.logging.Level;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.weave.utils.WeaveUtils;

/**
 * This method adapter is responsible for injecting our tracer code around methods.
 */
abstract class AbstractTracingMethodAdapter extends AdviceAdapter {
    private static final String JAVA_LANG_THROWABLE = "java/lang/Throwable";
    private static final boolean sDebugTracers = false;

    protected final String methodName;
    private int tracerLocalId;
    private final Label startFinallyLabel = new Label();
    protected final GenericClassAdapter genericClassAdapter;
    private int invocationHandlerIndex = -1;

    protected final MethodBuilder methodBuilder;

    public AbstractTracingMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access,
            Method method) {
        super(WeaveUtils.ASM_API_LEVEL, mv, access, method.getName(), method.getDescriptor());
        this.genericClassAdapter = genericClassAdapter;
        this.methodName = method.getName();
        methodBuilder = new MethodBuilder(this, access);
    }

    String getMethodDescriptor() {
        return this.methodDesc;
    }

    protected void systemOutPrint(String message) {
        systemPrint(message, false);
    }

    protected void systemPrint(String message, boolean error) {
        getStatic(Type.getType(System.class), error ? "err" : "out", Type.getType(PrintStream.class));
        visitLdcInsn(message);
        invokeVirtual(Type.getType(PrintStream.class), new Method("println", "(Ljava/lang/String;)V"));
    }

    /**
     * Writes the instrumentation code that is executed before the existing method body. The injected byte-code will get
     * an agent handle and get a tracer from the agent for this particular method invocation.
     */
    @Override
    protected void onMethodEnter() {
        int methodIndex = genericClassAdapter.addInstrumentedMethod(this);
        if (genericClassAdapter.canModifyClassStructure()) {
            setInvocationFieldIndex(methodIndex);
        }
        try {
            if (sDebugTracers) {
                systemOutPrint(MessageFormat.format("NewRelicAgent: Entering method {0}.{1}{2}",
                        genericClassAdapter.className, methodName, methodDesc));
            }

            final Type tracerType = getTracerType();
            tracerLocalId = newLocal(tracerType);

            visitInsn(ACONST_NULL);
            storeLocal(tracerLocalId);

            Label startLabel = new Label();
            Label endLabel = new Label();
            Label exceptionLabel = new Label();

            mv.visitTryCatchBlock(startLabel, endLabel, exceptionLabel, JAVA_LANG_THROWABLE);
            mv.visitLabel(startLabel);
            loadGetTracerArguments();
            invokeGetTracer();

            storeLocal(tracerLocalId);
            mv.visitLabel(endLabel);
            Label doneLabel = new Label();
            goTo(doneLabel);
            mv.visitLabel(exceptionLabel);
            if (sDebugTracers || Agent.LOG.isLoggable(Level.FINER)) {
                // print the exception, effectively popping it
                mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_THROWABLE, "printStackTrace", "()V", false);
                systemPrint(MessageFormat.format("An error occurred creating a tracer for {0}.{1}{2}",
                        genericClassAdapter.className, methodName, methodDesc), true);
            } else {
                // okay, we still need to store the exception
                final int exceptionVar = newLocal(Type.getType(Throwable.class));
                visitVarInsn(ASTORE, exceptionVar);
            }
            mv.visitLabel(doneLabel);

        } catch (Throwable e) {
            Agent.LOG.severe(MessageFormat.format("An error occurred transforming {0}.{1}{2} : {3}",
                    genericClassAdapter.className, methodName, methodDesc, e.toString()));
            throw new RuntimeException(e);
        }
    }

    private void setInvocationFieldIndex(int id) {
        invocationHandlerIndex = id;
    }

    public int getInvocationHandlerIndex() {
        return invocationHandlerIndex;
    }

    protected final Type getTracerType() {
        return MethodBuilder.INVOCATION_HANDLER_TYPE;
    }

    protected final void invokeGetTracer() {
        methodBuilder.invokeInvocationHandlerInterface(false);
    }

    protected abstract void loadGetTracerArguments();

    public GenericClassAdapter getGenericClassAdapter() {
        return genericClassAdapter;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        super.visitLabel(startFinallyLabel);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinallyLabel = new Label();
        super.visitTryCatchBlock(startFinallyLabel, endFinallyLabel, endFinallyLabel, JAVA_LANG_THROWABLE);
        super.visitLabel(endFinallyLabel);
        onFinally(ATHROW);
        super.visitInsn(ATHROW);
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            onFinally(opcode);
        }
    }

    /**
     * Writes the instrumentation code that is executed after the existing method body. The injected byte-code stores
     * the method return value (or the throwable if an exception was thrown) and calls either
     * {@link Tracer#finish(int, Object)} or {@link Tracer#finish(Throwable)}. If this is a bootstrap class, the finish
     * methods are invoked through the {@link InvocationHandler} wrapper around the tracer that was created by the
     * {@link AgentWrapper}.
     */
    protected void onFinally(final int opcode) {
        Label end = new Label();
        if (opcode == ATHROW) {
            if ("<init>".equals(methodName)) {
                return;
            }
            dup();
            final int exceptionVar = newLocal(Type.getType(Throwable.class));
            visitVarInsn(ASTORE, exceptionVar);

            loadLocal(tracerLocalId);
            ifNull(end);
            loadLocal(tracerLocalId);
            // shouldn't need to check cast - the local var is an invocation handler
            checkCast(MethodBuilder.INVOCATION_HANDLER_TYPE);

            invokeTraceFinishWithThrowable(exceptionVar);
        } else {
            Object loadReturnValue = null;
            if (opcode != RETURN) {
                loadReturnValue = new StoreReturnValueAndReload(opcode);
            }

            loadLocal(tracerLocalId);
            ifNull(end);
            loadLocal(tracerLocalId);

            invokeTraceFinish(opcode, loadReturnValue);
        }
        visitLabel(end);

    }

    protected final void invokeTraceFinish(final int opcode, Object loadReturnValue) {
        methodBuilder.loadSuccessful().loadArray(Object.class, opcode, loadReturnValue).invokeInvocationHandlerInterface(
                true);
    }

    protected final void invokeTraceFinishWithThrowable(final int exceptionVar) {
        methodBuilder.loadUnsuccessful()
                .loadArray(Object.class, (Runnable) () -> visitVarInsn(ALOAD, exceptionVar))
                .invokeInvocationHandlerInterface(true);
    }

    /**
     * Store the return value of this method and load it when {@link #run()} is invoked.
     */
    private final class StoreReturnValueAndReload implements Runnable {
        private final int returnVar;

        public StoreReturnValueAndReload(int opcode) {
            Type returnType = Type.getReturnType(methodDesc);

            // dup the return value and box it
            if (returnType.getSize() == 2) {
                dup2();
            } else {
                dup();
            }
            returnType = methodBuilder.box(returnType);

            returnVar = newLocal(returnType);
            storeLocal(returnVar, returnType);
        }

        @Override
        public void run() {
            loadLocal(returnVar);
        }

    }

}
