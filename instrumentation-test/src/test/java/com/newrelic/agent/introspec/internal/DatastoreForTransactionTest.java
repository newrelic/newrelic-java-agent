/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.DataStoreRequest;
import com.newrelic.agent.stats.TransactionStats;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class DatastoreForTransactionTest {

    @Test
    public void testHasDatastores() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Unscoped/other").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Another/another").recordResponseTime(10, TimeUnit.MILLISECONDS);

        Assert.assertFalse(DatastoreForTransaction.hasDatastores(stats));

        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        Assert.assertTrue(DatastoreForTransaction.hasDatastores(stats));
    }

    @Test
    public void testcheckDatastoresOne() {
        // web
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        Collection<DatastoreRequestImpl> actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(1, actual.size());
        DatastoreRequestImpl impl = actual.iterator().next();
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("mysql", impl.getDatastore());
        Assert.assertEquals("table", impl.getTable());
        Assert.assertEquals("select", impl.getOperation());

        // other
        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allOther").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allOther").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(0, actual.size());

        actual = DatastoreForTransaction.checkDatastores(false, stats);
        Assert.assertEquals(1, actual.size());
        impl = actual.iterator().next();
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("mysql", impl.getDatastore());
        Assert.assertEquals("table", impl.getTable());
        Assert.assertEquals("select", impl.getOperation());
    }

    @Test
    public void testcheckDatastoresTwo() {
        // web
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mongo/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mongo/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/operation/mongo/insert").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        Collection<DatastoreRequestImpl> actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(2, actual.size());
        Iterator<DatastoreRequestImpl> it = actual.iterator();
        while (it.hasNext()) {
            DatastoreRequestImpl impl = it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("Datastore/operation/mongo/insert")) {
                Assert.assertEquals("mongo", impl.getDatastore());
                Assert.assertEquals("insert", impl.getOperation());
            } else {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("table", impl.getTable());
                Assert.assertEquals("select", impl.getOperation());
            }
        }

        // other
        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allOther").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mongo/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mongo/allOther").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allOther").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allOther").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/operation/mongo/insert").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        actual = DatastoreForTransaction.checkDatastores(false, stats);
        Assert.assertEquals(2, actual.size());
        it = actual.iterator();
        while (it.hasNext()) {
            DatastoreRequestImpl impl = it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("Datastore/operation/mongo/insert")) {
                Assert.assertEquals("mongo", impl.getDatastore());
                Assert.assertEquals("insert", impl.getOperation());
            } else {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("table", impl.getTable());
                Assert.assertEquals("select", impl.getOperation());
            }
        }
    }

    @Test
    public void testcheckDatastoresSameHost() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/operation/mysql/insert").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        Collection<DatastoreRequestImpl> actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(2, actual.size());
        Iterator<DatastoreRequestImpl> it = actual.iterator();
        while (it.hasNext()) {
            DatastoreRequestImpl impl = it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("Datastore/operation/mysql/insert")) {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("insert", impl.getOperation());
            } else {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("table", impl.getTable());
                Assert.assertEquals("select", impl.getOperation());
            }
        }
    }

    @Test
    public void testcheckDatastoresInvalid() {
        TransactionStats stats = new TransactionStats();

        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        Collection<DatastoreRequestImpl> actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(0, actual.size());

        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(0, actual.size());

        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(0, actual.size());

        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        actual = DatastoreForTransaction.checkDatastores(true, stats);
        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void testAddDatastoresOneCall() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/operation/mysql/insert").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        DatastoreForTransaction exts = new DatastoreForTransaction();
        exts.addDatastore(true, stats);
        Collection<DataStoreRequest> actual = exts.getDatastores();
        Assert.assertEquals(2, actual.size());
        Iterator<DataStoreRequest> it = actual.iterator();
        while (it.hasNext()) {
            DatastoreRequestImpl impl = (DatastoreRequestImpl) it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("Datastore/operation/mysql/insert")) {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("insert", impl.getOperation());
            } else {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("table", impl.getTable());
                Assert.assertEquals("select", impl.getOperation());
            }
        }
    }

    @Test
    public void testAddDatastoresTwoCalls() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        DatastoreForTransaction exts = new DatastoreForTransaction();
        exts.addDatastore(true, stats);

        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/operation/mysql/insert").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        exts.addDatastore(true, stats);
        Collection<DataStoreRequest> actual = exts.getDatastores();
        Assert.assertEquals(2, actual.size());
        Iterator<DataStoreRequest> it = actual.iterator();
        while (it.hasNext()) {
            DatastoreRequestImpl impl = (DatastoreRequestImpl) it.next();
            Assert.assertEquals(1, impl.getCount());
            if (impl.getMetricName().equals("Datastore/operation/mysql/insert")) {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("insert", impl.getOperation());
            } else {
                Assert.assertEquals("mysql", impl.getDatastore());
                Assert.assertEquals("table", impl.getTable());
                Assert.assertEquals("select", impl.getOperation());
            }
        }
    }

    @Test
    public void testAddDatastoresTwoCallsSameMetrics() {
        TransactionStats stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        DatastoreForTransaction exts = new DatastoreForTransaction();
        exts.addDatastore(true, stats);

        stats = new TransactionStats();
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/all").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/allWeb").recordResponseTime(10, TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/all").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getUnscopedStats().getOrCreateResponseTimeStats("Datastore/mysql/allWeb").recordResponseTime(10,
                TimeUnit.MILLISECONDS);
        stats.getScopedStats().getOrCreateResponseTimeStats("Datastore/statement/mysql/table/select").recordResponseTime(10,
                TimeUnit.MILLISECONDS);

        exts.addDatastore(true, stats);
        Collection<DataStoreRequest> actual = exts.getDatastores();
        Assert.assertEquals(1, actual.size());
        Iterator<DataStoreRequest> it = actual.iterator();
        DatastoreRequestImpl impl = (DatastoreRequestImpl) it.next();
        Assert.assertEquals(2, impl.getCount());
    }

}
