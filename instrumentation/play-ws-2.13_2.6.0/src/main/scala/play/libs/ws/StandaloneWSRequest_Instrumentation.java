/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.libs.ws;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.playws.JavaPlayWSUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Weave(type = MatchType.Interface, originalName = "play.libs.ws.StandaloneWSRequest")
public abstract class StandaloneWSRequest_Instrumentation {

    @NewField
    public Segment segment = null;

    public StandaloneWSRequest_Instrumentation setMethod(String method) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setBody(BodyWritable body) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setHeaders(Map<String, List<String>> headers) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation addHeader(String name, String value) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setQueryString(String query) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation addQueryParameter(String name, String value) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setQueryString(Map<String, List<String>> params) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation addCookie(WSCookie cookie) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation addCookies(WSCookie... cookies) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setCookies(List<WSCookie> cookies) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setAuth(String userInfo) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setAuth(String username, String password) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setAuth(String username, String password, WSAuthScheme scheme) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation sign(WSSignatureCalculator calculator) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setFollowRedirects(boolean followRedirects) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setVirtualHost(String virtualHost) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setRequestTimeout(Duration timeout) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setRequestFilter(WSRequestFilter filter) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public StandaloneWSRequest_Instrumentation setContentType(String contentType) {
        Segment currentSegment = this.segment;
        this.segment = null;
        StandaloneWSRequest_Instrumentation result = Weaver.callOriginal();
        result.segment = currentSegment;
        return result;
    }

    public CompletionStage<? extends StandaloneWSResponse> get() {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("get", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> patch(BodyWritable body) {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> post(BodyWritable body) {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> put(BodyWritable body) {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> delete() {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("delete", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> head() {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("head", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> options() {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("options", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> execute(String method) {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("execute", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> execute() {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("execute", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<? extends StandaloneWSResponse> stream() {
        Segment currentSegment = this.segment;
        CompletionStage<? extends StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("stream", responseFuture, currentSegment);
        return responseFuture;
    }

    private CompletionStage<? extends StandaloneWSResponse> tryRecordExternalRequest(String procedure,
            CompletionStage<? extends StandaloneWSResponse> responseFuture, Segment segment) {
        if (segment != null) {
            responseFuture = JavaPlayWSUtils.finish(segment, procedure, (StandaloneWSRequest) this, responseFuture);
        }

        return responseFuture;
    }

}
