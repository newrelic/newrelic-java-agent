/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableSet;
import com.newrelic.api.agent.Headers;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.tracing.W3CTraceParentHeader;
import com.newrelic.agent.tracing.W3CTracePayload;
import com.newrelic.agent.tracing.W3CTraceStateHeader;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Utility methods for consistent processing message headers regardless whether they arrive by HTTP or JMS. The
 * underlying issue is that HTTP header strings (e.g. "X-Some-Header") are syntactically illegal in JMS because dashes
 * are not allowed there. So when given a headers collection, we need to ask its type so we can deduce the key used to
 * retrieve the header value. Presumably we may support additional protocols in the future.
 */
public class HeadersUtil {

    /**
     * The request header for storing our cross-process id in external calls.
     */
    public static final String NEWRELIC_ID_HEADER = "X-NewRelic-ID";
    public static final String NEWRELIC_ID_MESSAGE_HEADER = "NewRelicID";

    /**
     * The request header for storing our transaction id in external calls.
     */
    public static final String NEWRELIC_TRANSACTION_HEADER = "X-NewRelic-Transaction";
    public static final String NEWRELIC_TRANSACTION_MESSAGE_HEADER = "NewRelicTransaction";

    /**
     * The response header for returning transaction data to clients.
     */
    public static final String NEWRELIC_APP_DATA_HEADER = "X-NewRelic-App-Data";
    public static final String NEWRELIC_APP_DATA_MESSAGE_HEADER = "NewRelicAppData";

    /**
     * The request header for tracing a transaction created by New Relic Synthetics
     */
    public static final String NEWRELIC_SYNTHETICS_HEADER = "X-NewRelic-Synthetics";
    public static final String NEWRELIC_SYNTHETICS_MESSAGE_HEADER = "NewRelicSynthetics";

    /**
     * This request header contains additional information about the transaction
     * created by New Relic Synthetics
     */
    public static final String NEWRELIC_SYNTHETICS_INFO_HEADER = "X-NewRelic-Synthetics-Info";
    public static final String NEWRELIC_SYNTHETICS_INFO_MESSAGE_HEADER = "NewRelicSyntheticsInfo";

    /**
     * The request header for storing our distributed trace payloads.
     */
    public static final String NEWRELIC_TRACE_HEADER = "newrelic";
    private static final String NEWRELIC_TRACE_HEADER_UPPER = "NEWRELIC";
    private static final String NEWRELIC_TRACE_HEADER_FIRST_CAPITALIZED = "Newrelic";

    public static final String NEWRELIC_TRACE_MESSAGE_HEADER = "newrelic";

    /**
     * The request header for storing distributed trace baggage.
     */
    public static final String NEWRELIC_BAGGAGE_HEADER = "X-NewRelic-Baggage";

    /**
     * The request headers for storing w3c Trace Context distributed trace payloads.
     */
    static final String W3C_TRACESTATE_HEADER = "tracestate";
    private static final String W3C_TRACESTATE_HEADER_CAMEL = "TraceState";
    private static final String W3C_TRACESTATE_HEADER_CAPS = "TRACESTATE";

    static final String W3C_TRACEPARENT_HEADER = "traceparent";
    private static final String W3C_TRACEPARENT_HEADER_CAMEL = "TraceParent";
    private static final String W3C_TRACEPARENT_HEADER_CAPS = "TRACEPARENT";

    /**
     * Minimum supported version of New Relic Synthetics protocol.
     */
    public static final int SYNTHETICS_MIN_VERSION = 1;

    /**
     * Maximum supported version of New Relic Synthetics protocol.
     */
    public static final int SYNTHETICS_MAX_VERSION = 1;

    /**
     * Value that can never appear as a Synthetics protocol version.
     */
    public static final int SYNTHETICS_VERSION_NONE = -1;

    // Note - For now we do *not* want NEWRELIC_TRACE_* here because it is not an obfuscated header
    public static final Set<String> NEWRELIC_HEADERS = ImmutableSet.of(NEWRELIC_ID_HEADER, NEWRELIC_ID_MESSAGE_HEADER,
            NEWRELIC_TRANSACTION_HEADER, NEWRELIC_TRANSACTION_MESSAGE_HEADER, NEWRELIC_APP_DATA_HEADER,
            NEWRELIC_APP_DATA_MESSAGE_HEADER, NEWRELIC_SYNTHETICS_HEADER, NEWRELIC_SYNTHETICS_MESSAGE_HEADER,
            NEWRELIC_SYNTHETICS_INFO_HEADER, NEWRELIC_SYNTHETICS_INFO_MESSAGE_HEADER);

