package io.r2dbc.h2;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.r2dbc.R2dbcUtils;
import io.r2dbc.h2.client.Client;
import io.r2dbc.h2.client.SessionClient_Instrumentation;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.h2.H2Statement")
public class H2Statement_Instrumentation {
    private final Client client = Weaver.callOriginal();
    private final String sql = Weaver.callOriginal();

    public Flux<H2Result> execute() {
        Flux<H2Result> request = Weaver.callOriginal();
        if(client instanceof SessionClient_Instrumentation) {
            SessionClient_Instrumentation instrumentedClient = ((SessionClient_Instrumentation) client);
            return R2dbcUtils.wrapRequest(request, sql, instrumentedClient.databaseName, instrumentedClient.url);
        }
        return request;
    }
}
