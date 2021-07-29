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
        AgentBridge.privateApi.addTracerParameter("graphql.field.path", parameters.getPath().getSegmentName());
        //this isn't correct
        AgentBridge.privateApi.addTracerParameter("graphql.field.parentType", parameters.getParent().getExecutionStepInfo().getType().toString());
        AgentBridge.privateApi.addTracerParameter("graphql.field.name", parameters.getField().getName());
        //AgentBridge.privateApi.addTracerParameter("graphql.field.returnType", TBD);
        return Weaver.callOriginal();
    }
}
