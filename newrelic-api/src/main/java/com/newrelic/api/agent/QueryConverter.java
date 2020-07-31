/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Callers of {@link DatastoreParameters} that use slowQuery() or slowQueryWithInput() must implement this interface
 * in order to properly record slow queries for their framework.
 *
 * Note: Implementations of this interface MUST guarantee thread-safety.
 *
 * @param <T> The raw query type
 * @since 3.36.0
 */
public interface QueryConverter<T> {

    /**
     * Takes a raw query object and returns it as a String representing the query. This method should return a value
     * that includes any parameter values (if possible).
     *
     * @param rawQuery the raw query object
     * @return the raw query object in String form
     */
    String toRawQueryString(T rawQuery);

    /**
     * Takes a raw query object and return it as a String representing an obfuscated form of the query. It is VERY
     * important that extra care be taken to properly obfuscate the raw query to prevent any potentially sensitive
     * information from leaking.
     * <p>
     * Sensitive information generally includes any changing values that the caller includes in the query. Replacement
     * patterns are the responsibility of the caller but SHOULD coalesce queries that only differ by parameter values
     * down a single unique String. For example, the following SQL queries will coalesce into a single query:
     * </p>
     * <ul>
     * <li>SELECT * FROM table1 WHERE parameter1 = 'value1';</li>
     * <li>SELECT * FROM table1 WHERE parameter1 = 'value2';</li>
     * </ul>
     * <p>
     * Result: SELECT * FROM table1 WHERE parameter1 = ?
     * </p>
     * <p>
     * NOTE: This method MUST return a different value than {@link #toRawQueryString(Object)} otherwise slow sql
     * processing will NOT occur.
     * </p>
     * @param rawQuery the raw query object
     * @return the obfuscated version of the raw query object in String form
     */
    String toObfuscatedQueryString(T rawQuery);

}
