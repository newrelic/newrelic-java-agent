/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * The input parameters required to report a slow datastore query on the {@link TracedMethod} including a correlating
 * input query. Do not use this class directly. Instead use {@link DatastoreParameters}.
 *
 * @since 3.36.0
 */
public class SlowQueryWithInputDatastoreParameters<T, I> extends SlowQueryDatastoreParameters<T> {

    /**
     * The label used to display this input query in the UI. For example, "Hibernate HQL"
     */
    private final String inputQueryLabel;
    /**
     * The raw input query object used for transforming into a raw input query String and obfuscated input query String
     */
    private final I rawInputQuery;
    /**
     * A converter to transform the rawInputQuery into a raw input query String and obfuscated input query String
     */
    private final QueryConverter<I> rawInputQueryConverter;

    protected SlowQueryWithInputDatastoreParameters(SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters,
            String inputQueryLabel, I rawInputQuery, QueryConverter<I> rawInputQueryConverter) {
        super(slowQueryDatastoreParameters);
        this.inputQueryLabel = inputQueryLabel;
        this.rawInputQuery = rawInputQuery;
        this.rawInputQueryConverter = rawInputQueryConverter;
    }

    /**
     * Returns the label denoting the type of input query.
     *
     * @return the label for this input query
     * @since 3.36.0
     */
    public String getInputQueryLabel() {
        return inputQueryLabel;
    }

    /**
     * Returns the raw input query object used for processing.
     *
     * @return raw input query object
     * @since 3.36.0
     */
    public I getRawInputQuery() {
        return rawInputQuery;
    }

    /**
     * Returns the converter implementation used to transform the raw input query into a string
     *
     * @return input query converter implementation
     * @since 3.36.0
     */
    public QueryConverter<I> getRawInputQueryConverter() {
        return rawInputQueryConverter;
    }

}
