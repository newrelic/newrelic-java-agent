/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * The input parameters required to report a slow datastore query on the {@link TracedMethod}. Do not use this class
 * directly. Instead use {@link DatastoreParameters}.
 *
 * @since 3.36.0
 */
public class SlowQueryDatastoreParameters<T> extends DatastoreParameters {

    /**
     * The raw query object used for transforming into a raw query String and obfuscated query String
     */
    private final T rawQuery;

    /**
     * The hash of the normalized query string. This is used to link queries from
     * the Query Management Platform (QMP) to APM.
     */
    private final String normalizedSqlHash;

    /**
     * A converter to transform the rawQuery into a raw query String and obfuscated query String
     */
    private final QueryConverter<T> queryConverter;

    protected SlowQueryDatastoreParameters(DatastoreParameters datastoreParameters, T rawQuery,
            QueryConverter<T> queryConverter, String normalizedSqlHash) {
        super(datastoreParameters);
        this.rawQuery = rawQuery;
        this.queryConverter = queryConverter;
        this.normalizedSqlHash = normalizedSqlHash;
    }

    protected SlowQueryDatastoreParameters(SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters) {
        super(slowQueryDatastoreParameters);
        this.rawQuery = slowQueryDatastoreParameters.rawQuery;
        this.queryConverter = slowQueryDatastoreParameters.queryConverter;
        this.normalizedSqlHash = slowQueryDatastoreParameters.getNormalizedSqlHash();
    }

    /**
     * Returns the raw query object used for processing.
     *
     * @return raw query object
     * @since 3.36.0
     */
    public T getRawQuery() {
        return rawQuery;
    }

    /**
     * Returns the converter implementation used to transform the raw query into a string
     *
     * @return query converter implementation
     * @since 3.36.0
     */
    public QueryConverter<T> getQueryConverter() {
        return queryConverter;
    }

    /**
     * Returns the hash of the normalized SQL statement
     *
     * @return hash value
     */
    public String getNormalizedSqlHash() {
        return normalizedSqlHash;
    }
}
