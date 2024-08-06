/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.vertx.pgclient.impl;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.OperationAndTableName;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.sqlclient.instrumentation.NRSqlClientWrapper;
import com.nr.vertx.sqlclient.instrumentation.SqlClientUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.impl.command.CommandBase;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "io.vertx.pgclient.impl.PgSocketConnection")
public abstract class PgSocketConnection_Instrumentation {
    @Trace
    protected <R> void doSchedule(CommandBase<R> cmd, Handler<AsyncResult<R>> handler) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - SocketConnectionBase_Instrumentation.doSchedule");

        if (!(handler instanceof NRSqlClientWrapper)) {
            OperationAndTableName operationAndTableName = SqlClientUtils.extractSqlFromSqlClientCommand(cmd);
            if (operationAndTableName != null) {
                PgConnectOptions pgConnectOptions = connectOptions();
                Segment segment = NewRelic.getAgent().getTransaction().startSegment("Query");

                NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - SocketConnectionBase_Instrumentation.doSchedule - segment: {0}", segment.toString());

                DatastoreParameters databaseParams = DatastoreParameters.product(DatastoreVendor.Postgres.name())
                        .collection(operationAndTableName.getTableName())
                        .operation(operationAndTableName.getOperation())
                        .instance(pgConnectOptions.getHost(), pgConnectOptions.getPort())
                        .databaseName(pgConnectOptions.getDatabase())
                        .build();

                handler = new NRSqlClientWrapper(handler, segment, databaseParams);
                NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - SocketConnectionBase_Instrumentation.doSchedule - wrapper");
            }

        }
        Weaver.callOriginal();
    }

    protected abstract PgConnectOptions connectOptions();
}
