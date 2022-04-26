package io.r2dbc.mssql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.r2dbc.mssql.client.Client;
import io.r2dbc.mssql.client.R2dbcUtils;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.mssql.ParametrizedMssqlStatement")
final class ParametrizedMssqlStatement_Instrumentation {
    private final Client client = Weaver.callOriginal();

    @NewField
    private final String sql;

    public Flux<MssqlResult> execute() {
        Flux<MssqlResult> request = Weaver.callOriginal();
        if (request != null && this.client != null) {
            return R2dbcUtils.wrapRequest(request, sql, client);
        }
        return request;
    }

    ParametrizedMssqlStatement_Instrumentation(Client client, ConnectionOptions connectionOptions, String sql) {
        this.sql = sql;
    }
}
