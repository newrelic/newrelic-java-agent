/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.text.MessageFormat;
import java.util.Arrays;

public class DatabaseHelper {

    private static final String DATABASE_CONNECTION_URL = "jdbc:derby:memory:{0}{1}";
    private static final String DATABASE_PREFIX = "test_database_";
    public static final String DERBY_DATABASE_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    private static Connection connection;

    public static void initDatabase(Class<?> testClass) throws Exception {
        Class.forName(DERBY_DATABASE_DRIVER);
        connection = DriverManager.getConnection(getConnectionUrl(testClass) + ";create=true");

        for (String name : Arrays.asList("test", "test2", "test3", "test4")) {
            createTestTable(connection, name);
        }

        connection.createStatement().execute("select * from test");
    }

    public static String getConnectionUrl(Class<?> testClass) {
        return MessageFormat.format(DATABASE_CONNECTION_URL, DATABASE_PREFIX, testClass.getName());
    }

    private static void createTestTable(Connection connection, String name) throws SQLException {
        try {
            connection.createStatement().execute("drop table " + name);
        } catch (Exception ex) {
            // ignore
        }
        connection.createStatement().execute("create table " + name + "(id int, name varchar(255))");
    }

    public static void shutdownDatabase(Class<?> testClass) {
        try {
            DriverManager.getConnection(getConnectionUrl(testClass) + ";shutdown=true");
        } catch (SQLNonTransientConnectionException ignored) {
            // this exception is expected; it's how embedded derby tells you the connection is no longer valid.
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}
