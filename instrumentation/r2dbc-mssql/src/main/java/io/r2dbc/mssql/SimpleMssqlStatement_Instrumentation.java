package io.r2dbc.mssql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.r2dbc.mssql.client.R2dbcUtils;
import io.r2dbc.mssql.client.Client;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.mssql.SimpleMssqlStatement")
final class SimpleMssqlStatement_Instrumentation {
    private final Client client = Weaver.callOriginal();
    private final String sql = Weaver.callOriginal();

    public Flux<MssqlResult> execute() {
        Flux<MssqlResult> request = Weaver.callOriginal();
        if(request != null && this.sql != null && this.client != null) {
            return R2dbcUtils.wrapRequest(request, this.sql, this.client);
        }
        return request;
    }
}

