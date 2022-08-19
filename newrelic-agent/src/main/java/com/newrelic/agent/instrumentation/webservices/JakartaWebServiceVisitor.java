/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.AnnotationDetails;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Instrument implementations of {@literal WebService} so that we time the web methods and name the transaction using the
 * method name.
 */
public class JakartaWebServiceVisitor implements ClassMatchVisitorFactory {
    private static final String WEB_SERVICE_ANNOTATION_DESCRIPTOR = getDescriptor("jakarta.jws.WebService");

    private static String getDescriptor(String className) {
        StringBuilder buf = new StringBuilder();

        buf.append('L');
        int len = className.length();
        for (int i = 0; i < len; ++i) {
            char car = className.charAt(i);
            buf.append(car == '.' ? '/' : car);
        }
        buf.append(';');

        return buf.toString();
    }

    @Override
    public ClassVisitor newClassMatchVisitor(final ClassLoader loader, Class<?> classBeingRedefined,
            final ClassReader reader, ClassVisitor cv, final InstrumentationContext context) {

        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            Map<Method, AnnotationDetails> methodsToInstrument;
            Map<String, String> classWebServiceAnnotationDetails;
            String webServiceAnnotationNameValue;

            /**
             * If the {@literal WebService} annotation is present, add all of the methods on the interfaces marked with the
             * WebService annotation that this class implements to the methodsToInstrument set. Note: this is a rather
             * loose match. We should look for WebMethod annotations on these methods.
             */
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (WEB_SERVICE_ANNOTATION_DESCRIPTOR.equals(desc)) {
                    methodsToInstrument = new HashMap<>();
                    classWebServiceAnnotationDetails = new HashMap<>();
                    try {
                        ClassStructure classStructure = ClassStructure.getClassStructure(Utils.getClassResource(
                                loader, reader.getClassName()), ClassStructure.ALL);
                        AnnotationDetails webServiceDetails = classStructure.getClassAnnotations().get(
                                WEB_SERVICE_ANNOTATION_DESCRIPTOR);
                        if (webServiceDetails != null) {

                            webServiceAnnotationNameValue = (String) webServiceDetails.getValue("name");

                            for (Method m : classStructure.getMethods()) {
                                Map<String, AnnotationDetails> methodAnnotations = classStructure.getMethodAnnotations(m);
                                AnnotationDetails webMethodDetails = methodAnnotations.get(getDescriptor("jakarta.jws.WebMethod"));

                                if (webMethodDetails != null) {
                                    methodsToInstrument.put(m, webMethodDetails);
                                }
                            }

                        }
                    } catch (Exception e) {
                        Agent.LOG.log(Level.FINEST, e.toString(), e);
                    }

                    return new WebServiceAnnotationVisitor(super.visitAnnotation(desc, visible));
                }
                return super.visitAnnotation(desc, visible);
            }

            /**
             * Instrument all of the methods in the methodsToInstrument set.
             */
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (methodsToInstrument != null) {
                    Method method = new Method(name, desc);

                    if (methodsToInstrument.containsKey(method)) {
                        AnnotationDetails webMethod = methodsToInstrument.get(method);
                        // REVIEW should we use the "name" attribute of the interface WebService annotation?
                        String className = classWebServiceAnnotationDetails.get("endpointInterface");
                        if (className == null) {
                            className = Type.getObjectType(reader.getClassName()).getClassName();
                        }
                        String operationName = webMethod == null ? name : (String) webMethod.getValue("operationName");
                        if (operationName == null) {
                            operationName = name;
                        }
                        String txName = Strings.join(MetricNames.SEGMENT_DELIMITER, className, operationName);

                        if (Agent.LOG.isFinerEnabled()) {
                            Agent.LOG.finer("Creating a web service tracer for " + reader.getClassName() + '.' + method
                                    + " using transaction name " + txName);
                        }
                        context.addTrace(
                                method,
                                TraceDetailsBuilder.newBuilder().setInstrumentationType(InstrumentationType.BuiltIn).setInstrumentationSourceName(
                                        JakartaWebServiceVisitor.class.getName()).setDispatcher(true).setWebTransaction(true).setTransactionName(
                                        TransactionNamePriority.FRAMEWORK_HIGH, false, "WebService", txName).build());
                    }
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            class WebServiceAnnotationVisitor extends AnnotationVisitor {

                public WebServiceAnnotationVisitor(AnnotationVisitor av) {
                    super(WeaveUtils.ASM_API_LEVEL, av);
                }

                @Override
                public void visit(String name, Object value) {
                    if (value instanceof String) {
                        classWebServiceAnnotationDetails.put(name, (String) value);
                    }
                    super.visit(name, value);
                }
            }

        };
    }
}
