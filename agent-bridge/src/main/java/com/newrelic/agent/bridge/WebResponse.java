/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

/**
 * 
 * @see Transaction#getWebResponse()
 */
public interface WebResponse {

    /**
     * Set the web response status code.
     */
    void setStatus(int statusCode);

    /**
     * Get the web response status code.
     */
    int getStatus();

    /**
     * Set the web response status message.
     */
    void setStatusMessage(String message);

    /**
     * Get the web response status message.
     */
    String getStatusMessage();

    /**
     * Freeze the web response status code.
     */
    void freezeStatus();
}
