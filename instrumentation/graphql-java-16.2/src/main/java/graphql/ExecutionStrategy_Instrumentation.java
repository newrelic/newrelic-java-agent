package graphql;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.graphql.GraphQLTransactionName;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.FieldValueInfo;
import graphql.language.Document;

import java.util.concurrent.CompletableFuture;
import static com.nr.instrumentation.graphql.GraphQLSpanUtil.*;

@Weave(originalName = "graphql.execution.ExecutionStrategy", type = MatchType.BaseClass)
public class ExecutionStrategy_Instrumentation {

    @Trace
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        Document document = executionContext.getDocument();
        String query = executionContext.getExecutionInput().getQuery();
        String transactionName = GraphQLTransactionName.from(document);
        NewRelic.setTransactionName("GraphQL", transactionName);
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/operation" + transactionName);
        setOperationAttributes(document, query);
        return Weaver.callOriginal();
    }

    @Trace
    protected CompletableFuture<FieldValueInfo> resolveFieldWithInfo(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {

        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/resolve/" + parameters.getPath().getSegmentName());
        setResolverAttributes(parameters);
        CompletableFuture<FieldValueInfo> resolveResult = Weaver.callOriginal();
        if(!executionContext.getErrors().isEmpty()){
            reportGraphQLError(executionContext.getErrors().get(0));
        }
        return resolveResult;
    }
}
