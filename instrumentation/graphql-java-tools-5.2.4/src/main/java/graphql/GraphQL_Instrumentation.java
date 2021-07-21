package graphql;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.graphql.GraphQLInstrumentationUtil;
import graphql.execution.ExecutionContext;

import java.util.concurrent.CompletableFuture;

@Weave(originalName = "graphql.GraphQL", type = MatchType.ExactClass)
public class GraphQL_Instrumentation {

    @Trace
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        System.out.println("Weaving execute() query = [" + executionInput.getQuery() + "]");
//        util.instrumentExecutionContext((ExecutionContext) executionInput.getContext());
//        CompletableFuture<ExecutionResult> cfResult = Weaver.callOriginal();
//        if(cfResult != null) {
//            cfResult.thenAccept(result -> util.instrumentExecutionResult(result, executionInput));
//        }
//        return cfResult;
        return Weaver.callOriginal();
    }

}
