/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.vertx.sqlclient.impl;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;

import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "io.vertx.sqlclient.impl.SqlClientBase")
public abstract class SqlClientBase_Instrumentation {
    @Trace
    public abstract Query<RowSet<Row>> query(String sql);

    @Weave(type = MatchType.ExactClass, originalName = "io.vertx.sqlclient.impl.SqlClientBase$QueryImpl")
    private static abstract class QueryImpl_Instrumentation<T, R extends SqlResult<T>>  {
        @Trace
        public abstract void execute(Handler<AsyncResult<R>> handler);
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.vertx.sqlclient.impl.SqlClientBase$PreparedQueryImpl")
    private static abstract class PreparedQueryImpl_Instrumentation<T, R extends SqlResult<T>>  {
        @Trace
        public abstract void execute(Tuple arguments, Handler<AsyncResult<R>> handler);

        @Trace
        public abstract void executeBatch(List<Tuple> batch, Handler<AsyncResult<R>> handler);
    }
}
