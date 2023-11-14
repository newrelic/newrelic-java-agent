/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.r2dbc.postgresql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.r2dbc.postgresql.api.PostgresqlResult;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.postgresql.PostgresqlStatement")
final class PostgresqlStatement_Instrumentation {
    private final ParsedSql parsedSql = Weaver.callOriginal();
    private final ConnectionResources resources = Weaver.callOriginal();

    public Flux<PostgresqlResult> execute() {
        Flux<PostgresqlResult> request = Weaver.callOriginal();
        if(request != null && parsedSql != null && resources != null) {
            return R2dbcUtils.wrapRequest(request, parsedSql.getSql(), resources.getClient(), resources.getConfiguration());
        }
        return request;
    }
}
