/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl;

import akka.http.scaladsl.model.HttpRequest;
import akka.http.scaladsl.model.HttpResponse;
import akka.stream.Materializer;
import akka.stream.scaladsl.Flow;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.Future;

@Weave(originalName = "akka.http.scaladsl.Http$IncomingConnection")
public class IncomingConnection {

  @NewField
  public boolean bindingInstrumented;

  public void handleWithSyncHandler(Function1<HttpRequest, HttpResponse> func, Materializer mat) {
    SyncRequestHandler wrapperHandler = new SyncRequestHandler(func);
    bindingInstrumented = true;

    func = wrapperHandler;
    Weaver.callOriginal();
  }

  public void handleWithAsyncHandler(Function1<HttpRequest, Future<HttpResponse>> func, int parallel, Materializer mat) {
    AsyncRequestHandler wrapperHandler = new AsyncRequestHandler(func, mat.executionContext());
    bindingInstrumented = true;

    func = wrapperHandler;
    Weaver.callOriginal();
  }

  public Object handleWith(Flow<HttpRequest, HttpResponse, Object> handler, final Materializer fm) {
    if(!bindingInstrumented) {
      handler = new FlowRequestHandler().instrumentFlow(handler);
    }
    return Weaver.callOriginal();
  }

}
