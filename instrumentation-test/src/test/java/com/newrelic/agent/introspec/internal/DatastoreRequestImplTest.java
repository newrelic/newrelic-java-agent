/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import org.junit.Assert;
import org.junit.Test;

public class DatastoreRequestImplTest {

    @Test
    public void testDatastoreMerge() {
        DatastoreRequestImpl impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/operation/mysql/insert");
        Assert.assertNotNull(impl);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("mysql", impl.getDatastore());
        Assert.assertEquals("insert", impl.getOperation());
        Assert.assertNull(impl.getTable());

        DatastoreRequestImpl impl2 = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/operation/mysql/insert");
        Assert.assertTrue(impl.wasMerged(impl2));

        Assert.assertEquals(2, impl.getCount());
    }

    @Test
    public void testDatastore() {
        DatastoreRequestImpl impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/operation/m12!@#$%^&ysql/in,.,.,.sert");
        Assert.assertNotNull(impl);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("m12!@#$%^&ysql", impl.getDatastore());
        Assert.assertEquals("in,.,.,.sert", impl.getOperation());
        Assert.assertNull(impl.getTable());

        impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/statement/mysql/mytable/select");
        Assert.assertNotNull(impl);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("mysql", impl.getDatastore());
        Assert.assertEquals("select", impl.getOperation());
        Assert.assertEquals("mytable", impl.getTable());

        impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/statement/mysql!!~/myt1@#$%able/sel,.,.ect");
        Assert.assertNotNull(impl);
        Assert.assertEquals(1, impl.getCount());
        Assert.assertEquals("mysql!!~", impl.getDatastore());
        Assert.assertEquals("myt1@#$%able", impl.getTable());
        Assert.assertEquals("sel,.,.ect", impl.getOperation());

        impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/statement/sql/table/select/");
        Assert.assertNull(impl);

        impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/statement/sql/table/select////");
        Assert.assertNull(impl);

        impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/state/sql/table/select");
        Assert.assertNull(impl);

        impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/operation/sql/table/select");
        Assert.assertNull(impl);

        impl = DatastoreRequestImpl.checkAndMakeDatastore("Datastore/oper/sql/table/select");
        Assert.assertNull(impl);
    }

}
