package graphql;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.CompletableFuture;

import static com.nr.instrumentation.graphql.GraphQLErrorHelper.maybeReportExecutionResultError;

@Weave(originalName = "graphql.GraphQL", type = MatchType.ExactClass)
public class GraphQL_Instrumentation {

    @Trace
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput){
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/executeAsync");
        CompletableFuture<ExecutionResult> executionResult = Weaver.callOriginal();
        if(executionResult != null && executionResult.isDone()){
            maybeReportExecutionResultError(executionResult);
        }
        return executionResult;
    }
}
