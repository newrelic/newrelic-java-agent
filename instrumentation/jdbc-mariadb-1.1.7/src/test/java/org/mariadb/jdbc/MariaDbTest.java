/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.mariadb.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"org.mariadb.jdbc", "java.sql", "javax.sql" })
@Ignore
/* Using an embedded mariaDB instance is incompatible with Ubuntu 22 and our goal of adding test converage of Java 8.
*  Todo: Use test containers as opposed to the mariaDB instance and remove the @Ignore annotation
*/
public class MariaDbTest {

    private static DB mariaDb;
    
    private static String connectionString;
    private static String dbName;

    @BeforeClass
    public static void setUpDb() throws Exception {
        DBConfigurationBuilder builder = DBConfigurationBuilder.newBuilder()
                .setPort(0); // This will automatically find a free port

        dbName = "MariaDB" + System.currentTimeMillis();
        mariaDb = DB.newEmbeddedDB(builder.build());
        connectionString = builder.getURL(dbName);
        mariaDb.start();

        mariaDb.createDB(dbName);
        mariaDb.source("maria-db-test.sql", null, null, dbName);
    }

    @AfterClass
    public static void tearDownDb() throws Exception {
        mariaDb.stop();
    }

    @Test
    @Ignore
    public void testPreparedStatementQuery() throws Exception {
        mariaDbPreparedStatementQuery();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount());
        DatastoreHelper helper = new DatastoreHelper("MySQL");
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("select", 1);
    }

    @Test
    @Ignore
    public void testCrud() throws Exception {
        mariaDbInsert();
        mariaDbReadInsert();
        mariaDbUpdate();
        mariaDbReadUpdate();
        mariaDbDelete();
        mariaDbReadDelete();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(6, introspector.getFinishedTransactionCount());

        DatastoreHelper helper = new DatastoreHelper("MySQL");
        helper.assertAggregateMetrics();
        helper.assertUnscopedOperationMetricCount("insert", 1); // C
        helper.assertUnscopedOperationMetricCount("select", 3); // R (once per step)
        helper.assertUnscopedOperationMetricCount("update", 1); // U
        helper.assertUnscopedOperationMetricCount("delete", 1); // D
    }

    @Trace(dispatcher = true)
    public void mariaDbPreparedStatementQuery() throws Exception {
        Connection connection = DriverManager.getConnection(connectionString, "root", "");
        PreparedStatement statement = connection.prepareStatement("SELECT id FROM testQuery WHERE value LIKE ?");
        statement.setString(1, "cool");
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            long value = resultSet.getLong(1);
            Assert.assertEquals(1, value);
        } else {
            Assert.fail("Unable to get any results from database");
        }
        connection.close();
    }

    @Trace(dispatcher = true)
    public void mariaDbInsert() throws Exception {
        Connection connection = DriverManager.getConnection(connectionString, "root", "");

        PreparedStatement statement = connection.prepareStatement("INSERT INTO testCrud (id, value) VALUES (1, ?)");
        statement.setString(1, "insert");
        int inserted = statement.executeUpdate();
        Assert.assertEquals(1, inserted); // Only 1 row to insert
        connection.close();
    }


    @Trace(dispatcher = true)
    public void mariaDbReadInsert() throws Exception {
        Connection connection = DriverManager.getConnection(connectionString, "root", "");
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT id FROM testCrud WHERE value = 'insert'");
        if (resultSet.next()) {
            long value = resultSet.getLong(1);
            Assert.assertEquals(1, value);
        } else {
            Assert.fail("Unable to find inserted row");
        }
        connection.close();
    }

    @Trace(dispatcher = true)
    public void mariaDbUpdate() throws Exception {
        Connection connection = DriverManager.getConnection(connectionString, "root", "");

        PreparedStatement statement = connection.prepareStatement("UPDATE testCrud SET value = ? WHERE id = ?");
        statement.setString(1, "update");
        statement.setInt(2, 1);
        int updated = statement.executeUpdate();
        Assert.assertEquals(1, updated); // Only 1 row to update
        connection.close();
    }

    @Trace(dispatcher = true)
    public void mariaDbReadUpdate() throws Exception {
        Connection connection = DriverManager.getConnection(connectionString, "root", "");
        Statement statement = connection.createStatement();
        statement.execute("SELECT value FROM testCrud WHERE id = 1");
        ResultSet resultSet = statement.getResultSet();
        if (resultSet.next()) {
            String value = resultSet.getString(1);
            Assert.assertEquals("update", value);
        } else {
            Assert.fail("Unable to find updated row");
        }
        connection.close();
    }

    @Trace(dispatcher = true)
    public void mariaDbDelete() throws Exception {
        Connection connection = DriverManager.getConnection(connectionString, "root", "");

        Statement statement = connection.createStatement();
        int updated = statement.executeUpdate("DELETE FROM testCrud WHERE id = 1");
        Assert.assertEquals(1, updated); // Only 1 row to remove
        connection.close();
    }

    @Trace(dispatcher = true)
    public void mariaDbReadDelete() throws Exception {
        Connection connection = DriverManager.getConnection(connectionString, "root", "");
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM testCrud");
        ResultSet resultSet = statement.executeQuery();
        Assert.assertFalse("We found a row when we didn't expect one", resultSet.next());
        connection.close();
    }
}
