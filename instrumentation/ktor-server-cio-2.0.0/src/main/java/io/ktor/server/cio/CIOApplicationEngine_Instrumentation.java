package io.ktor.server.cio;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.http.cio.Request;
import io.ktor.server.cio.backend.ServerRequestScope;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave(originalName = "io.ktor.server.cio.CIOApplicationEngine")
public class CIOApplicationEngine_Instrumentation {

    @Trace
    private Object handleRequest(ServerRequestScope serverRequestScope, Request request, Continuation<? super Unit> continuation) {
        TracedMethod traced = NewRelic.getAgent().getTracedMethod();
        traced.addCustomAttribute("Request-URI", request.getUri().toString());
        traced.addCustomAttribute("Request-Method", request.getMethod().toString());
        return Weaver.callOriginal();
    }

}
