package io.ktor.client.plugins;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.client.KtorRequestHeaders;
import io.ktor.client.call.HttpClientCall;
import io.ktor.client.request.HttpRequestBuilder;
import io.ktor.http.HeadersBuilder;
import kotlin.coroutines.Continuation;

@Weave(originalName = "io.ktor.client.plugins.Sender", type = MatchType.Interface)
public class Sender_Instrumentation {

    public Object execute(HttpRequestBuilder requestBuilder, Continuation<? super HttpClientCall>  continuation) {
        if (requestBuilder != null) {
            HeadersBuilder headersBuilder = requestBuilder.getHeaders();
            if(headersBuilder != null) {
                KtorRequestHeaders wrapper = new KtorRequestHeaders(requestBuilder);
                NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(wrapper);
            }
        }
        return Weaver.callOriginal();
    }
}
