/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl;

import akka.NotUsed;
import akka.http.scaladsl.model.HttpRequest;
import akka.http.scaladsl.model.HttpResponse;
import akka.stream.Materializer;
import akka.stream.scaladsl.Flow;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "akka.http.scaladsl.Http$IncomingConnection")
public class IncomingConnection {
  public Flow<HttpResponse, HttpRequest, NotUsed> flow() {
    return Weaver.callOriginal();
  }

  public Object handleWith(Flow<HttpRequest, HttpResponse, Object> handler, final Materializer fm) {
    handler = FlowRequestHandler$.MODULE$.instrumentFlow(handler, fm);
    return Weaver.callOriginal();
  }

}
