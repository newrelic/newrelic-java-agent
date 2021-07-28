package graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.CompletableFuture;

@Weave(originalName = "graphql.GraphQL", type = MatchType.ExactClass)
public class GraphQL_Instrumentation {

    @Trace
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput){
            /*We instrument executeAsync because it appears all executes eventually call here, plus it is public api.
    Unfortunately, the executionInput has the raw query string, unparsed. So, construction of the graphQL tx name from
    the raw is not desirable - we would need to custom parse it ourselves.

    This is why the setMetricName (which renames this tracer (and thus, the span) is  hardcoded and does not reflect
    the actual graphql query.
     */
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/executeAsync");

        //todo: Ideally, this tracer/span name does reflect the query. We could use the graphQL parser ourselves to get the Document?
        // Document document = Parser.parse(executionInput.getQuery())
        // This feels bad...Our instrumention would call the Parser on the query and then Graphql will repeat it again.

        /*Using the available agent Apis, this is how to add additional "agentAttributes" to this tracer.
        Although this works (tracer does get more agentAttributes), these attributes will not end up on the span
        created from this tracer.

        TracerToSpanEvent in the agent only copies agentAttributes to the span created from the root Tracer of a TX.
        */
        AgentBridge.privateApi.addTracerParameter("graphql.attribute", "addTracerParameter-graphql");

        return Weaver.callOriginal();
    }
}
