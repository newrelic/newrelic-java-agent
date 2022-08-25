/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.ejb4;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.util.asm.AnnotationDetails;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Detect @Stateless and @Stateful EJBs. Also detect those that implement jakarta.ejb.SessionBean. Find all methods
 * declared by their corresponding @Remote or @Local interface and trace those methods.
 */
public class EJB4AnnotationVisitor implements ClassMatchVisitorFactory {
    private static final Set<String> EJB_DESCRIPTORS = ImmutableSet.of(
            Type.getObjectType("jakarta/ejb/Stateless").getDescriptor(),
            Type.getObjectType("jakarta/ejb/Stateful").getDescriptor());
    private static final String EJB_REMOTE_INTERFCE_DESCRIPTOR = Type.getObjectType("jakarta/ejb/Remote").getDescriptor();
    private static final String EJB_LOCAL_INTERFCE_DESCRIPTOR = Type.getObjectType("jakarta/ejb/Local").getDescriptor();

    private static final Object EJB_INTERFACE = Type.getObjectType("jakarta/ejb/SessionBean");

    @Override
    public ClassVisitor newClassMatchVisitor(final ClassLoader loader, Class<?> classBeingRedefined,
            final ClassReader reader, ClassVisitor cv, final InstrumentationContext context) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            Set<Method> methodsToInstrument = new HashSet<>();

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                for (String interfaceName : interfaces) {
                    if (interfaceName.equals(EJB_INTERFACE)) {
                        try {
                            URL classResource = Utils.getClassResource(loader, interfaceName);
                            if (classResource == null) {
                                continue;
                            }

                            ClassStructure classStructure = ClassStructure.getClassStructure(classResource, ClassStructure.ALL);
                            collectMethodsToInstrument(classStructure);
                        } catch (IOException e) {
                            if (Agent.LOG.isFinerEnabled()) {
                                Agent.LOG.finer("EJB class match visitor: " + e.toString());
                            }
                        }
                    }
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (EJB_DESCRIPTORS.contains(desc)) {
                    // Find interface annotated with @Remote or @Local and collect all the methods it declares.
                    for (String interfaceName : reader.getInterfaces()) {
                        try {
                            URL classResource = Utils.getClassResource(loader, interfaceName);
                            if (classResource == null) {
                                continue;
                            }

                            ClassStructure classStructure = ClassStructure.getClassStructure(classResource, ClassStructure.ALL);
                            Map<String, AnnotationDetails> annotations = classStructure.getClassAnnotations();
                            if (annotations.containsKey(EJB_REMOTE_INTERFCE_DESCRIPTOR)
                                    || annotations.containsKey(EJB_LOCAL_INTERFCE_DESCRIPTOR)) {
                                collectMethodsToInstrument(classStructure);
                            }
                        } catch (IOException e) {
                            if (Agent.LOG.isFinerEnabled()) {
                                Agent.LOG.finer("EJB annotation visitor: " + e.toString());
                            }
                        }
                    }
                }
                return super.visitAnnotation(desc, visible);
            }

            @Override
            public MethodVisitor visitMethod(int access, final String name, final String desc, final String signature,
                    String[] exceptions) {
                Method method = new Method(name, desc);
                if (methodsToInstrument.contains(method)) {
                    if (Agent.LOG.isFinerEnabled()) {
                        Agent.LOG.finer("Creating a tracer for " + reader.getClassName() + '.' + method);
                    }
                    context.addTrace(method, TraceDetailsBuilder.newBuilder().setInstrumentationType(
                            InstrumentationType.BuiltIn).setInstrumentationSourceName(
                            EJB4AnnotationVisitor.class.getName()).build());
                }

                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            private void collectMethodsToInstrument(ClassStructure classStructure) {
                for (Method m : classStructure.getMethods()) {
                    methodsToInstrument.add(m);
                }
            }
        };

    }

}
