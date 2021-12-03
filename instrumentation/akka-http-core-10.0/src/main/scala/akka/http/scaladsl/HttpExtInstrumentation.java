/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl;

import akka.event.LoggingAdapter;
import akka.http.scaladsl.model.HttpRequest;
import akka.http.scaladsl.model.HttpResponse;
import akka.http.scaladsl.settings.ConnectionPoolSettings;
import akka.http.scaladsl.settings.ServerSettings;
import akka.stream.Materializer;
import akka.stream.scaladsl.Flow;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.akkahttpcore.AkkaHttpUtils;
import scala.Function1;
import scala.concurrent.Future;

@Weave(type = MatchType.ExactClass, originalName = "akka.http.scaladsl.HttpExt")
public class HttpExtInstrumentation {

  @NewField
  public boolean bindingInstrumented;

  public Future<HttpInstrumentation.ServerBinding> bindAndHandleAsync(
    Function1<HttpRequest, Future<HttpResponse>> handler,
    String interfaceString, int port,
    ConnectionContext connectionContext,
    ServerSettings settings, int parallelism,
    LoggingAdapter adapter, Materializer mat) {

    AsyncRequestHandler wrapperHandler = new AsyncRequestHandler(handler, mat.executionContext());
    handler = wrapperHandler;
    bindingInstrumented = true;
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
    bindingInstrumented = true;

    return Weaver.callOriginal();
  }

  public Future<HttpInstrumentation.ServerBinding> bindAndHandle(
    Flow<HttpRequest, HttpResponse, Object> handler,
    String _interface,
    int port, ConnectionContext connectionContext,
    ServerSettings settings, LoggingAdapter log, Materializer fm) {

    if (!bindingInstrumented) {
      handler = new FlowRequestHandler().instrumentFlow(handler, fm);
    }
    return Weaver.callOriginal();
  }

  // We are weaving the singleRequestImpl method here rather than just singleRequest because the javadsl only flows through here
  public Future<HttpResponse> singleRequest(HttpRequest httpRequest, HttpsConnectionContext connectionContext, ConnectionPoolSettings settings,
                                            LoggingAdapter log, Materializer fm) {
    final Segment segment = NewRelic.getAgent().getTransaction().startSegment("Akka", "singleRequest");

    Future<HttpResponse> responseFuture = Weaver.callOriginal();

    AkkaHttpUtils.finishSegmentOnComplete(httpRequest, responseFuture, segment);

    return responseFuture;
  }

}
