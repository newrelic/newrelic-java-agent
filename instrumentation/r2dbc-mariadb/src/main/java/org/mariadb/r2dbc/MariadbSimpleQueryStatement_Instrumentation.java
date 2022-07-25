package org.mariadb.r2dbc;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.r2dbc.R2dbcUtils;
import org.mariadb.r2dbc.api.MariadbResult;
import org.mariadb.r2dbc.client.Client;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "org.mariadb.r2dbc.MariadbSimpleQueryStatement")
final class MariadbSimpleQueryStatement_Instrumentation {
    private final Client client = Weaver.callOriginal();
    private final String sql = Weaver.callOriginal();

    public Flux<MariadbResult> execute() {
        Flux<MariadbResult> request = Weaver.callOriginal();
        return R2dbcUtils.wrapRequest(request, sql, client);
    }
}
