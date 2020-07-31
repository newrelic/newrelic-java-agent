/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.DatastoreParameters} instead.
 *
 * The input parameters required to report a datastore on the {@link TracedMethod}. A fluent builder is provided to
 * allow for easy usage and management of this API.
 *
 * @since 3.26.0
 */
@Deprecated
public class DatastoreParameters extends com.newrelic.api.agent.DatastoreParameters implements ExternalParameters {

    DatastoreParameters(String product, String collection, String operation, String host, Integer port) {
        super(com.newrelic.api.agent.DatastoreParameters
                .product(product)
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .build());
    }

}
