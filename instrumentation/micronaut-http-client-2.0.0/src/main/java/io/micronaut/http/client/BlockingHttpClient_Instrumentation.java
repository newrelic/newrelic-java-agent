package io.micronaut.http.client;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.http.client.MicronautHeaders;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;

import java.net.URI;

@Weave(originalName = "io.micronaut.http.client.BlockingHttpClient", type = MatchType.Interface)
public class BlockingHttpClient_Instrumentation {

    @Trace(leaf = true)
    public <I, O, E> O retrieve(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
        MicronautHeaders headers = new MicronautHeaders(request);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        URI uri = request.getUri();
        String method = request.getMethodName();
        HttpParameters parameters = HttpParameters.library("Micronaut-Client").uri(uri).procedure(method).noInboundHeaders().build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(parameters);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public <I, O, E> HttpResponse<O> exchange(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
        MicronautHeaders headers = new MicronautHeaders(request);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        URI uri = request.getUri();
        String method = request.getMethodName();
        HttpParameters parameters = HttpParameters.library("Micronaut-Client").uri(uri).procedure(method).noInboundHeaders().build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(parameters);
        return Weaver.callOriginal();
    }
}
