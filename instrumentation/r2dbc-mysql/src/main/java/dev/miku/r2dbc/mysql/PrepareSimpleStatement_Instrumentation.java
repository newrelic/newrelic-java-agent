package dev.miku.r2dbc.mysql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import dev.miku.r2dbc.mysql.client.R2dbcUtils;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "dev.miku.r2dbc.mysql.PrepareSimpleStatement")
final class PrepareSimpleStatement_Instrumentation extends SimpleStatementSupport_Instrumentation {
    public Flux<MySqlResult> execute() {
        Flux<MySqlResult> request = Weaver.callOriginal();
        if(request != null && this.sql != null && this.client != null) {
            return R2dbcUtils.wrapRequest(request, this.sql, this.client);
        }
        return request;
    }
}
