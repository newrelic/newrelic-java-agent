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
                operationAndTableName = R2dbcOperation.extractFrom(sql);
            }
        }

        return operationAndTableName;
    }
}
