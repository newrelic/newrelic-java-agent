/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import java.util.List;

import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.api.agent.Trace;

/**
 * As classes pass through different transformers registered in the {@link InstrumentationContextManager}, they can add
 * {@link TraceDetails} for methods to the {@link InstrumentationContext}. After all of the registered transformers have
 * passed over the bytecode, the {@link TraceClassVisitor} injects calls into the methods with TraceDetails to create
 * tracers.
 * 
 * Use {@link TraceDetailsBuilder} to create a new TraceDetails instance.
 * 
 * @see InstrumentationContext
 * @see TraceClassVisitor
 * @see TraceMethodVisitor
 * @see Instrumentation#createTracer
 */
public interface TraceDetails {

    /**
     * @see Trace#metricName()
     */
    String metricName();

    String[] rollupMetricName();

    /**
     * If true, a dispatcher tracer will be generated.
     * 
     */
    boolean dispatcher();

    /**
     * If true, transaction creation will be deferred
     * 
     */
    boolean async();

    TransactionName transactionName();

    /**
     * The tracer factory name.
     * 
     * @see Instrumentation#createTracer(Object, int, String, int, String)
     */
    String tracerFactoryName();

    /**
     * This means the transaction trace is still present, but this method will be excluded from the call graph. The
     * individual metric for the method will also still be present.
     * 
     * @see Trace#excludeFromTransactionTrace()
     */
    boolean excludeFromTransactionTrace();

    String metricPrefix();

    String getFullMetricName(final String className, final String methodName);

    /**
     * This means the whole transaction will be ignored, regardless of how far the code is into the transaction.
     */
    boolean ignoreTransaction();

    /**
     * The type of instrumentation that generated this trace.
     * 
     * @see InstrumentationType
     * @see InstrumentedMethod#instrumentationTypes()
     */
    List<InstrumentationType> instrumentationTypes();

    /**
     * The name of the instrumentation source such as the weave package implementation title, the custom xml name, the
     * pointcut name, etc.
     * 
     * @see InstrumentedMethod#instrumentationNames()
     */
    List<String> instrumentationSourceNames();

    /**
     * Returns true if this instrumentation was user generated.
     * 
     */
    boolean isCustom();

    /**
     * @see Trace#leaf()
     */
    boolean isLeaf();

    boolean isWebTransaction();

    /**
     * Returns a list of method parameter attribute names. The matching method parameters will be added to Transactions
     * as custom parameters.
     * 
     */
    List<ParameterAttributeName> getParameterAttributeNames();

}
