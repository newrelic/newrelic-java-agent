/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.TransportType;

import java.util.Map;

/**
 * The internal bridge version of Transaction.
 */
public interface Transaction extends com.newrelic.api.agent.Transaction {

    Map<String, Object> getAgentAttributes();
    Map<String, Object> getUserAttributes();

    /**
     * Sets the current transaction's name.
     *
     * @param namePriority
     * @param override Overrides the current transaction name if it has the same priority level (or lower).
     * @param category
     * @param parts
     */
    boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category, String... parts);

    /**
     * @Deprecated Do not use. Use {@link #addOutboundResponseHeaders()} instead.
     */
    @Deprecated
    void beforeSendResponseHeaders();

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#addOutboundResponseHeaders} instead.
     *
     * Instruct the transaction to write the outbound response headers. This must be called before response headers are
     * sent and the response is committed. Successive calls will have no effect (first wins).
     *
     * This must be called after {@link #setWebRequest(ExtendedRequest)} and {@link #setWebResponse(Response)}.
     */
    @Deprecated
    void addOutboundResponseHeaders();

    boolean isStarted();

    /**
     * Sets the name of the application as reported in the New Relic UI.
     *
     * @param priority
     * @param appName
     */
    void setApplicationName(ApplicationNamePriority priority, String appName);

    boolean isAutoAppNamingEnabled();

    /**
     * @Deprecated Do not use.
     *
     * @return
     */
    @Deprecated
    boolean isWebRequestSet();

    /**
     * @Deprecated Do not use.
     *
     * @return
     */
    @Deprecated
    boolean isWebResponseSet();

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#setWebRequest} instead.
     *
     * Sets the request for the current transaction.
     * Setting the request will convert the current transaction into a web transaction.
     * Successive calls will have no effect (first wins).
     *
     * @param request The current transaction's request.
     */
    @Deprecated
    void setWebRequest(Request request);

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#setWebResponse} instead.
     *
     * Sets the response for the current transaction.
     * Setting the response will convert the current transaction into a web transaction.
     * Successive calls will have no effect (first wins).
     *
     * @param response The current transaction's response.
     */
    @Deprecated
    void setWebResponse(Response response);

    /**
     * Provide a headers collection to be processed for NewRelic-defined synthetics, CAT, or beacon (RUM) header keys
     *
     * @param headers a headers collection
     */
    void provideHeaders(InboundHeaders headers);

    /**
     * @Deprecated Do not use.
     *
     * Returns the web response associated with this transaction that tracks the response status code, message, etc.
     * This method will always return an object so there's no need for a null check.
     *
     * @return
     */
    @Deprecated
    WebResponse getWebResponse();

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#convertToWebTransaction} instead.
     *
     * Turns the current transaction from a background transaction into a web transaction.
     */
    @Deprecated
    void convertToWebTransaction();

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#isWebTransaction} instead.
     *
     * Returns true if in a web transaction.
     *
     * @return
     */
    @Deprecated
    boolean isWebTransaction();


    /**
     * Ignore throwable and http status code errors resulting from this transaction.
     *
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#ignoreErrors()} instead.
     */
    void ignoreErrors();

    /**
     * Called from servlet containers when a request is initiated. This starts a transaction which will be completed
     * when {@link #requestDestroyed()} is invoked.
     *
     * @param request
     * @param response
     */
    void requestInitialized(Request request, Response response);

    /**
     * Called from servlet containers when a request is destroyed to finish the current web transaction.
     */
    void requestDestroyed();

    /**
     * @param parameters
     */
    void saveMessageParameters(Map<String, String> parameters);

    CrossProcessState getCrossProcessState();

    /**
     * Starts a flyweight tracer and returns the parent tracer.
     *
     * @return
     */
    TracedMethod startFlyweightTracer();

    /**
     * @param parent
     * @param startInNanos
     * @param finishInNanos
     * @param className
     * @param methodName
     * @param methodDesc
     * @param metricName
     * @param rollupMetricNames
     */
    void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
            String methodName, String methodDesc, String metricName, String[] rollupMetricNames);

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#getToken()} instead.
     *
     * Make a pending asynchronous activity known to instrumentation, associating the given key with the activity. This
     * method must be called within a Transaction (the "active" Transaction) while running on the thread that creates
     * the asynchronous activity.
     * <p>
     * As a result of this call, the registered activity becomes associated with the active Transaction. The active
     * Transaction will not complete until all associated activities complete. A registered activity may complete in one
     * of three ways:
     * <ul>
     * <li>the activity is started via a call to {@see startAsyncActivity} and proceeds to completion.
     * <li>a call is made to {@see ignoreAsyncActivity} with the same argument.
     * <li>the registration times out.
     * </ul>
     * The timeout period is unspecified but is in the range of several minutes. The timeout behavior is defined in
     * order to prevent unbounded memory growth from occurring as a consequence of ill-behaved applications that do not
     * properly manage their asynchronous activities. Relying on the timeout behavior is strongly discouraged.
     *
     * Note: calling this method on a transaction that is already ignored will have no effect.
     *
     * @param activityContext the key used by instrumentation to identify this asynchronous activity. The value must be
     *        unique across all asynchronous activities tracked during the life of the current Agent instance.
     * @return true if the registration succeeded else false
     */
    @Deprecated
    boolean registerAsyncActivity(Object activityContext);

    /**
     * Does not affect APM UI. See {@link #markResponseSent()} to set the response time.
     *
     * Marks the time when the first byte of the response left the server. This time can only be set once. After the
     * first setting, all other attempts to set the time will fail and return false.
     *
     * @return True if the time to first byte was successfully set.
     */
    boolean markFirstByteOfResponse();

    /**
     * Does not affect APM UI. See {@link #markResponseSent()} to set the response time.
     *
     * Marks the time to last byte as right now (as this method is called). This time can only be set once. After the
     * first setting, all other attempts to set the time will fail and return false.
     *
     * @return True if the last byte time has not already been set.
     */
    boolean markLastByteOfResponse();

    /**
     * @Deprecated Do not use. Use {@link #markResponseSent()} instead.
     *
     * Marks the time when the last byte of the response left the server as the time when this method ends. This time
     * can only be set once. After the first setting, all other attempts to set the time will fail and return false.
     *
     * @return True if the last byte time has not already been set.
     */
    @Deprecated
    void markResponseAtTxaEnd();

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#markResponseSent()} instead.
     * Marks the time when the last byte of the response left the server as the current timestamp.
     * Successive calls will have no effect (first wins).
     *
     * @return True if the call to set the response time was successful
     */
    @Deprecated
    boolean markResponseSent();

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#getToken()} instead.
     * Used when asynchronous work needs to be added to the current transaction. The token must be passed to the other
     * thread of work which should be linked. Additionally, the transaction will remain open until the token is expired.
     *
     * @return A token to pass to another thread with work for the current transaction.
     */
    @Deprecated
    Token getToken();

    /**
     * Expires all tokens associated with the current transaction. The transaction will then end when all currently
     * running work then finishes.
     */
    void expireAllTokens();

    boolean clearTransaction();

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Transaction#startSegment(String)} instead.
     * Create and start a {@link TracedActivity} for this transaction.
     *
     * @return a new traced activity, if successful. Will return null if the circuit breaker is tripped, if the
     *         transaction has not started, or if the transaction is ignored.
     */
    @Deprecated
    TracedActivity createAndStartTracedActivity();

    /**
     * Create a distributed trace payload.
     *
     * @return a {@link DistributedTracePayload}
     */
    DistributedTracePayload createDistributedTracePayload();

    /**
     * Accept a distributed trace payload.
     *
     * @param payload {@link DistributedTracePayload} to accept
     */
    void acceptDistributedTracePayload(String payload);

    /**
     * Accept a distributed trace payload.
     *
     * @param payload {@link DistributedTracePayload} to accept
     */
    void acceptDistributedTracePayload(DistributedTracePayload payload);

    void setTransportType(TransportType transportType);

}
