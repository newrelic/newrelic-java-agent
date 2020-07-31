/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.SlowQueryDatastoreParameters;

import java.util.List;

/**
 * Implementations of this interface must be thread safe.
 */
public interface SlowQueryListener {

    /**
     * Evaluates whether or not the provided tracer is above the slow query threshold. If the query is above the
     * threshold then the slow query parameters on the DatastoreParameters object will be used to correctly capture and
     * record the required information.
     *
     * @param tracer the tracer to use when checking durations and storing query information
     * @param datastoreParameters contains the slow query parameters (if present) for recording slow queries
     */
    <T> void noticeTracer(Tracer tracer, SlowQueryDatastoreParameters<T> datastoreParameters);

    /**
     * Return a list of all captured slow queries.
     *
     * @return slow queries that are above the configured threshold
     */
    List<SlowQueryInfo> getSlowQueries();

}
