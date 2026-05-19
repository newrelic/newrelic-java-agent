package io.ktor.server.routing;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.http.Parameters;
import io.ktor.server.application.PipelineCall;
import io.ktor.util.pipeline.PipelineContext;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave(originalName = "io.ktor.server.routing.RoutingRoot")
public class RoutingRoot_Instrumentation {

    @Trace
    private java.lang.Object executeResult(PipelineContext<Unit, PipelineCall> context, RoutingNode routingNode, Parameters parameters, Continuation<? super Unit> continuation) {
        if(routingNode != null) {
            String routingString = routingNode.toString();
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_LOW, true, "KtorRouting", routingString);
        }
        return Weaver.callOriginal();
    }
}
