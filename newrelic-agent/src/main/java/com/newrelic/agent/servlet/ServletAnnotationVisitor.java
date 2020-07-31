/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.servlet;

import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.weave.utils.WeaveUtils;

public class ServletAnnotationVisitor implements ClassMatchVisitorFactory {
    private static final String WEB_SERVLET_DESCRIPTOR = Type.getObjectType("javax/servlet/annotation/WebServlet").getDescriptor();

    // private static final String WEB_FILTER_DESCRIPTOR =
    // Type.getObjectType("javax/servlet/annotation/WebFilter").getDescriptor();

    private static final Set<String> SERVLET_METHODS = ImmutableSet.of("service", "doGet", "doPost", "doHead", "doPut",
            "doOptions", "doTrace");

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
            final ClassReader reader, ClassVisitor cv, final InstrumentationContext context) {

        cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            private TraceDetails traceDetails;

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (traceDetails != null && SERVLET_METHODS.contains(name)) {
                    context.addTrace(new Method(name, desc), traceDetails);
                }

                return mv;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (WEB_SERVLET_DESCRIPTOR.equals(desc)) {
                    return new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, super.visitAnnotation(desc, visible)) {

                        String[] urlPatterns;

                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            AnnotationVisitor av = super.visitArray(name);
                            if ("value".equals(name) || "urlPatterns".equals(name)) {
                                av = new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, av) {

                                    @Override
                                    public void visit(String name, Object value) {
                                        super.visit(name, value);
                                        if (urlPatterns == null) {
                                            urlPatterns = new String[] { value.toString() };
                                        }
                                    }

                                };
                            }
                            return av;

                        }

                        @Override
                        public void visitEnd() {
                            super.visitEnd();

                            if (urlPatterns != null) {
                                // For now I'm going to treat the servlet annotations like a 'framework'. A separate
                                // category is overkill.
                                traceDetails = TraceDetailsBuilder.newBuilder().setInstrumentationType(
                                        InstrumentationType.BuiltIn).setInstrumentationSourceName(
                                        ServletAnnotationVisitor.class.getName()).setTransactionName(
                                        TransactionNamePriority.FRAMEWORK_LOW, false, "WebServletPath", urlPatterns[0]).build();
                            }
                        }

                    };
                }
                return super.visitAnnotation(desc, visible);
            }

        };

        return cv;
    }

}
