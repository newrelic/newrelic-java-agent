/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.vertx.sqlclient.instrumentation;

import com.newrelic.agent.bridge.datastore.OperationAndTableName;
import com.newrelic.agent.bridge.datastore.R2dbcOperation;
import com.newrelic.api.agent.NewRelic;
import io.vertx.sqlclient.impl.command.CommandBase;
import io.vertx.sqlclient.impl.command.PrepareStatementCommand;
import io.vertx.sqlclient.impl.command.QueryCommandBase;

import java.util.logging.Level;

public class SqlClientUtils {
    public static <R> OperationAndTableName extractSqlFromSqlClientCommand(CommandBase<R> cmd) {
        OperationAndTableName operationAndTableName = null;
        String sql = null;

        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - extractSqlFromSqlClientCommand {0}", cmd);
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - extractSqlFromSqlClientCommand {0}", cmd instanceof QueryCommandBase);
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - extractSqlFromSqlClientCommand {0}", cmd instanceof PrepareStatementCommand);

        if (cmd != null) {
            if (cmd instanceof QueryCommandBase) {
                QueryCommandBase<?> qCmd = (QueryCommandBase<?>)cmd;
                sql = qCmd.sql();
            }
            if (cmd instanceof PrepareStatementCommand) {
                PrepareStatementCommand pCmd = (PrepareStatementCommand)cmd;
                sql = pCmd.sql();
            }

            if (sql != null) {
                NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - extractSqlFromSqlClientCommand {0}", sql);
                operationAndTableName = R2dbcOperation.extractFrom(sql);
            }
        }

        return operationAndTableName;
    }
}
