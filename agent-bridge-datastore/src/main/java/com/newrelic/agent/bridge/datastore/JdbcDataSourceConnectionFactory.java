/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.sql.DataSource;

import com.newrelic.agent.bridge.AgentBridge;

public class JdbcDataSourceConnectionFactory implements ConnectionFactory {

    private final DatabaseVendor databaseVendor;
    private final DataSource dataSource;
    private final String username;
    private final String password;

    public JdbcDataSourceConnectionFactory(DatabaseVendor databaseVendor, DataSource dataSource) {
        this(databaseVendor, dataSource, null, null);
    }

    public JdbcDataSourceConnectionFactory(DatabaseVendor databaseVendor, DataSource dataSource, String username,
            String password) {
        this.databaseVendor = databaseVendor;
        this.dataSource = dataSource;
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            if (username == null || password == null) {
                return dataSource.getConnection();
            }
            return dataSource.getConnection(username, password);
        } catch (SQLException e) {
            logError();
            throw e;
        } catch (Exception e) {
            logError();
            throw new SQLException(e);
        }
    }

    @Override
    public DatabaseVendor getDatabaseVendor() {
        return databaseVendor;
    }

    private void logError() {
        AgentBridge.getAgent().getLogger().log(
                Level.FINER, "An error occurred getting a database connection. DataSource: {0}", dataSource);
    }

}
