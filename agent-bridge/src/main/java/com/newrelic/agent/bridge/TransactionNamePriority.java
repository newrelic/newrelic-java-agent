/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

/**
 * This is the internal version of TransactionNamePriority.
 */
public enum TransactionNamePriority {

    /**
     * The transaction is unnamed.
     */
    NONE(0, 0),
    /**
     * Use the request uri.
     */
    REQUEST_URI(1, 4),
    /**
     * Use the response status code.
     */
    STATUS_CODE(2, 5),
    /**
     * Use the name of a servlet Filter.
     */
    FILTER_NAME(3, 1),

    FILTER_INIT_PARAM(4, 6),
    /**
     * Use the name of a Servlet.
     */
    SERVLET_NAME(5, 2),

    SERVLET_INIT_PARAM(6, 7),
    /**
     * Use the name of a JSP.
     */
    JSP(7, 3),

    FRAMEWORK_LOW(8, 8),

    FRAMEWORK(9, 9),

    FRAMEWORK_HIGH(10, 10),
    /**
     * Use a custom transaction name (user defined).
     */
    CUSTOM_LOW(11, 11),
    /**
     * Use a custom transaction name (user defined).
     */
    CUSTOM_HIGH(12, 12),
    /**
     * The transaction name is frozed. Subsequent setTransaction calls will have no effect.
     */
    FROZEN(13, 13);

    public final int legacyPriority;
    public final int pathPriority;

    TransactionNamePriority(int legacyPriority, int pathPriority) {
        this.legacyPriority = legacyPriority;
        this.pathPriority = pathPriority;
    }

    /**
     * Converts the public api priority to the internal priority.
     * 
     * @param priority
     * @return
     */
    public static TransactionNamePriority convert(com.newrelic.api.agent.TransactionNamePriority priority) {
        switch (priority) {
        case CUSTOM_HIGH:
            return TransactionNamePriority.CUSTOM_HIGH;
        case CUSTOM_LOW:
            return TransactionNamePriority.CUSTOM_LOW;
        case FRAMEWORK_HIGH:
            return TransactionNamePriority.FRAMEWORK_HIGH;
        case FRAMEWORK_LOW:
            return TransactionNamePriority.FRAMEWORK_LOW;
        case REQUEST_URI:
            return TransactionNamePriority.REQUEST_URI;
        default:
            throw new IllegalArgumentException("Unmapped TransactionNamePriority " + priority);
        }
    }
}