    public static String getIdHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_ID_HEADER, NEWRELIC_ID_MESSAGE_HEADER);
        return (key == null) ? null : headers.getHeader(key);
    }

    public static String getTransactionHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_TRANSACTION_HEADER,
                NEWRELIC_TRANSACTION_MESSAGE_HEADER);
        return (key == null) ? null : headers.getHeader(key);
    }

    public static String getAppDataHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_APP_DATA_HEADER,
                NEWRELIC_APP_DATA_MESSAGE_HEADER);
        return (key == null) ? null : headers.getHeader(key);
    }

    public static String getSyntheticsHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_SYNTHETICS_HEADER,
                NEWRELIC_SYNTHETICS_MESSAGE_HEADER);
        return (key == null) ? null : headers.getHeader(key);
    }

    public static String getSyntheticsInfoHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_SYNTHETICS_INFO_HEADER,
                NEWRELIC_SYNTHETICS_INFO_MESSAGE_HEADER);
        return (key == null) ? null : headers.getHeader(key);
    }

    public static String getNewRelicTraceHeader(InboundHeaders headers) {
        // Receiving app should check three different kinds of headers keys cases: lowercase, uppercase, first letter capitalized
        String lowercase = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_TRACE_HEADER, NEWRELIC_TRACE_MESSAGE_HEADER);
        if (lowercase != null) {
            String value = headers.getHeader(lowercase);
            if (value != null) {
                return value;
            }
        }

        String uppercase = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_TRACE_HEADER_UPPER, NEWRELIC_TRACE_MESSAGE_HEADER);
        if (uppercase != null) {
            String value = headers.getHeader(uppercase);
            if (value != null) {
                return value;
            }
        }

        String firstUppercase = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_TRACE_HEADER_FIRST_CAPITALIZED, NEWRELIC_TRACE_MESSAGE_HEADER);
        if (firstUppercase != null) {
            String value = headers.getHeader(firstUppercase);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    static List<String> getTraceParentHeader(InboundHeaders inboundHeaders) {
        return getHeadersTryingDifferentCases(inboundHeaders, W3C_TRACEPARENT_HEADER, W3C_TRACEPARENT_HEADER_CAMEL, W3C_TRACEPARENT_HEADER_CAPS);
    }

    static List<String> getTraceStateHeader(InboundHeaders inboundHeaders) {
        return getHeadersTryingDifferentCases(inboundHeaders, W3C_TRACESTATE_HEADER, W3C_TRACESTATE_HEADER_CAMEL, W3C_TRACESTATE_HEADER_CAPS);
    }

    private static List<String> getHeadersTryingDifferentCases(InboundHeaders inboundHeaders, String... caseVariants) {
        for (String variant : caseVariants) {
            List<String> headers = getHeaders(inboundHeaders, variant);
            if (headers != null) {
                return headers;
            }
        }

        return null;
    }

    private static List<String> getHeaders(InboundHeaders inboundHeaders, String key) {
        if (inboundHeaders instanceof ExtendedInboundHeaders) {
            return ((ExtendedInboundHeaders) inboundHeaders).getHeaders(key);
        }
        if (inboundHeaders instanceof Headers) {
            return new ArrayList<>(((Headers) inboundHeaders).getHeaders(key));
        }

        String header = inboundHeaders.getHeader(key);
        return header != null ? Collections.singletonList(header) : null;
    }

    /**
     * HeaderWrapper utility methods.
     */

    public static void setIdHeader(OutboundHeaders headers, String crossProcessId) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_ID_HEADER, NEWRELIC_ID_MESSAGE_HEADER);
        headers.setHeader(key, crossProcessId);
    }

    public static void setTransactionHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_TRANSACTION_HEADER,
                NEWRELIC_TRANSACTION_MESSAGE_HEADER);
        headers.setHeader(key, value);
    }

    public static void setAppDataHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_APP_DATA_HEADER,
                NEWRELIC_APP_DATA_MESSAGE_HEADER);
        headers.setHeader(key, value);
    }

    public static void setSyntheticsHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_SYNTHETICS_HEADER,
                NEWRELIC_SYNTHETICS_MESSAGE_HEADER);
        headers.setHeader(key, value);
    }

    public static void setSyntheticsInfoHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_SYNTHETICS_INFO_HEADER,
                NEWRELIC_SYNTHETICS_INFO_MESSAGE_HEADER);
        headers.setHeader(key, value);
    }

    public static void setNewRelicTraceHeader(OutboundHeaders headers, String value) {
        headers.setHeader(NEWRELIC_TRACE_HEADER, value);
    }

    public static void setTraceStateHeader(OutboundHeaders headers, String value) {
        headers.setHeader(W3C_TRACESTATE_HEADER, value);
    }

    public static void setTraceParentHeader(OutboundHeaders headers, String value) {
        headers.setHeader(W3C_TRACEPARENT_HEADER, value);
    }

    /**
     * parse headers from the inbound payload. It prioritizes trace context headers over newrelic headers. It then accepts the distributed
     * trace payload if it was able to parse it from the headers.
     *
     * @param tx             current transaction
     * @param inboundHeaders the request headers containing the distributed trace payload
     */
    public static void parseAndAcceptDistributedTraceHeaders(Transaction tx, InboundHeaders inboundHeaders) {
        List<String> traceParent = HeadersUtil.getTraceParentHeader(inboundHeaders);
        Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - traceParent: "+traceParent);
        for (String p : traceParent) {
            Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - tp: "+p);
        }
        if (traceParent != null && !traceParent.isEmpty()) {
            List<String> traceState = HeadersUtil.getTraceStateHeader(inboundHeaders);
            Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - traceState: "+traceState);
            for (String s : traceState) {
                Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - ts: "+s);
            }
            W3CTracePayload w3CTracePayload = W3CTracePayload.parseHeaders(tx, traceParent, traceState);
            Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - w3CTracePayload: "+w3CTracePayload);
            if (w3CTracePayload != null) {
                Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - w3CTracePayload.getPayload: "+w3CTracePayload.getPayload());
                if (w3CTracePayload.getPayload() != null) {
                    tx.acceptDistributedTracePayload(w3CTracePayload.getPayload(), w3CTracePayload.getTraceParent());
                }
                Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - w3CTracePayload.getTraceParent: "+w3CTracePayload.getTraceParent());
                if (w3CTracePayload.getTraceParent() != null) {
                    tx.getSpanProxy().setInitiatingW3CTraceParent(w3CTracePayload.getTraceParent());
                }
                Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - w3CTracePayload.getTraceState: "+w3CTracePayload.getTraceState());
                if (w3CTracePayload.getTraceState() != null) {
                    tx.getSpanProxy().setInitiatingW3CTraceState(w3CTracePayload.getTraceState());
                }
            }
        }
        else {
            String tracePayload = HeadersUtil.getNewRelicTraceHeader(inboundHeaders);
            Agent.LOG.log(Level.INFO, "EBSCOW3C - parseAndAcceptDistributedTraceHeaders - no traceParent, tracePayload: "+tracePayload);
            if (tracePayload != null) {
                tx.acceptDistributedTracePayload(tracePayload);
            }
        }
    }

    /**
     * creates new trace context distributed trace headers (and maybe new relic headers) and adds them to the headers object passed in
     *
     * @param tx           current transaction
     * @param tracedMethod the current traced method, used to grab the span id
     * @param headers      outbound headers where distributed trace headers will be added
     * @return true if the headers were successfully added, false otherwise
     */
    public static boolean createAndSetDistributedTraceHeaders(Transaction tx, com.newrelic.api.agent.TracedMethod tracedMethod, OutboundHeaders headers) {
        final String spanId = getSpanId(tx, tracedMethod);

        DistributedTracePayloadImpl payload = tx.createDistributedTracePayload(spanId);
        if (payload == null) {
            return false;
        }

        Agent.LOG.log(Level.FINER, "Sending distributed trace header in transaction {0}", tx);
        DistributedTracingConfig distributedTracingConfig = tx.getAgentConfig().getDistributedTracingConfig();
        boolean includeNewRelicHeader = distributedTracingConfig.isIncludeNewRelicHeader();
        if (includeNewRelicHeader) {
            HeadersUtil.setNewRelicTraceHeader(headers, payload.httpSafe());
        }

        try {
            SpanProxy spanProxy = tx.getSpanProxy();
            HeadersUtil.setTraceParentHeader(headers, W3CTraceParentHeader.create(spanProxy, payload.traceId, payload.guid, payload.sampled.booleanValue()));
            W3CTraceStateHeader traceStateHeader = new W3CTraceStateHeader(spanEventsEnabled(tx), transactionEventsEnabled(tx));
            String traceStateHeaderValue = traceStateHeader.create(spanProxy);
            HeadersUtil.setTraceStateHeader(headers, traceStateHeaderValue);
            tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_CREATE_SUCCESS);
        } catch (Exception e) {
            tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_CREATE_EXCEPTION);
        }
        return true;
    }

    private static String getSpanId(Transaction tx, com.newrelic.api.agent.TracedMethod tracedMethod) {
        if (tracedMethod instanceof DefaultTracer
                && spanEventsEnabled(tx)) {
            DefaultTracer tracer = (DefaultTracer) tracedMethod;
            return tracer.getGuid();
        }
        // Don't send a spanGuid if span events is disabled
        return null;
    }

    private static boolean spanEventsEnabled(Transaction tx) {
        return tx.getAgentConfig().getSpanEventsConfig().isEnabled();
    }

    private static boolean transactionEventsEnabled(Transaction tx) {
        return tx.getAgentConfig().getTransactionEventsConfig().isEnabled();
    }


    // Get the header key appropriate to the protocol (HTTP or MQ)
    private static String getTypedHeaderKey(HeaderType type, String httpHeader, String messageHeader) {
        if (type == null) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "null is not a valid header type");
            return null;
        }

        switch (type) {
            case MESSAGE:
                return messageHeader;
            case HTTP:
                return httpHeader;
            default:
                NewRelic.getAgent().getLogger().log(Level.FINE, "{0} is not a valid header type", type);
                return null;
        }
    }
}
