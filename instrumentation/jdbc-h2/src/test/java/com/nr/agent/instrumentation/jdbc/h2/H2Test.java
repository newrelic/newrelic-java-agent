/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jdbc.h2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;

/**
 * This is a quick test to allow make sure the unit test framework works with the module testrunner.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.h2")
public class H2Test {
    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";
    private static final Connection CONNECTION = getDBConnection();

    @AfterClass
    public static void teardown() throws SQLException {
        CONNECTION.close();
    }

    private static Connection getDBConnection() {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
            return dbConnection;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dbConnection;
    }

    @Before
    public void initData() throws SQLException {
        // set up data in h2
        Statement stmt = CONNECTION.createStatement();
        stmt.execute(
                "CREATE TABLE IF NOT EXISTS USER(id int primary key, first_name varchar(255), last_name varchar(255))");
        stmt.execute("TRUNCATE TABLE USER");
        stmt.execute("INSERT INTO USER(id, first_name, last_name) VALUES(1, 'Fakus', 'Namus')");
        stmt.execute("INSERT INTO USER(id, first_name, last_name) VALUES(2, 'Some', 'Guy')");
        stmt.execute("INSERT INTO USER(id, first_name, last_name) VALUES(3, 'Whatsher', 'Name')");
        stmt.close();
    }

    @Test
    @Ignore
    public void testSelect() throws SQLException {
        jdbcTx();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount());

        DatastoreHelper helper = new DatastoreHelper(H2DatabaseVendor.INSTANCE.getDatastoreVendor().toString());
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("select", 1);
    }

    @Trace(dispatcher = true)
    private void jdbcTx() throws SQLException {
        Statement stmt = CONNECTION.createStatement();
        stmt.execute("select * from USER");
        stmt.close();
    }

}
