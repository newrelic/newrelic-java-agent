/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl;

import org.apache.pekko.event.LoggingAdapter;
import org.apache.pekko.http.scaladsl.model.HttpRequest;
import org.apache.pekko.http.scaladsl.model.HttpResponse;
import org.apache.pekko.http.scaladsl.settings.ConnectionPoolSettings;
import org.apache.pekko.http.scaladsl.settings.ServerSettings;
import org.apache.pekko.stream.Materializer;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.pekkohttpcore.PekkoHttpUtils;
import scala.Function1;
import scala.concurrent.Future;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.pekko.http.scaladsl.HttpExt")
public class HttpExtInstrumentation {

    // These methods are deprecated but still exist in Pekko Http Core 1.0.0.
    // They have been replaced by Http().newServerAt().bind().
    public Future<HttpInstrumentation.ServerBinding> bindAndHandleAsync(
            Function1<HttpRequest, Future<HttpResponse>> handler,
            String interfaceString, int port,
            ConnectionContext connectionContext,
            ServerSettings settings, int parallelism,
            LoggingAdapter adapter, Materializer mat) {

        AsyncRequestHandler wrapperHandler = new AsyncRequestHandler(handler, mat.executionContext());
        handler = wrapperHandler;

        return Weaver.callOriginal();
    }

    public Future<HttpInstrumentation.ServerBinding> bindAndHandleSync(
            Function1<HttpRequest, HttpResponse> handler,
            String interfaceString, int port,
            ConnectionContext connectionContext,
            ServerSettings settings,
            LoggingAdapter adapter, Materializer mat) {

        SyncRequestHandler wrapperHandler = new SyncRequestHandler(handler);
        handler = wrapperHandler;

        return Weaver.callOriginal();
    }

    public Future<HttpResponse> singleRequest(HttpRequest httpRequest, HttpsConnectionContext connectionContext, ConnectionPoolSettings poolSettings,
            LoggingAdapter loggingAdapter) {
        final Segment segment = NewRelic.getAgent().getTransaction().startSegment("Pekko", "singleRequest");

        Future<HttpResponse> responseFuture = Weaver.callOriginal();

        PekkoHttpUtils.finishSegmentOnComplete(httpRequest, responseFuture, segment);

        return responseFuture;
    }

}
