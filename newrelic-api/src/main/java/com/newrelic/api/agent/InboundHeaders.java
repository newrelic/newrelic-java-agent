/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * The type-specific headers collection of an inbound message.
 */
public interface InboundHeaders {

    /**
     * Return the type of header key syntax used for this.
     * 
     * @return An <code>enum</code> specifying the type of headers present.
     * @since 3.5.0
     */
    HeaderType getHeaderType();

    /**
     * Returns the value of the specified request header as a <code>String</code>. If the request does not include a
     * header with the specified input name, then this method returns <code>null</code>.
     * 
     * @param name The name of the desired request header.
     * @return A <code>String</code> containing the value of the specified input request header, or <code>null</code> if the request header is not present.
     * @since 3.5.0
     */
    String getHeader(String name);
}
