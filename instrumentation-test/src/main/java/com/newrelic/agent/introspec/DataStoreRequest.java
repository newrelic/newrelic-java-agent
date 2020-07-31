/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

/**
 * Represents a data store request made and traced.
 */
public interface DataStoreRequest {
    String getDatastore();

    String getOperation();

    String getTable();

    int getCount();
}