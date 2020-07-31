/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.preprocessors;

import java.util.Iterator;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.api.agent.Trace;

/**
 * Tracks methods which should be annotated with the TracedWeaveInstrumentation {@link InstrumentationType}.
 */
public class TracedWeaveInstrumentationTracker {
    private final String weavePackageName;
    private final String className;
    private final Method method;
    private final boolean isWeaveIntoAllMethods;
    private final TraceDetails traceDetails;
    private static final String traceDesc = Type.getDescriptor(Trace.class);

    public TracedWeaveInstrumentationTracker(String weavePackageName, String className, Method method,
                                             boolean isWeaveIntoAllMethods, TraceDetails traceDetails) {
        this.weavePackageName = weavePackageName;
        this.className = className;
        this.method = method;
        this.isWeaveIntoAllMethods = isWeaveIntoAllMethods;
        this.traceDetails = traceDetails;
    }

    /**
     * Add this weave trace details to the passed in InstrumentationContext.
     */
    public void addToInstrumentationContext(InstrumentationContext context, Method method) {
        context.addTrace(method, traceDetails);
    }

    /**
     * Remove all @Trace annotations from a MethodNode
     * 
     * @param method
     */
    public static void removeTraceAnnotations(MethodNode method) {
        if (null != method.visibleAnnotations) {
            Iterator<AnnotationNode> iter = method.visibleAnnotations.iterator();
            while (iter.hasNext()) {
                AnnotationNode compositeAnnotation = iter.next();
                if (traceDesc.equals(compositeAnnotation.desc)) {
                    iter.remove();
                }
            }
        }
    }

    public String getWeavePackageName() {
        return weavePackageName;
    }

    public String getClassName() {
        return className;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isWeaveIntoAllMethods() {
        return isWeaveIntoAllMethods;
    }
}
