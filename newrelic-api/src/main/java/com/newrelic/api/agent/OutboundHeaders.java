/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * The type-specific headers collection of an outbound message
 */
public interface OutboundHeaders {

    /**
     * Return the type of header key syntax used for this.
     * 
     * @return An <code>enum</code> specifying the type of headers present.
     * @since 3.5.0
     */
    HeaderType getHeaderType();

    /**
     * Sets a response header with the given name and value.
     * 
     * @param name The name of the header.
     * @param value The value of the header.
     * @since 3.5.0
     */
    void setHeader(String name, String value);
}
