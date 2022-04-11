package dev.miku.r2dbc.mysql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import dev.miku.r2dbc.mysql.client.R2dbcUtils;
import reactor.core.publisher.Flux;

import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "dev.miku.r2dbc.mysql.TextParametrizedStatement")
final class TextParametrizedStatement_Instrumentation extends ParametrizedStatementSupport_Instrumentation {
    private final TextQuery query = Weaver.callOriginal();

    protected Flux<MySqlResult> execute(List<Binding> bindings) {
        Flux<MySqlResult> request = Weaver.callOriginal();
        if(request != null && this.query != null && this.client != null) {
            return R2dbcUtils.wrapRequest(request, String.join("", query.getSqlParts()), client);
        }
        return request;
    }
}
