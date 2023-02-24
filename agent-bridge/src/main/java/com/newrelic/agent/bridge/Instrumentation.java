/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.io.Closeable;
import java.lang.reflect.Method;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public interface Instrumentation {

    /**
     * Create a tracer. A call to this method is injected into methods that are traced with weaved implementations,
     * trace annotations and custom xml.
     * 
     * @param invocationTarget The instance of the object owning the method being invoked, or null for a static method.
     * @param signatureId The index of the ClassMethodSignature in the ClassMethodSignatures cache.
     * @param metricName
     * @param flags
     * @return the Tracer on which to call finish()
     */
    ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags);

    /**
     * Create a tracer. A call to this method is injected into methods that are traced because of yaml configuration.
     * 
     * @param invocationTarget The instance of the object owning the method being invoked, or null for a static method.
     * @param signatureId The index of the ClassMethodSignature in the ClassMethodSignatures cache.
     * @param dispatcher If true, this should be treated as the start of a transaction if a transaction is not already
     *        in progress.
     * @param metricName
     * @param tracerFactory
     * @param args The arguments passed in the method invocation.
     * @return the Tracer on which to call finish()
     */
    ExitTracer createTracer(Object invocationTarget, int signatureId, boolean dispatcher, String metricName,
            String tracerFactoryName, Object[] args);

    /**
     * Create a sql tracer. A call to this method is injected into methods that are traced with weaved implementations,
     * trace annotations and custom xml where the
     * {@link com.newrelic.agent.bridge.datastore.DatastoreMetrics#noticeSql(ConnectionFactory, String, Object[])}
     * method is used. The sql tracer properly handles queueing up and managing explain plans for slow sql.
     *
     * @param invocationTarget The instance of the object owning the method being invoked, or null for a static method.
     * @param signatureId The index of the ClassMethodSignature in the ClassMethodSignatures cache.
     * @param metricName
     * @param flags
     * @return the Tracer on which to call finish()
     */
    ExitTracer createSqlTracer(Object invocationTarget, int signatureId, String metricName, int flags);

    ExitTracer createScalaTxnTracer();

    /**
     * Returns the current transaction. This should not be called directly - instead use {@link Agent#getTransaction()}.
     *
     * @deprecated Use {@link com.newrelic.api.agent.NewRelic}.{@link NewRelic#getAgent() getAgent()}.
     *             {@link com.newrelic.api.agent.Agent#getTransaction()} getTransaction()} if possible instead of this
     * 
     * @return Tx on thread or created Tx
     */
    @Deprecated
    Transaction getTransaction();

    /**
     * Returns the current transaction. This should not be called directly
     *
     * @return Tx on thread, or null.
     */
    Transaction getTransactionOrNull();

    /**
     * Log an instrumentation error.
     * 
     * @param throwable
     * @param libraryName
     */
    void noticeInstrumentationError(Throwable throwable, String libraryName);

    /**
     * Adds instrumentation for all methods of a given class, but does not call retransform.
     * 
     * @param className - exact class name to instrument all methods.
     * @param metricPrefix
     */
    void instrument(String className, String metricPrefix);

    /**
     * Add instrumentation for a given method of a given class only if no @InstrumentedMethod annotation is present.
     * Does not instrument native, abstract, or interface methods. Calls retransform if necessary.
     * 
     * @param methodToInstrument - exact method name along with its associated declared class to instrument.
     * @param metricPrefix
     */
    void instrument(Method methodToInstrument, String metricPrefix);

    /**
     * Trace with transaction activity enabled (async=true) the first non-New Relic stack element on the current stack.
     * Often customers will call APIs like {@link Token#link()}, which need to be called within a transaction,
     * without having instrumented code to start a transaction.  We can detect that case and instrument the calling
     * method.
     * This will be rate limited to reduce the overhead, controlled by
     * the config class_transformer:auto_async_link_rate_limit
     */
    void instrument();

    /**
     * Re-transform a class if it hasn't already been instrumented (Annotated with @InstrumentedClass). Classes that
     * have instrumentation added or removed will not be affected.
     * 
     * @param classToRetransform
     */
    void retransformUninstrumentedClass(Class<?> classToRetransform);

    Class<?> loadClass(ClassLoader classLoader, Class<?> theClass) throws ClassNotFoundException;

    int addToObjectCache(Object object);

    Object getCachedObject(int id);

    void registerCloseable(String instrumentationName, Closeable closeable);
}
