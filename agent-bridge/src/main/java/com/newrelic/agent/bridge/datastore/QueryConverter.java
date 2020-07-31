/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.QueryConverter} instead.
 *
 * Callers of {@link com.newrelic.agent.bridge.TracedMethod#reportQuery(Object, QueryConverter)} must implement this
 * interface in order to properly record slow queries for their framework.
 *
 * Note: Implementations of this interface MUST guarantee thread-safety.
 *
 * @param <T> The raw query type
 */
@Deprecated
public interface QueryConverter<T> extends com.newrelic.api.agent.QueryConverter<T> {
}
