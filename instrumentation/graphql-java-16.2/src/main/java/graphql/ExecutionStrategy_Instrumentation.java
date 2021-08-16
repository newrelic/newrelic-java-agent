package graphql;

import com.newrelic.agent.bridge.AgentBridge;
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
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.concurrent.CompletableFuture;
import static com.nr.instrumentation.graphql.GraphQLErrorUtil.*;
import static com.nr.instrumentation.graphql.GraphQLObfuscateUtil.obfuscateQuery;
import static com.nr.instrumentation.graphql.GraphQLTransactionName.getFirstOperationDefinitionFrom;
import static com.nr.instrumentation.graphql.GraphQLTransactionName.getOperationTypeFrom;

@Weave(originalName = "graphql.execution.ExecutionStrategy", type = MatchType.BaseClass)
public class ExecutionStrategy_Instrumentation {

    @Trace
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        Document document = executionContext.getDocument();
        String query = executionContext.getExecutionInput().getQuery();
        String transactionName = GraphQLTransactionName.from(document);
        NewRelic.setTransactionName("GraphQL", transactionName);
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/operation" + transactionName);
        OperationDefinition definition = getFirstOperationDefinitionFrom(document);
        String operationName = definition.getName();
        AgentBridge.privateApi.addTracerParameter("graphql.operation.type", definition != null ? getOperationTypeFrom(definition) : "Unavailable");
        AgentBridge.privateApi.addTracerParameter("graphql.operation.name", operationName != null ? operationName  : "<anonymous>");
        AgentBridge.privateApi.addTracerParameter("graphql.operation.query", obfuscateQuery(query));
        return Weaver.callOriginal();
    }

    @Trace
    protected CompletableFuture<FieldValueInfo> resolveFieldWithInfo(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/resolve/" + parameters.getPath().getSegmentName());
        AgentBridge.privateApi.addTracerParameter("graphql.field.path", parameters.getPath().getSegmentName());
        GraphQLObjectType type = (GraphQLObjectType) parameters.getExecutionStepInfo().getType();
        AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", type.getName());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
        CompletableFuture<FieldValueInfo> resolveResult = Weaver.callOriginal();
        if(!executionContext.getErrors().isEmpty()){
            reportGraphQLError(executionContext.getErrors().get(0));
        }
        return resolveResult;
    }
}
