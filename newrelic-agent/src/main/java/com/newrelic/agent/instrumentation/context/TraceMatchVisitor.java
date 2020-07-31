/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import com.newrelic.agent.instrumentation.tracing.Annotation;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;
import com.newrelic.weave.UtilityClass;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * The class visitor returned by this factory matches trace annotations and ignore apdex/transaction trace annotations
 * and records this in the {@link InstrumentationContext}.
 *
 * @see ClassTransformerConfig#getTraceAnnotationMatcher()
 * @see ClassTransformerConfig#getIgnoreApdexAnnotationMatcher()
 * @see ClassTransformerConfig#getIgnoreTransactionAnnotationMatcher()
 */
class TraceMatchVisitor implements ClassMatchVisitorFactory {

    private static final String WEAVE_UTILITY_CLASS_PACKAGE_NAME = "weavePackageName";
    private static final String WEAVE_UTILITY_CLASS_DESC = Type.getDescriptor(UtilityClass.class);
    private static final String TRACE_DESC = Type.getDescriptor(Trace.class);
    private final AnnotationMatcher traceAnnotationMatcher;
    private final AnnotationMatcher ignoreTransactionAnnotationMatcher;
    private final AnnotationMatcher ignoreApdexAnnotationMatcher;

    public TraceMatchVisitor() {
        ConfigService configService = ServiceFactory.getConfigService();
        ClassTransformerConfig classTransformerConfig = configService.getDefaultAgentConfig()
                .getClassTransformerConfig();
        traceAnnotationMatcher = classTransformerConfig.getTraceAnnotationMatcher();
        ignoreTransactionAnnotationMatcher = classTransformerConfig.getIgnoreTransactionAnnotationMatcher();
        ignoreApdexAnnotationMatcher = classTransformerConfig.getIgnoreApdexAnnotationMatcher();
    }

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
            ClassVisitor cv, final InstrumentationContext context) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            private String source;
            private boolean isWeaveUtilityClass = false;

            @Override
            public void visitSource(String source, String debug) {
                super.visitSource(source, debug);
                this.source = source;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (WEAVE_UTILITY_CLASS_DESC.equals(desc)) {
                    isWeaveUtilityClass = true;
                    av = new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, av) {
                        @Override
                        public void visit(String name, Object value) {
                            if (WEAVE_UTILITY_CLASS_PACKAGE_NAME.equals(name)) {
                                source = (String) value;
                            }
                            super.visit(name, value);
                        }
                    };
                }
                return av;
            }

            @Override
            public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc,
                    String signature, String[] exceptions) {
                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, super.visitMethod(access, methodName, methodDesc,
                        signature, exceptions)) {

                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (traceAnnotationMatcher.matches(desc) || (TRACE_DESC.equals(desc) && isWeaveUtilityClass)) {
                            return processTraceAnnotation(desc, visible);
                        }
                        if (ignoreApdexAnnotationMatcher.matches(desc)) {
                            context.addIgnoreApdexMethod(methodName, methodDesc);
                        }
                        if (ignoreTransactionAnnotationMatcher.matches(desc)) {
                            context.addIgnoreTransactionMethod(methodName, methodDesc);
                        }
                        return super.visitAnnotation(desc, visible);
                    }

                    private AnnotationVisitor processTraceAnnotation(final String desc, final boolean visible) {
                        InstrumentationType type = isWeaveUtilityClass ? InstrumentationType.TracedWeaveInstrumentation
                                : InstrumentationType.TraceAnnotation;

                        Annotation node = new Annotation(super.visitAnnotation(desc, visible), TRACE_DESC,
                                TraceDetailsBuilder.newBuilder().setInstrumentationType(type)
                                        .setInstrumentationSourceName(source)) {

                            @Override
                            public void visitEnd() {
                                context.putTraceAnnotation(new Method(methodName, methodDesc), getTraceDetails(
                                        !isWeaveUtilityClass));
                                super.visitEnd();
                            }

                        };

                        return node;
                    }

                };
            }
        };
    }

}
