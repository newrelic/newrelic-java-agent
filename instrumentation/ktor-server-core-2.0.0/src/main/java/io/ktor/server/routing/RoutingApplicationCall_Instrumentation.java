package io.ktor.server.routing;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.instrumentation.labs.ktor.server.KtorExtendedRequest;
import io.ktor.http.Parameters;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.request.ApplicationReceivePipeline;
import io.ktor.server.response.ApplicationSendPipeline;
import kotlin.coroutines.CoroutineContext;

@Weave(originalName = "io.ktor.server.routing.RoutingApplicationCall")
public class RoutingApplicationCall_Instrumentation {

    public RoutingApplicationCall_Instrumentation(ApplicationCall call, Route route, CoroutineContext context, ApplicationReceivePipeline requestPipeline, ApplicationSendPipeline sendPipeline, Parameters parameters) {
        KtorExtendedRequest extendedRequest = new KtorExtendedRequest(call);
        NewRelic.getAgent().getTransaction().setWebRequest(extendedRequest);
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, false, "KtorServerRouting", route.toString());
    }
}
