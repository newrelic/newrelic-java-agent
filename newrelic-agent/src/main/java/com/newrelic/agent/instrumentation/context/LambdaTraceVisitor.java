/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.api.agent.LambdaTrace;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LambdaTraceVisitor implements ClassMatchVisitorFactory {

    private static final String LAMBDA_TRACE_DESC = Type.getDescriptor(LambdaTrace.class);
    private static final Pattern LAMBDA_METHOD_NAME = Pattern.compile("^\\$anonfun\\$(.*)");

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, final ClassReader reader, ClassVisitor cv,
            final InstrumentationContext context) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            private boolean isLambdaTraceClass = false;

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
                if (LAMBDA_TRACE_DESC.equals(descriptor)) {
                    isLambdaTraceClass = true;
                }
                return av;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (isLambdaTraceClass) {
                    boolean isStatic = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
                    if (isStatic) {
                        Method method = new Method(name, descriptor);
                        Matcher matcher = LAMBDA_METHOD_NAME.matcher(method.getName());
                        if (matcher.matches()) {
                            context.addTrace(method, TraceDetailsBuilder.newBuilder()
                                    .setInstrumentationType(InstrumentationType.BuiltIn)
                                    .setInstrumentationSourceName(LambdaTraceVisitor.class.getName())
                                    .setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, MetricNames.CUSTOM, matcher.group(1).replace("$", "."))
                                    .build());
                        }
                    }
                }
                return mv;
            }
        };
    }
}
