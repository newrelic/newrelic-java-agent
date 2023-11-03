package io.vertx.core.http.impl;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.HttpClientRequestPromiseWrapper;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.impl.future.PromiseInternal;
import io.vertx.core.net.SocketAddress;

@Weave(type = MatchType.BaseClass, originalName = "io.vertx.core.http.impl.HttpClientImpl")
public class HttpClientImpl_Instrumentation {

    private void doRequest(RequestOptions request, PromiseInternal<HttpClientRequest> promise) {
        promise = new HttpClientRequestPromiseWrapper(promise, NewRelic.getAgent().getTransaction().getToken());
        Weaver.callOriginal();
    }
}
