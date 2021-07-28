package graphql;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import graphql.schema.DataFetchingEnvironment;

@Weave(originalName = "graphql.schema.DataFetcher", type = MatchType.Interface)
public class DataFetcher_Instrumentation<T> {
    @Trace
    public T get(DataFetchingEnvironment environment) {
        //todo: from environment, create String of "<parentType.field>" and setMetricName with it
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/resolve/<parenttype>.<field>");
        return Weaver.callOriginal();
    }
}
