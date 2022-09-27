/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.test.marker.Java10IncompatibleTest;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java12IncompatibleTest;
import com.newrelic.test.marker.Java13IncompatibleTest;
import com.newrelic.test.marker.Java14IncompatibleTest;
import com.newrelic.test.marker.Java15IncompatibleTest;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java18IncompatibleTest;
import com.newrelic.test.marker.Java19IncompatibleTest;
import com.newrelic.test.marker.Java9IncompatibleTest;
import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

// Issue when running cassandra unit on Java 9+ - https://github.com/jsevellec/cassandra-unit/issues/249
@Category({ Java9IncompatibleTest.class, Java10IncompatibleTest.class, Java11IncompatibleTest.class, Java12IncompatibleTest.class, Java13IncompatibleTest.class, Java14IncompatibleTest.class,
        Java15IncompatibleTest.class, Java16IncompatibleTest.class, Java17IncompatibleTest.class, Java18IncompatibleTest.class, Java19IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "none" })
public class CassandraNoInstrumentation {

    @Rule
    public CassandraCQLUnit cassandra = new CassandraCQLUnit(new ClassPathCQLDataSet("users.cql", "users"));

    @Test
    public void testSyncBasicRequests() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        DatastoreHelper helper = new DatastoreHelper(DatastoreVendor.Cassandra.toString());

        //When
        CassandraTestUtils.syncBasicRequests(cassandra.session);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getTransactionNames().size());
        String transactionName = introspector.getTransactionNames().stream().findFirst().orElse("");
        helper.assertScopedStatementMetricCount(transactionName, "INSERT", "users", 0);
        helper.assertScopedStatementMetricCount(transactionName, "SELECT", "users", 0);
        helper.assertScopedStatementMetricCount(transactionName, "UPDATE", "users", 0);
        helper.assertScopedStatementMetricCount(transactionName, "DELETE", "users", 0);
    }
}
