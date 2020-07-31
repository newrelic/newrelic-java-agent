/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

import java.net.URI;

public interface CrossProcessState {

    /**
     * Add New Relic "X" headers to the request headers of outbound external requests.
     *
     * @param outboundHeaders
     */
    void processOutboundRequestHeaders(OutboundHeaders outboundHeaders);

    /**
     * Add New Relic "X" headers to the request headers of outbound external requests.
     *
     * @param outboundHeaders
     * @param tracedMethod
     */
    void processOutboundRequestHeaders(OutboundHeaders outboundHeaders,
                                       com.newrelic.api.agent.TracedMethod tracedMethod);

    /**
     * Add New Relic "X" headers to our response (to the request that initiated this transaction)
     * 
     * @param outboundHeaders
     * @param contentLength
     */
    void processOutboundResponseHeaders(OutboundHeaders outboundHeaders, long contentLength);

    /**
     * Process the "X-NewRelic-App-Data" header and generate External metrics
     * for response.
     * 
     * @param inboundHeaders
     * @param tracer
     * @param host
     * @param uri
     * @param addRollupMetric
     */
    void processInboundResponseHeaders(InboundHeaders inboundHeaders, TracedMethod tracer, String host, String uri,
            boolean addRollupMetric);

    /**
     * @return CAT and Synthetics encoded request metadata for outbound request.
     */
    String getRequestMetadata();

    /**
     * Process inbound request metadata.
     * 
     * @param requestMetadata
     */
    void processRequestMetadata(String requestMetadata);

    /**
     * 
     * @return CAT and Synthetics encoded response metadata for outbound response.
     */
    String getResponseMetadata();

    /**
     * Process inbound response metadata.
     *
     * @param responseMetadata
     * @param uri
     */
    void processResponseMetadata(String responseMetadata, URI uri);

}
