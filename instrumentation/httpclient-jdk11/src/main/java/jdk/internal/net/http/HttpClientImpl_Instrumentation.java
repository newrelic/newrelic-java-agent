package jdk.internal.net.http;

import com.newrelic.agent.bridge.jfr.events.external.HttpExternalEvent;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient.Java11HttpClientUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.nr.agent.instrumentation.httpclient.Java11HttpClientUtil.*;

@Weave(originalName = "jdk.internal.net.http.HttpClientImpl", type = MatchType.ExactClass)
final class HttpClientImpl_Instrumentation {
    @NewField
    private HttpExternalEvent httpExternalEvent; // fixme adding this object could be problematic if jfr apis don't exist

    @Trace
    public <T> HttpResponse<T> send(HttpRequest req, HttpResponse.BodyHandler<T> responseHandler)
            throws IOException, InterruptedException {
        beginJfrEvent();
        URI uri = req.uri();
        Segment segment = startSegment(uri);
        HttpResponse<T> response;
        try {
            response = Weaver.callOriginal();
            if (segment == null) {
                endJfrEvent();
                return response;
            }
        } catch (Exception e) {
            handleConnectException(e, req, segment);
            throw e;
        }
        processResponse(response, segment);
        commitJfrEvent(req, response, "send");
        endJfrEvent();
        return response;
    }

    @Trace
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest, HttpResponse.BodyHandler<T> responseHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        beginJfrEvent();
        URI uri = userRequest.uri();
        Segment segment = startSegment(uri);
        CompletableFuture<HttpResponse<T>> completableFutureResponse = Weaver.callOriginal();
        if (segment == null) {
            endJfrEvent();
            return completableFutureResponse;
        }
        commitJfrEvent(userRequest, null, "sendAsync");
        endJfrEvent();
        return completableFutureResponse.whenComplete(Java11HttpClientUtil.reportAsExternal(uri, segment));
    }

    private void beginJfrEvent() {
        httpExternalEvent = new HttpExternalEvent();
        httpExternalEvent.begin();
    }

    private void endJfrEvent() {
        httpExternalEvent.end();
    }

    private <T> void commitJfrEvent(HttpRequest request, HttpResponse<T> response, String javaMethod) {
        httpExternalEvent.httpClient = "JDK11 HttpClient";
        httpExternalEvent.instrumentation = "httpclient-jdk11";
        httpExternalEvent.javaMethod = javaMethod;

        if (request != null) {
            httpExternalEvent.method = request.method();

            URI uri = request.uri();

            if (uri != null) {
                httpExternalEvent.path = uri.getPath();
                httpExternalEvent.queryParameters = uri.getQuery();
            }

//            Optional<HttpRequest.BodyPublisher> bodyPublisherOptional = request.bodyPublisher();
//            if (bodyPublisherOptional.isPresent()) {
//                HttpRequest.BodyPublisher bodyPublisher = bodyPublisherOptional.get();
//                httpExternalEvent.length = Math.toIntExact(bodyPublisher.contentLength());
//            }
        }

        if (response != null) {

//            httpExternalEvent.error
            httpExternalEvent.responseHeaders = response.headers().toString();
//            httpExternalEvent.responseLength = response.
            httpExternalEvent.status = response.statusCode();

        }
        httpExternalEvent.commit();
    }
}
