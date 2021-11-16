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
     * The query object used for transforming into a raw query String and obfuscated query String
     */
    private final T rawQuery;

    /**
     * A converter to transform the rawQuery into a raw query String and obfuscated query String
     */
    private final QueryConverter<T> queryConverter;

    protected SlowQueryDatastoreParameters(DatastoreParameters datastoreParameters, T rawQuery,
            QueryConverter<T> queryConverter) {
        super(datastoreParameters);
        this.rawQuery = rawQuery;
        this.queryConverter = queryConverter;
    }

    protected SlowQueryDatastoreParameters(SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters) {
        super(slowQueryDatastoreParameters);
        this.rawQuery = slowQueryDatastoreParameters.rawQuery;
        this.queryConverter = slowQueryDatastoreParameters.queryConverter;
    }

    /**
     * Returns the query object used for processing. This query might be raw with parameters or obfuscated.
     *
     * @return query object
     * @since 3.36.0
     */
    public T getQuery() {
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

}
