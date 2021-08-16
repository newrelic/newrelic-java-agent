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

import static com.nr.instrumentation.graphql.GraphQLAttributeUtil.addAttributeForArgument;
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
        //todo complete the following attributes
        /*

        the resolver spans in the UI should look like

        resolve/..../bookById
           resolve/..../title - these spans are removed because they return Scalar,

           unless top level item.
        query {
            hello {
             this returns a string "World"
            }
        }

        Example query:

         query fastAndFun {
            bookById (id: "book-1", title: "furious") {
                title
            }
        }

       I think the attributes for resolver span  - bookById - should be:
        graphql.field.path  - bookById
        graphql.field.parentType  - Query
        graphql.field.name - bookById
        graphql.field.returnType - Book -> this from scraping the TypeDef, look in the schema.
                                        Or, look up the parentType and it may have the TypeDef.

        graphql.field.args.<name>(so graphql.field.args.id) - book-1

        query fastAndFun {
            bookById (id: "book-1", title: "furious") {
                title {
                 id
                }
            }
        }

       I think the attributes for resolver span  - title - :
        graphql.field.path  - bookById.title
        graphql.field.parentType  - Book
        graphql.field.name - title
        graphql.field.returnType - Title
        graphql.field.args - NA, don't report

         */

        AgentBridge.privateApi.addTracerParameter("graphql.field.path", parameters.getPath().getSegmentName());
        GraphQLObjectType type = (GraphQLObjectType) parameters.getExecutionStepInfo().getType();
        AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", type.getName());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
        if (!parameters.getField().getSingleField().getArguments().isEmpty()) {
            addAttributeForArgument(parameters.getField().getSingleField().getArguments());
        }
        //AgentBridge.privateApi.addTracerParameter("graphql.field.args", TBD map);
        CompletableFuture<FieldValueInfo> resolveResult = Weaver.callOriginal();
        if(!executionContext.getErrors().isEmpty()){
            reportGraphQLError(executionContext.getErrors().get(0));
        }
        return resolveResult;
    }
}
