/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.api.agent.InboundHeaders;

import java.net.URI;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.HttpParameters} instead.
 *
 * Use to report an HTTP external call with cross application tracing.
 */
@Deprecated
public class HttpParameters extends com.newrelic.api.agent.HttpParameters implements ExternalParameters {

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.HttpParameters#HttpParameters} instead.
     *
     * @param library
     * @param uri
     * @param procedure
     * @param inboundResponseHeaders
     */
    @Deprecated
    protected HttpParameters(String library, URI uri, String procedure, InboundHeaders inboundResponseHeaders) {
        super(library, uri, procedure, inboundResponseHeaders);
    }

}

