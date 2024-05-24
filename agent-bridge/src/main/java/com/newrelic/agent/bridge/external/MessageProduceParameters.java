/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.api.agent.OutboundHeaders;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.MessageProduceParameters} instead.
 */
@Deprecated
public class MessageProduceParameters extends com.newrelic.api.agent.MessageProduceParameters implements ExternalParameters {

    /**
     * @Deprecated Do not use. Use the fluent builder {@link com.newrelic.api.agent.MessageProduceParameters#library(String)} instead.
     *
     * @param library
     * @param destinationType
     * @param destinationName
     * @param outboundHeaders
     */
    @Deprecated
    protected MessageProduceParameters(String library, DestinationType destinationType, String destinationName,
            OutboundHeaders outboundHeaders) {
        super(library, destinationType.toApiDestinationType(), destinationName, outboundHeaders, null);
    }

}
