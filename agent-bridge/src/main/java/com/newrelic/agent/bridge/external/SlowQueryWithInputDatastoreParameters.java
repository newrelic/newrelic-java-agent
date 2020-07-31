/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.agent.bridge.datastore.QueryConverter;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.SlowQueryWithInputDatastoreParameters} instead.
 *
 * The input parameters required to report a slow datastore query on the {@link TracedMethod} including a correlating
 * input query. Do not use this class directly. Instead use the {@link ExternalParametersFactory}.
 *
 * @since 3.27.0
 */
@Deprecated
public class SlowQueryWithInputDatastoreParameters<T, I> extends
        com.newrelic.api.agent.SlowQueryWithInputDatastoreParameters<T, I> implements ExternalParameters {

    protected SlowQueryWithInputDatastoreParameters(SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters,
            String inputQueryLabel, I rawInputQuery, QueryConverter<I> rawInputQueryConverter) {
        super(slowQueryDatastoreParameters, inputQueryLabel, rawInputQuery, rawInputQueryConverter);
    }

}
