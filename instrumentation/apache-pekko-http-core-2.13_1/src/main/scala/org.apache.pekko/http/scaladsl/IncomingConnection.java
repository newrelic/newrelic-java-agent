/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl;

import org.apache.pekko.http.scaladsl.model.HttpRequest;
import org.apache.pekko.http.scaladsl.model.HttpResponse;
import org.apache.pekko.stream.Materializer;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.Future;

@Weave(originalName = "org.apache.pekko.http.scaladsl.Http$IncomingConnection")
public class IncomingConnection {

    public void handleWithSyncHandler(Function1<HttpRequest, HttpResponse> func, Materializer mat) {

        SyncRequestHandler wrapperHandler = new SyncRequestHandler(func);
        func = wrapperHandler;

        Weaver.callOriginal();
    }

    public void handleWithAsyncHandler(Function1<HttpRequest, Future<HttpResponse>> func, int parallel, Materializer mat) {

        AsyncRequestHandler wrapperHandler = new AsyncRequestHandler(func, mat.executionContext());
        func = wrapperHandler;

        Weaver.callOriginal();
    }


}
