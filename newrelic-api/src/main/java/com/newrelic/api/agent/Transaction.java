/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.net.URI;

/**
 * A transaction represents a unit of work in an application. It may be a single web request or a scheduled background
 * task. To indicate that a method is the starting point of a transaction, mark it with a {@link Trace} annotation and
 * set {@link Trace#dispatcher()} to true.
 *
 * @see Agent#getTransaction()
 * @see Trace#dispatcher()
 */
public interface Transaction {

    /**
     * Sets the current transaction's name using the given priority. Higher priority levels are given precedence, and if
     * the name is set many times with the same priority, the first call wins unless override is true.
     *
     * @param namePriority The priority of the new transaction name.
     * @param override Overrides the current transaction name if it has the same priority level (or lower).
     * @param category The type of transaction. This is the second segment of the full transaction metric name.
     * @param parts The category and all of the parts are concatenated together with / characters to create the full
     * name.
     * @return Returns true if the transaction name was successfully changed, else false.
     * @since 3.9.0
     */
    boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category, String... parts);

    /**
     * Returns true if the transaction name has been set. This method is inherently unreliable in the presence of
     * transactions with multiple threads, because another thread may set the transaction name after this method returns
     * but before the caller can act on the return value.
     *
     * @return True if the transaction name has already been set, else false.
     * @since 3.9.0
     */
    boolean isTransactionNameSet();

    /**
     * Gets the current transaction's name. The transaction name will include the full metric name with prefix
     * and category (e.g., "WebTransaction/Servlet/MyController" or "OtherTransaction/Custom/MyBackgroundJob").
     *
     * @return The full transaction name, or null if not set.
     * @since 9.0.0
     */
    String getTransactionName();

    /**
     * Returns this transaction's last tracer.
     *
     * @return deprecated
     * @since 3.9.0
     * @deprecated use {@link #getTracedMethod}.
     */
    @Deprecated
    TracedMethod getLastTracer();

    /**
     * Returns the {@link TracedMethod} enclosing the caller.
     *
     * @return The {@link TracedMethod} enclosing the caller. The return value is <code>null</code> if the caller is not
     * within a transaction.
     * @since 3.9.0
     */
    TracedMethod getTracedMethod();

    /**
     * Ignore this transaction so that none of its data is reported to the New Relic service.
     *
     * @since 3.9.0
     */
    void ignore();

    /**
     * Ignore the current transaction for calculating Apdex score.
     *
     * @since 3.9.0
     */
    void ignoreApdex();

    /**
     * Get transaction metadata to include in an outbound call. Use this method when implementing cross application
     * tracing support for a transport not supported by New Relic, such as a proprietary RPC transport.
     * <p>
     * Server A
     * <p>
     * <b>String requestMetadata = NewRelic.getAgent().getTransaction().getRequestMetadata();</b>
     * <p>
     * ...send requestMetadata to Server B as part of an outbound call.
     *
     * @return A string representation of the metadata required for linking this transaction to a remote child
     * transaction, or null if no metadata should be sent.
     * @since 3.16.1
     * @deprecated Instead, use the Distributed Tracing API {@link #insertDistributedTraceHeaders(Headers)} to create a
     * distributed tracing payload and {@link #acceptDistributedTraceHeaders(TransportType, Headers)} to link the services
     * together.
     */
    @Deprecated
    String getRequestMetadata();

    /**
     * Provide the metadata string from a remote transaction's call to {@link #getRequestMetadata()} to the current
     * transaction. Use this method when implementing cross application tracing support for a transport not supported
     * by New Relic, such as a proprietary RPC transport.
     *
     * <p>
     * Server B
     * <p>
     * ... get requestMetadata from request.
     * <p>
     * <b>NewRelic.getAgent().getTransaction().processRequestMetadata(requestMetadata);</b>
     * <p>
     *
     * @param requestMetadata metadata string received from an inbound request.
     * @since 3.16.1
     * @deprecated Instead, use the Distributed Tracing API {@link #insertDistributedTraceHeaders(Headers)} to create a
     * distributed tracing payload and {@link #acceptDistributedTraceHeaders(TransportType, Headers)} to link the services
     * together.
     */
    @Deprecated
    void processRequestMetadata(String requestMetadata);

    /**
     * Get transaction metadata to include in an outbound response. Use this method when implementing cross
     * application tracing support for a transport not supported by New Relic, such as a proprietary RPC transport.
     * <p>
     * <b>This call is time sensitive and should be made as close as possible to the code that writes the response.</b>
     *
     * <p>
     * Server B
     * <p>
     * <b>String responseMetadata = NewRelic.getAgent().getTransaction.getResponseMetadata();</b>
     * <p>
     * ... send response containing responseMetadata to server A.
     *
     * @return A string representation of the metadata required when responding to a remote transaction's call, or null
     * if no metadata should be sent.
     * @since 3.16.1
     * @deprecated Instead, use the Distributed Tracing API. There is no equivalent of this method in Distributed Tracing.
     */
    @Deprecated
    String getResponseMetadata();

    /**
     * Provide the metadata string from a remote transaction's call to {@link #getResponseMetadata()} to the current
     * transaction. This should only be called on the originating transaction when processing response to a remote call.
     * Use this method when implementing cross application tracing support for a transport not supported by New
     * Relic, such as a proprietary RPC transport.
     *
     * <p>
     * Server A
     * <p>
     * ... get response containing responseMetadata from call to server B.
     * <p>
     * <b>NewRelic.getAgent().getTransaction().processResponseMetadata(responseMetadata);</b>
     *
     * If a URI is available, please use {@link #processResponseMetadata(String, URI)}.
     *
     * @param responseMetadata metadata string from a remote transaction (generated by {@link #getResponseMetadata()}).
     * @since 3.16.1
     * @deprecated Instead, use the Distributed Tracing API. There is no equivalent of this method in Distributed Tracing.
     */
    @Deprecated
    void processResponseMetadata(String responseMetadata);

    /**
     * Provide the metadata string from a remote transaction's call to {@link #getResponseMetadata()} to the current
     * transaction. This should only be called on the originating transaction when processing response to a remote call.
     * Use this method when implementing cross application tracing support for a transport not supported by New
     * Relic, such as a proprietary RPC transport.
     *
     * <p>
     * Server A
     * <p>
     * ... get response containing responseMetadata from call to server B.
     * <p>
     * <b>NewRelic.getAgent().getTransaction().processResponseMetadata(responseMetadata, uri);</b>
     *
     * Note that the URI parameter should include a valid scheme, host, and port.
     *
     * @param responseMetadata metadata string from a remote transaction (generated by {@link #getResponseMetadata()}).
     * @param uri The external URI for the call.
     * @since 3.36.0
     * @deprecated Instead, use the Distributed Tracing API. There is no equivalent of this method in Distributed Tracing.
     */
    @Deprecated
    void processResponseMetadata(String responseMetadata, URI uri);

    /**
     * Sets the request for the current transaction. Setting the request will convert the current transaction into a web
     * transaction. Successive calls will have no effect (first wins). If this method is used, it's important to also
     * use {@link #setWebResponse} in order to capture information like the response code for this Transaction.
     *
     * @param request The current transaction's request.
     * @since 3.36.0
     */
    void setWebRequest(ExtendedRequest request);

    /**
     * Sets the response for the current transaction. Setting the response will convert the current transaction into a
     * web transaction. Successive calls will have no effect (first wins). If this method is used, it's important to
     * also use {@link #setWebRequest} in order to name the Transaction properly.
     *
     * @param response The current transaction's response.
     * @since 3.36.0
     */
    void setWebResponse(Response response);

    /**
     * Marks the time when the last byte of the response left the server as the current timestamp.
     * Successive calls will have no effect (first wins).
     *
     * @return True if the call to set the response time was successful
     * @since 3.36.0
     */
    boolean markResponseSent();

    /**
     * Returns true if in a web transaction.
     *
     * @return true if this is a web transaction, else false.
     * @since 3.36.0
     */
    boolean isWebTransaction();

    /**
     * Ignore throwable and http status code errors resulting from this transaction.
     *
     * @since 4.12.0
     */
    void ignoreErrors();

    /**
     * Turns the current transaction from a background transaction into a web transaction.
     *
     * @since 3.36.0
     */
    void convertToWebTransaction();

    /**
     * Adds headers to the external response so that the response can be recognized on the receiving end.
     *
     * Instruct the transaction to write the outbound response headers. This must be called before response headers are
     * sent and the response is committed. Successive calls will have no effect (first wins).
     *
     * This must be called after {@link #setWebRequest(ExtendedRequest)} and {@link #setWebResponse(Response)}, which
     * together provide the Agent with the inbound request headers and a place to record the outbound headers.
     *
     * @since 3.36.0
     */
    void addOutboundResponseHeaders();

    /**
     * Creates a {@link Token} for linking asynchronous work to the current {@link Transaction}. Subsequent calls to
     * getToken() will return a new Token.
     *
     * A single {@link Token} can be passed between multiple threads of work that should be linked to the transaction.
     * A transaction will remain open until all tokens are expired or timed out.
     *
     * If getToken() is called outside of a {@link Transaction} a NoOpToken will be returned which will always return
     * false when calling a method on the {@link Token} object.
     *
     * @return A token to pass to another thread with work for the current transaction.
     * @since 3.37.0
     */
    Token getToken();

    /**
     * Starts and returns a {@link Segment}. This {@link Segment} will show up in the Transaction Breakdown
     * table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
     * category by default. If you want to specify a custom category use {@link #startSegment(String, String)} instead.
     *
     * @param segmentName Name of the {@link Segment} segment in APM.
     * This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
     *
     * if null or an empty String, the agent will report "Unnamed Segment".
     * @return A started {@link Segment}.
     * @since 3.37.0
     */
    Segment startSegment(String segmentName);

    /**
     * Starts and returns a {@link Segment}. This {@link Segment} will show up in the Transaction Breakdown
     * table, as well as the Transaction Trace page.
     *
     * @param category Metric category that will be used for this segment. If null or an empty String, the agent will
     * report this Segment under the "Custom/" category.
     * @param segmentName Name of the {@link Segment} segment in APM.
     * This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
     *
     * if null or an empty String, the agent will report "Unnamed Segment".
     * @return A started {@link Segment}.
     * @since 3.37.0
     */
    Segment startSegment(String category, String segmentName);

    /**
     * Create a distributed trace payload. A {@link DistributedTracePayload} should be sent from one service to another
     * when you want to follow a trace throughout a distributed system. A {@link DistributedTracePayload} must be created
     * within an active New Relic {@link Transaction}.
     *
     * @return a {@link DistributedTracePayload}
     * @since 4.3.0
     * @deprecated Instead, use the Distributed Tracing API {@link #insertDistributedTraceHeaders(Headers)} to create a
     * distributed tracing payload and {@link #acceptDistributedTraceHeaders(TransportType, Headers)} to link the services
     * together.
     */
    @Deprecated
    DistributedTracePayload createDistributedTracePayload();

    /**
     * Accept a distributed trace payload. Accepting a {@link DistributedTracePayload} sent from one service to another
     * in a distributed system will result in those services being linked together in a trace of the system. A
     * {@link DistributedTracePayload} must be accepted within an active New Relic {@link Transaction}.
     *
     * @param payload a String representation of the {@link DistributedTracePayload} to accept
     * @since 4.3.0
     * @deprecated Instead, use the Distributed Tracing API {@link #insertDistributedTraceHeaders(Headers)} to create a
     * distributed tracing payload and {@link #acceptDistributedTraceHeaders(TransportType, Headers)} to link the services
     * together.
     */
    @Deprecated
    void acceptDistributedTracePayload(String payload);

    /**
     * Accept a distributed trace payload. Accepting a {@link DistributedTracePayload} sent from one service to another
     * in a distributed system will result in those services being linked together in a trace of the system.
     * {@link DistributedTracePayload} must be accepted within an active New Relic {@link Transaction}.
     *
     * @param payload a {@link DistributedTracePayload} instance to accept
     * @since 4.3.0
     * @deprecated Instead, use the Distributed Tracing API {@link #insertDistributedTraceHeaders(Headers)} to create a
     * distributed tracing payload and {@link #acceptDistributedTraceHeaders(TransportType, Headers)} to link the services
     * together.
     */
    @Deprecated
    void acceptDistributedTracePayload(DistributedTracePayload payload);

    /**
     * Generate distributed trace headers and insert them into the {@link Headers}. The header names inserted will depend
     * on the {@link Headers#getHeaderType()}.
     *
     * @param headers The headers to be updated with distributed trace header names and values.
     * @since 6.5.0
     */
    void insertDistributedTraceHeaders(Headers headers);

    /**
     * Accept the distributed trace headers. Accepting distributed trace headers sent from one service to another in a
     * distributed system will result in those services being linked together in a trace of the system. Distributed trace
     * payloads must be accept within an active New Relic {@link Transaction}. The header names accepted will depend on
     * the {@link Headers#getHeaderType()}.
     *
     * @param transportType The transport type of headers being accepted.
     * @param headers The headers to be accepted.
     * @since 6.5.0
     */
    void acceptDistributedTraceHeaders(TransportType transportType, Headers headers);

    /**
     * Returns the associated security related metadata from this Transaction.
     * @return securityMetaData object associated with this transaction.
     */
    Object getSecurityMetaData();

}
