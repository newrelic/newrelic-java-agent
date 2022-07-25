package jdk.internal.net.http;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient.Java11HttpClientUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static com.nr.agent.instrumentation.httpclient.Java11HttpClientUtil.*;

@Weave(originalName = "jdk.internal.net.http.HttpClientImpl", type = MatchType.ExactClass)
final class HttpClientImpl_Instrumentation {

    @Trace
    public <T> HttpResponse<T> send(HttpRequest req, HttpResponse.BodyHandler<T> responseHandler)
            throws IOException, InterruptedException {
        URI uri = req.uri();
        Segment segment = startSegment(uri);
        HttpResponse<T> response;
        try {
            response = Weaver.callOriginal();
            if (segment == null) {
                return response;
            }
        } catch (Exception e) {
            handleConnectException(e, req, segment);
            throw e;
        }
        processResponse(response, segment);
        return response;
    }

    @Trace
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest, HttpResponse.BodyHandler<T> responseHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        URI uri = userRequest.uri();
        Segment segment = startSegment(uri);
        CompletableFuture<HttpResponse<T>> completableFutureResponse = Weaver.callOriginal();
        if (segment == null) {
            return completableFutureResponse;
        }
        return completableFutureResponse.whenComplete(Java11HttpClientUtil.reportAsExternal(uri, segment));
    }
}
