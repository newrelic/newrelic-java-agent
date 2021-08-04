/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.api.agent.TraceLambda;
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

/**
 * The class visitor returned by this factory matches TraceLambda annotations and matches the marked classes method names
 * against the TraceLambda annotations pattern, if matched marking them to be traced, recording this in the {@link InstrumentationContext}.
 */
public class TraceLambdaVisitor implements ClassMatchVisitorFactory {

    /**
     * The annotation which the class visitor matches
     */
    private static final String TRACE_LAMBDA_DESC = Type.getDescriptor(TraceLambda.class);
    /**
     * The default pattern used to match against a marked classes method names. Matches Java and Scala lambda method names.
     */
    private static final Pattern LAMBDA_METHOD_NAME_PATTERN = Pattern.compile("^\\$?(lambda|anonfun)\\$(?<name>.*)");
    private static final String PATTERN_PROPERTY_NAME = "pattern";
    private static final String INCLUDE_NONSTATIC_PROPERTY_NAME = "includeNonstatic";

    @Override
    public ClassVisitor newClassMatchVisitor(
            ClassLoader loader,
            Class<?> classBeingRedefined,
            final ClassReader reader, ClassVisitor cv,
            final InstrumentationContext context
    ) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            private boolean isTraceLambdaClass = false;
            private boolean includeNonstatic = false;
            private Pattern traceLambdaPattern = LAMBDA_METHOD_NAME_PATTERN;


            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
                if (TRACE_LAMBDA_DESC.equals(descriptor)) {
                    isTraceLambdaClass = true;
                    av = new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, av) {
                        @Override
                        public void visit(String name, Object value) {
                            super.visit(name, value);
                            if(PATTERN_PROPERTY_NAME.equals(name) && value instanceof String && value != "") {
                                traceLambdaPattern = Pattern.compile(String.valueOf(value));
                            }
                            if(INCLUDE_NONSTATIC_PROPERTY_NAME.equals(name) && value instanceof Boolean) {
                                includeNonstatic = (boolean) value;
                            }
                        }
                    };
                }
                return av;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (isTraceLambdaClass) {
                    boolean isStatic = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
                    if (includeNonstatic || isStatic) {
                        Method method = new Method(name, descriptor);
                        Matcher matcher = traceLambdaPattern.matcher(method.getName());
                        if (matcher.matches()) {
                            String match = containsNameGroup(matcher) ? matcher.group("name") : matcher.group();
                            String metricName = match.replace("$", ".");
                            context.addTrace(method, TraceDetailsBuilder.newBuilder()
                                    .setMetricName(metricName)
                                    .setInstrumentationType(InstrumentationType.BuiltIn)
                                    .setInstrumentationSourceName(TraceLambdaVisitor.class.getName())
                                    .setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, MetricNames.CUSTOM, metricName)
                                    .build());
                        }
                    }
                }
                return mv;
            }

            private boolean containsNameGroup(Matcher matcher) {
                try {
                    return !matcher.group("name").isEmpty();
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            }
        };
    }
}
