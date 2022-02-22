package io.r2dbc.h2;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.r2dbc.R2dbcUtils;
import io.r2dbc.h2.client.Client;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "io.r2dbc.h2.H2Statement")
public class H2Statement {
    private final Client client = Weaver.callOriginal();
    private final String sql = Weaver.callOriginal();

    @Trace(leaf = true)
    public Flux<H2Result> execute() {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("execute");
        Flux<H2Result> request = Weaver.callOriginal();
        return R2dbcUtils.wrapRequest(request, client, sql, segment);
    }
}
