package io.ktor.server.routing;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.instrumentation.labs.ktor.server.KtorExtendedRequest;
import io.ktor.http.Parameters;
import io.ktor.server.application.PipelineCall;
import io.ktor.server.request.ApplicationReceivePipeline;
import io.ktor.server.response.ApplicationSendPipeline;
import kotlin.coroutines.CoroutineContext;

@Weave(originalName = "io.ktor.server.routing.RoutingPipelineCall")
public class RoutingPipelineCall_Instrumentation {

    public RoutingPipelineCall_Instrumentation(PipelineCall pipelineCall, RoutingNode routingNode,CoroutineContext coroutineContext,
            ApplicationReceivePipeline applicationReceivePipeline, ApplicationSendPipeline applicationSendPipeline, Parameters parameters) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(pipelineCall);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, false, "KtorServerRouting", routingNode.toString());
    }
}
