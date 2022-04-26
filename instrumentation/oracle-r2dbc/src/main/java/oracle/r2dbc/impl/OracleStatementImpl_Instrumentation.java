package oracle.r2dbc.impl;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.sql.Connection;

@Weave(type = MatchType.ExactClass, originalName = "oracle.r2dbc.impl.OracleStatementImpl")
final class OracleStatementImpl_Instrumentation {
    private final String sql = Weaver.callOriginal();

    private final Connection jdbcConnection = Weaver.callOriginal();

    public Publisher<OracleResultImpl> execute() {
        Flux<OracleResultImpl> request = Weaver.callOriginal();
        if(request != null && jdbcConnection != null) {
             return R2dbcUtils.wrapRequest(request, sql, jdbcConnection);
        }
        return request;
    }
}
