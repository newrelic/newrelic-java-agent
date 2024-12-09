package io.asyncer.r2dbc.mysql;

import io.asyncer.r2dbc.mysql.api.MySqlResult;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.asyncer.r2dbc.mysql.client.R2dbcUtils;
import reactor.core.publisher.Flux;

import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "io.asyncer.r2dbc.mysql.PrepareParameterizedStatement")
final class PrepareParameterizedStatement_Instrumentation extends ParameterizedStatementSupport_Instrumentation {
    public Flux<MySqlResult> execute(List<Binding> bindings) {
        Flux<MySqlResult> request = Weaver.callOriginal();
        if(request != null && this.query != null && this.client != null) {
            return R2dbcUtils.wrapRequest(request, query.getFormattedSql(), client);
        }
        return request;
    }
}
