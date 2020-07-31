/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.api.libs.ws;

import akka.stream.scaladsl.Source;
import akka.util.ByteString;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.playws.PlayWSUtils;
import play.api.mvc.MultipartFormData;
import scala.Tuple2;
import scala.collection.Seq;
import scala.concurrent.Future;

import java.io.File;

@Weave(type = MatchType.Interface, originalName = "play.api.libs.ws.WSRequest")
public class WSRequest_Instrumentation {

    public WSRequest_Instrumentation withHeaders(Seq<Tuple2<String, String>> headers) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public WSRequest_Instrumentation withQueryString(Seq<Tuple2<String, String>> parameters) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        StandaloneWSRequest_Instrumentation.class.cast(this).segment = null;
        WSRequest_Instrumentation result = Weaver.callOriginal();
        StandaloneWSRequest_Instrumentation.class.cast(result).segment = currentSegment;
        return result;
    }

    public Future<StandaloneWSResponse> post(File body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> post(Source<MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("post", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> patch(File body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> patch(Source<MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("patch", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> put(File body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    public Future<StandaloneWSResponse> put(Source<MultipartFormData.Part<Source<ByteString, ?>>, ?> body) {
        Segment currentSegment = StandaloneWSRequest_Instrumentation.class.cast(this).segment;
        Future<StandaloneWSResponse> responseFuture = Weaver.callOriginal();
        responseFuture = tryRecordExternalRequest("put", responseFuture, currentSegment);
        return responseFuture;
    }

    private Future<StandaloneWSResponse> tryRecordExternalRequest(String procedure, Future<StandaloneWSResponse> responseFuture, Segment currentSegment) {
        if (currentSegment != null) {
            responseFuture = PlayWSUtils.finish(currentSegment, procedure, (StandaloneWSRequest) StandaloneWSRequest_Instrumentation.class.cast(this), responseFuture);
        }

        return responseFuture;
    }

}
