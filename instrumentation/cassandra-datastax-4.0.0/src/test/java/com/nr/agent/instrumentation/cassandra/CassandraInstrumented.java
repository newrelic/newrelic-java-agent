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
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.test.marker.IBMJ9IncompatibleTest;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java21IncompatibleTest;
import com.newrelic.test.marker.Java23IncompatibleTest;
import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// Issue when running cassandra unit on Java 9+ - https://github.com/jsevellec/cassandra-unit/issues/249
@Category({ IBMJ9IncompatibleTest.class, Java11IncompatibleTest.class, Java17IncompatibleTest.class, Java21IncompatibleTest.class, Java23IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.datastax.oss.driver" })
public class CassandraInstrumented {

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
        helper.assertScopedStatementMetricCount(transactionName, "INSERT", "users", 1);
        helper.assertScopedStatementMetricCount(transactionName, "SELECT", "users", 3);
        helper.assertScopedStatementMetricCount(transactionName, "UPDATE", "users", 1);
        helper.assertScopedStatementMetricCount(transactionName, "DELETE", "users", 1);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("INSERT", 1);
        helper.assertUnscopedOperationMetricCount("SELECT", 3);
        helper.assertUnscopedOperationMetricCount("UPDATE", 1);
        helper.assertUnscopedOperationMetricCount("DELETE", 1);
        helper.assertUnscopedStatementMetricCount("INSERT", "users", 1);
        helper.assertUnscopedStatementMetricCount("SELECT", "users", 3);
        helper.assertUnscopedStatementMetricCount("UPDATE", "users", 1);
        helper.assertUnscopedStatementMetricCount("DELETE", "users", 1);
    }

    @Test
    public void testSyncStatementRequests() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        DatastoreHelper helper = new DatastoreHelper(DatastoreVendor.Cassandra.toString());

        //When
        CassandraTestUtils.statementRequests(cassandra.session);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getTransactionNames().size());
        String transactionName = introspector.getTransactionNames().stream().findFirst().orElse("");
        helper.assertScopedStatementMetricCount(transactionName, "INSERT", "users", 1);
        helper.assertScopedStatementMetricCount(transactionName, "SELECT", "users", 3);
        helper.assertScopedStatementMetricCount(transactionName, "UPDATE", "users", 1);
        helper.assertScopedStatementMetricCount(transactionName, "DELETE", "users", 1);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("INSERT", 1);
        helper.assertUnscopedOperationMetricCount("SELECT", 3);
        helper.assertUnscopedOperationMetricCount("UPDATE", 1);
        helper.assertUnscopedOperationMetricCount("DELETE", 1);
        helper.assertUnscopedStatementMetricCount("INSERT", "users", 1);
        helper.assertUnscopedStatementMetricCount("SELECT", "users", 3);
        helper.assertUnscopedStatementMetricCount("UPDATE", "users", 1);
        helper.assertUnscopedStatementMetricCount("DELETE", "users", 1);
    }

    @Test
    public void testSyncBatchStatementRequests() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        DatastoreHelper helper = new DatastoreHelper(DatastoreVendor.Cassandra.toString());

        //When
        CassandraTestUtils.batchStatementRequests(cassandra.session);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getTransactionNames().size());
        String transactionName = introspector.getTransactionNames().stream().findFirst().orElse("");
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(transactionName);
        List<TraceSegment> traceSegments = traces.stream()
                .findFirst()
                .map(TransactionTrace::getInitialTraceSegment)
                .map(TraceSegment::getChildren)
                .orElse(Collections.emptyList());
        helper.assertScopedOperationMetricCount(transactionName, "BATCH", 1);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("BATCH", 1);
        assertEquals(1, traces.size());
        assertEquals(1, traceSegments.size());
        traceSegments.stream().map(TraceSegment::getTracerAttributes).forEach(x -> {
            assertNotNull(x.get("host"));
            assertNotNull(x.get("port_path_or_id"));
            assertNotNull(x.get("db.instance"));
        });
    }

    @Test
    public void testSyncRequestError() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        //When
        CassandraTestUtils.syncRequestError(cassandra.session);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getErrors().size());
        assertEquals(1, introspector.getErrorEvents().size());
    }

    @Test
    public void testAsyncBasicRequests() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        DatastoreHelper helper = new DatastoreHelper(DatastoreVendor.Cassandra.toString());

        //When
        CassandraTestUtils.asyncBasicRequests(cassandra.session);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getTransactionNames().size());
        String transactionName = introspector.getTransactionNames().stream().findFirst().orElse("");
        helper.assertScopedStatementMetricCount(transactionName, "SELECT", "users", 2);
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("SELECT", 2);
        helper.assertUnscopedStatementMetricCount("SELECT", "users", 2);
    }

    @Test
    public void testAsyncRequestError() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        //When
        CassandraTestUtils.asyncRequestError(cassandra.session);

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getErrors().size());
        assertEquals(1, introspector.getErrorEvents().size());
    }
}
