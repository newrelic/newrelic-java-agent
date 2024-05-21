/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.api.agent.InboundHeaders;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.MessageConsumeParameters} instead.
 */
@Deprecated
public class MessageConsumeParameters extends com.newrelic.api.agent.MessageConsumeParameters implements ExternalParameters {

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.MessageConsumeParameters#MessageConsumeParameters} instead.
     *
     * @param library
     * @param destinationType
     * @param destinationName
     * @param inboundHeaders
     */
    @Deprecated
    protected MessageConsumeParameters(String library, DestinationType destinationType, String destinationName,
            InboundHeaders inboundHeaders) {
        super(library, destinationType.toApiDestinationType(), destinationName, inboundHeaders, null, null, null, null, null);
    }

}
