package graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.FieldValueInfo;

import java.util.concurrent.CompletableFuture;
@Weave(originalName = "graphql.execution.ExecutionStrategy", type = MatchType.BaseClass)
public class ExecutionStrategy_Instrumentation {
    @Trace
    protected CompletableFuture<FieldValueInfo> resolveFieldWithInfo(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/resolve/" + parameters.getPath().getSegmentName());
        NewRelic.getAgent().getTracedMethod().addRollupMetricName("GraphQL/resolve", parameters.getPath().getSegmentName());
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
        //this isn't correct
        AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", parameters.getParent().getExecutionStepInfo().getType().toString());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
        //AgentBridge.privateApi.addTracerParameter("graphql.field.returnType", TBD);
        //AgentBridge.privateApi.addTracerParameter("graphql.field.args", TBD map);

        return Weaver.callOriginal();
    }
}
