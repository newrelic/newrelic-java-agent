package io.r2dbc.postgresql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.r2dbc.postgresql.api.PostgresqlResult;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.postgresql.PostgresqlStatement")
final class PostgresqlStatement_Instrumentation {
    private final TokenizedSql tokenizedSql = Weaver.callOriginal();
    private final ConnectionResources resources = Weaver.callOriginal();

    public Flux<PostgresqlResult> execute() {
        Flux<PostgresqlResult> request = Weaver.callOriginal();
        if(request != null && tokenizedSql != null && resources != null) {
             return R2dbcUtils.wrapRequest(request, tokenizedSql.getSql(), resources.getConfiguration());
        }
        return request;
    }
}
