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
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.api.agent.TraceByReturnType;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.util.*;

public class TraceByReturnTypeMatchVisitor implements ClassMatchVisitorFactory {

  private static final String TRACE_BY_RETURN_TYPE_DESC = Type.getDescriptor(TraceByReturnType.class);
  private static final String TRACE_RETURN_TYPES_NAME = "traceReturnTypes";

  @Override
  public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, final ClassReader reader, ClassVisitor cv,
                                           final InstrumentationContext context) {
    return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

      private boolean isScalaFutureTrace = false;
      private List<String> traceReturnTypeDescriptors = new ArrayList<>();

      @Override
      public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        if (TRACE_BY_RETURN_TYPE_DESC.equals(descriptor)) {
          isScalaFutureTrace = true;
          av = new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, av) {
            @Override
            public AnnotationVisitor visitArray(String name) {
              AnnotationVisitor av = super.visitArray(name);
              if(TRACE_RETURN_TYPES_NAME.equals(name)) {
                return new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, av) {
                  @Override
                  public void visit(String name, Object value) {
                    super.visit(name, value);
                    traceReturnTypeDescriptors.add(((Type)value).getDescriptor());
                  }
                };
              }
              return av;
            }
          };
        }
        return av;
      }

      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (isScalaFutureTrace && isTracedMethod(descriptor)) {
          Method method = new Method(name, descriptor);
          TraceDetails traceDetails = TraceDetailsBuilder.newBuilder()
                                                         .setMetricName(method.getName())
                                                         .setInstrumentationType(InstrumentationType.BuiltIn)
                                                         .setInstrumentationSourceName(TraceByReturnType.class.getName())
                                                         .setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false,
                                                                             MetricNames.CUSTOM, method.getName())
                                                         .build();
          context.addTrace(method, traceDetails);
        }
        return mv;
      }

      private boolean isTracedMethod(String descriptor) {
        boolean matchFound = false;
        for(String returnTypeDescriptor: traceReturnTypeDescriptors) {
          if(descriptor.endsWith(returnTypeDescriptor)) {
            matchFound = true;
            break;
          }
        }
        return matchFound;
      }
    };
  }
}
