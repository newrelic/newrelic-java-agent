package io.ktor.client.statement;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.client.KtorClientUtils;
import com.newrelic.instrumentation.labs.ktor.client.NRContinuationWrapper;
import com.newrelic.instrumentation.labs.ktor.client.NRFunction2Wrapper;
import io.ktor.client.HttpClient;
import io.ktor.client.engine.HttpClientEngine;
import io.ktor.client.request.HttpRequestBuilder;
import io.ktor.http.HttpMethod;
import io.ktor.http.URLBuilder;
import io.ktor.http.Url;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;

import java.net.URI;

@Weave(originalName = "io.ktor.client.statement.HttpStatement")
public class HttpStatement_Instrumentation {

    @NewField
    private HttpParameters httpParameters = null;

    @NewField
    private boolean needsTracking = false;

    public HttpStatement_Instrumentation(HttpRequestBuilder builder, HttpClient client) {
        HttpClientEngine engine = client.getEngine();
        String engineType = engine.getClass().getSimpleName();
        needsTracking = KtorClientUtils.needsLeaf(engineType);

        if (needsTracking) {
            URLBuilder urlBuilder = builder.getUrl();
            if(urlBuilder != null) {
                Url url = urlBuilder.build();
                String urlString = url.toString();
                if(urlString != null) {
                    HttpMethod method = builder.getMethod();
                    URI uri = URI.create(urlString);
                    httpParameters = HttpParameters.library("Ktor-HttpStatement").uri(uri).procedure(method.getValue()).noInboundHeaders().build();
                }
            }
        }
    }

    public Object execute(Continuation<? super HttpResponse> continuation) {
        if(needsTracking) {
            NRContinuationWrapper<? super HttpResponse> wrapper = KtorClientUtils.getContinuationWrapper(continuation, httpParameters);
            if (wrapper != null) {
                continuation = wrapper;
            }
        }
        return Weaver.callOriginal();
    }

    @Trace
    public <T> Object execute(Function2<? super HttpResponse, ? super Continuation<? super T>, ? extends T> function2, Continuation<? super T> continuation) {
        if(needsTracking) {
            if (!(function2 instanceof NRFunction2Wrapper)) {
                function2 = new NRFunction2Wrapper<>(function2);
            }
            NRContinuationWrapper<? super T> wrapper = KtorClientUtils.getContinuationWrapper(continuation, httpParameters);
            if (wrapper != null) {
                continuation = wrapper;
            }
        }
        return Weaver.callOriginal();
    }

    @Trace
    public Object executeUnsafe(Continuation<? super HttpResponse> continuation) {
        return Weaver.callOriginal();
    }

}
