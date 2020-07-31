/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

/**
 * Provides consistent names to use for various datastore vendors.
 */
public enum DatastoreVendor {
    Cassandra, Derby, IBMDB2, JDBC, Memcache, MongoDB, MSSQL, MySQL, Neptune, Oracle, Postgres, Redis, JCache,
    H2, HSQLDB, Sybase, Solr, DynamoDB
}
