/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import org.objectweb.asm.commons.Method;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

public class TraceInformation {

    private Map<Method, TraceDetails> traces;
    private Set<Method> ignoreApdexMethods;
    private Set<Method> ignoreTransactionMethods;

    public TraceInformation() {
    }

    /**
     * Gets the field traceAnnotations. This is not a mutable map.
     *
     * @return the traceAnnotations
     */
    public Map<Method, TraceDetails> getTraceAnnotations() {
        return traces == null ? Collections.<Method, TraceDetails>emptyMap() : Collections.unmodifiableMap(traces);
    }

    void pullAll(Map<Method, TraceDetails> tracedMethods) {
        if (traces == null) {
            traces = new HashMap<>(tracedMethods);
        } else {
            for (Entry<Method, TraceDetails> entry : tracedMethods.entrySet()) {
                putTraceAnnotation(entry.getKey(), entry.getValue());
            }
        }
    }

    void putTraceAnnotation(Method method, TraceDetails trace) {
        if (traces == null) {
            traces = new HashMap<>();
        } else {
            TraceDetails existing = traces.get(method);
            if (existing != null) {
                Agent.LOG.log(Level.FINEST, "Merging trace details {0} and {1} for method {2}", existing, trace,
                        method);
                trace = TraceDetailsBuilder.merge(existing, trace);
            }
        }
        traces.put(method, trace);
    }

    /**
     * Gets the field ignoreApdexMethods.
     *
     * @return the ignoreApdexMethods
     */
    public Set<Method> getIgnoreApdexMethods() {
        return ignoreApdexMethods == null ? Collections.<Method>emptySet() : ignoreApdexMethods;
    }

    /**
     * Gets the field ignoreTransactionMethods.
     *
     * @return the ignoreTransactionMethods
     */
    public Set<Method> getIgnoreTransactionMethods() {
        return ignoreTransactionMethods == null ? Collections.<Method>emptySet() : ignoreTransactionMethods;
    }

    public void addIgnoreApdexMethod(final String methodName, final String methodDesc) {
        if (ignoreApdexMethods == null) {
            ignoreApdexMethods = new HashSet<>();
        }
        ignoreApdexMethods.add(new Method(methodName, methodDesc));
    }

    /**
     * Sets the field ignoreTransactionMethods.
     */
    public void addIgnoreTransactionMethod(final String methodName, final String methodDesc) {
        if (ignoreTransactionMethods == null) {
            ignoreTransactionMethods = new HashSet<>();
        }
        ignoreTransactionMethods.add(new Method(methodName, methodDesc));
    }

    /**
     * Sets the field ignoreTransactionMethods.
     */
    public void addIgnoreTransactionMethod(final Method m) {
        if (ignoreTransactionMethods == null) {
            ignoreTransactionMethods = new HashSet<>();
        }
        ignoreTransactionMethods.add(m);
    }

    public boolean isMatch() {
        return !getTraceAnnotations().isEmpty() || !getIgnoreApdexMethods().isEmpty()
                || !getIgnoreTransactionMethods().isEmpty();
    }

}
