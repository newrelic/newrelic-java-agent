package io.asyncer.r2dbc.mysql;

import io.asyncer.r2dbc.mysql.api.MySqlResult;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.asyncer.r2dbc.mysql.client.R2dbcUtils;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "io.asyncer.r2dbc.mysql.TextSimpleStatement")
final class TextSimpleStatement_Instrumentation extends SimpleStatementSupport_Instrumentation {
    public Flux<MySqlResult> execute() {
        Flux<MySqlResult> request = Weaver.callOriginal();
        if(request != null && this.sql != null && this.client != null) {
            return R2dbcUtils.wrapRequest(request, this.sql, this.client);
        }
        return request;
    }
}
