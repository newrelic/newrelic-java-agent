/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.libs.ws;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.playws.JavaPlayWSUtils;
import org.w3c.dom.Document;
import play.mvc.Http;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CompletionStage;

@Weave(type = MatchType.Interface, originalName = "play.libs.ws.WSRequest")
public class WSRequest_Instrumentation {

    public WSRequest_Instrumentation setBody(String body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation setBody(JsonNode body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation setBody(InputStream body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation setBody(File body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public <U> WSRequest_Instrumentation setBody(Source<ByteString, U> body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation setHeader(String name, String value) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation setQueryParameter(String name, String value) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation addCookie(Http.Cookie cookie) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation setRequestTimeout(long timeout) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public CompletionStage<WSResponse> patch(String body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> patch(JsonNode body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> patch(Document body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> patch(InputStream body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> patch(File body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> patch(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> post(String body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> post(JsonNode body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> post(Document body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> post(InputStream body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> post(File body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> post(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> put(String body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> put(JsonNode body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> put(Document body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> put(InputStream body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> put(File body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public CompletionStage<WSResponse> put(Source<? super Http.MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        CompletionStage<WSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    private CompletionStage<WSResponse> tryRecordExternalRequest(String procedure, CompletionStage<WSResponse> responseFuture, Segment segment) {
        if (segment != null) {
            responseFuture = (CompletionStage<WSResponse>) JavaPlayWSUtils.finish(segment, procedure,
                    (StandaloneWSRequest) StandaloneWSRequest_Instrumentation.class.cast(this), responseFuture);
        }

        return responseFuture;
    }

}
