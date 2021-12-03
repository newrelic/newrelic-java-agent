/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.api.libs.ws;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.playws.PlayWSUtils;
import scala.Tuple2;
import scala.collection.immutable.Seq;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

@Weave(type = MatchType.Interface, originalName = "play.api.libs.ws.StandaloneWSRequest")
public abstract class StandaloneWSRequest_Instrumentation {

    @NewField
    public Segment segment = null;

    public StandaloneWSRequest_Instrumentation addHttpHeaders(Seq<Tuple2<String, String>> hdrs) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withHttpHeaders(Seq<Tuple2<String, String>> headers) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withQueryStringParameters(Seq<Tuple2<String, String>> parameters) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation addQueryStringParameters(Seq<Tuple2<String, String>> parameters) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation addCookies(Seq<WSCookie> cookies) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withCookies(Seq<WSCookie> cookie) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation sign(WSSignatureCalculator calc) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withAuth(String username, String password, WSAuthScheme scheme) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withFollowRedirects(boolean follow) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withRequestTimeout(Duration timeout) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withRequestFilter(WSRequestFilter filter) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withVirtualHost(String vh) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withProxyServer(WSProxyServer proxyServer) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public <T> StandaloneWSRequest_Instrumentation withBody(T body, BodyWritable<T> evidence$1) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation withMethod(String method) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public Future<StandaloneWSResponse> get() {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("get", responseFuture, currentSegment);
        return responseFuture;
    }

    public <T> Future<StandaloneWSResponse> post(T body, BodyWritable<T> evidence$2) {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public <T> Future<StandaloneWSResponse> patch(T body, BodyWritable<T> evidence$3) {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public <T> Future<StandaloneWSResponse> put(T body, BodyWritable<T> evidence$4) {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> delete() {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("delete", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> head() {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("head", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> options() {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("options", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> execute(String method) {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("execute", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> execute() {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("execute", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> stream() {
        Segment currentSegment = this.segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("stream", responseFuture, currentSegment);
        return responseFuture;
    }

    private Future<StandaloneWSResponse> tryRecordExternalRequest(String procedure, Future<StandaloneWSResponse> responseFuture, Segment segment) {
        if (segment != null) {
            responseFuture = PlayWSUtils.finish(segment, procedure, (StandaloneWSRequest) this, responseFuture);
        }

        return responseFuture;
    }

}
