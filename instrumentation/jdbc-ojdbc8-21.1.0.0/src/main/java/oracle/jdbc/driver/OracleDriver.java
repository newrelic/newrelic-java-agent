/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package oracle.jdbc.driver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.oracle.OracleDatabaseVendor;

@Weave
public abstract class OracleDriver {

    @Trace(leaf = true, excludeFromTransactionTrace = true)
    public Connection connect(String url, Properties props) throws SQLException {
        JdbcHelper.putVendor(getClass(), OracleDatabaseVendor.INSTANCE);
        Connection connection = Weaver.callOriginal();

        AgentBridge.getAgent().getLogger().log(Level.INFO, "ORACELSQL OracelDriver::connect - url: " + url);
        AgentBridge.getAgent().getLogger().log(Level.INFO, "ORACELSQL OracelDriver::connect - connection: " + connection);

        try {
            // DUF--
            if (connection != null && !JdbcHelper.databaseNameExists(connection)) {
                AgentBridge.getAgent().getLogger().log(Level.INFO, "ORACELSQL OracelDriver::connect - databaseNameExists?: " + JdbcHelper.databaseNameExists(connection));
                // We want to query instance_name here and not db_name because a db_name can be shared between instances
                PreparedStatement databaseNameQuery =
                        connection.prepareStatement("select sys_context('userenv','instance_name') from dual");

                ResultSet result = databaseNameQuery.executeQuery();
                AgentBridge.getAgent().getLogger().log(Level.INFO, "ORACELSQL OracelDriver::connect - resultSet: " + result);
                if (result != null && result.next()) {
                    String databaseName = result.getString(1);
                    AgentBridge.getAgent().getLogger().log(Level.INFO, "ORACELSQL OracelDriver::connect - dbName: " + databaseName);
                    if (databaseName != null) {
                        JdbcHelper.putDatabaseName(url, databaseName);
                    }
                }
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to capture database name for Oracle");
        }
        return connection;
    }
    
}
