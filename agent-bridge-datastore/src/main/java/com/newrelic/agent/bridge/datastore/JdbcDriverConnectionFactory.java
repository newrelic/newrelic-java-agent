/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class JdbcDriverConnectionFactory implements ConnectionFactory {

    private static final Properties EMPTY_PROPERTIES = new Properties();

    private final WeakReference<DatabaseVendor> databaseVendor;
    private final WeakReference<Driver> driver;
    private final String url;
    private final Properties props;

    public JdbcDriverConnectionFactory(DatabaseVendor databaseVendor, Driver driver, String url, Properties props) {
        this.databaseVendor = new WeakReference<>(databaseVendor);
        this.driver = new WeakReference<>(driver);
        this.url = url;
        this.props = (props == null || props.isEmpty()) ? EMPTY_PROPERTIES : props;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            Driver jdbcDriver = driver.get();
            if (jdbcDriver != null) {
                return jdbcDriver.connect(url, props);
            }
            throw new RuntimeException("JDBC Driver has been Garbage Collected");
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
        DatabaseVendor vendor = databaseVendor.get();
        if (vendor != null) {
            return vendor;
        }
        return UnknownDatabaseVendor.INSTANCE;
    }

    private void logError() {
        AgentBridge.getAgent().getLogger().log(Level.FINER, "An error occurred getting a database connection. Driver: {0} url: {1}", driver, url);
    }

}
