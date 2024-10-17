/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Cloud;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.NewRelic;

/**
 * The internal bridge version of the Agent API.
 */
public interface Agent extends com.newrelic.api.agent.Agent {

    /**
     * Returns the current traced method. This can only be invoked within methods that are traced.
     *
     * @deprecated Use {@link com.newrelic.api.agent.NewRelic}.{@link NewRelic#getAgent() getAgent()}.
     *             {@link com.newrelic.api.agent.Agent#getTracedMethod() getTracedMethod()} if possible instead of this
     *             method.
     *
     * @return The current method being traced
     */
    @Override
    @Deprecated
    TracedMethod getTracedMethod();

    /**
     * Returns the current transaction.
     *
     * @deprecated Use {@link com.newrelic.api.agent.NewRelic}.{@link NewRelic#getAgent() getAgent()}.
     *             {@link com.newrelic.api.agent.Agent#getTransaction() getTransaction()} if possible instead of this
     *             method.
     *
     * @return The current transaction
     */
    @Override
    @Deprecated
    Transaction getTransaction();

    /**
     * Get the transaction stored in a thread local.
     *
     * @param createIfNotExists if true, create a transaction if needed.
     * @return the transaction in the thread local or possibly null if createIfExists == false
     */
    Transaction getTransaction(boolean createIfNotExists);

    /**
     * Return a weak reference to the current transaction stored in the thread local. Use this if you need to compare an actual
     * concrete Transaction object rather than the TransactionApiImpl wrapper
     *
     * @param createIfNotExists if true, create a transaction if needed.
     * @return a weak reference wrapped transaction in the thread local or possibly null if createIfExists == false
     */
    Transaction getWeakRefTransaction(boolean createIfNotExists);

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#getToken()} instead.
     * Inform instrumentation that the asynchronous activity identified by the key is starting. This method must be
     * called while running on the thread that executes the asynchronous activity.
     *
     * @param activityContext the key used by instrumentation to identify this asynchronous activity. The value must
     *        have been previously made known to New Relic by a call to {@see registerAsyncActivity}.
     *        <p>
     *        Implementation note: the activityContext must match the argument passed to {@see registerAsyncActivity} by
     *        identity comparison ("=="). Strings or other values that are derived or computed in the parent activity
     *        and the re-derived or computed in the child do not meet this requirement. In many Java implementations,
     *        string interning is nondeterministic and does not provide a reliable solution.
     *
     * @return true if the context is recognized and an action is taken. False if the context does not exist.
     */
    @Deprecated
    boolean startAsyncActivity(Object activityContext);

    /**
     * Inform instrumentation that the asynchronous activity identified by the key should no longer be tracked by
     * instrumentation. This method may be called from any thread, however it will only apply to activities that haven't
     * been started.
     *
     * @param activityContext the key used by instrumentation to identify this asynchronous activity. The value must be
     *        unique across all asynchronous activities tracked during the life of the current Agent instance and must
     *        have been previously made known to New Relic by a call to {@see registerAsyncActivity}.
     * @return true if the context is recognized and an action is taken. False if the context does not exist.
     */
    boolean ignoreIfUnstartedAsyncContext(Object activityContext);

    /**
     * Provides access to the LogSender events API.
     *
     * @return Object used to add custom events.
     */
    Logs getLogSender();

    String getEntityGuid(boolean wait);

}
