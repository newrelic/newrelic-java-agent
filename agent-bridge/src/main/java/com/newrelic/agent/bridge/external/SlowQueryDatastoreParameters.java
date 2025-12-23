/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.agent.bridge.datastore.QueryConverter;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.SlowQueryDatastoreParameters} instead.
 *
 * The input parameters required to report a slow datastore query on the {@link TracedMethod}. Do not use this class
 * directly. Instead use the {@link ExternalParametersFactory}.
 *
 * @since 3.27.0
 */
@Deprecated
public class SlowQueryDatastoreParameters<T> extends com.newrelic.api.agent.SlowQueryDatastoreParameters<T> implements ExternalParameters {

    protected SlowQueryDatastoreParameters(DatastoreParameters datastoreParameters, T rawQuery, QueryConverter<T> queryConverter) {
        super(datastoreParameters, rawQuery, queryConverter, null);
    }

}
