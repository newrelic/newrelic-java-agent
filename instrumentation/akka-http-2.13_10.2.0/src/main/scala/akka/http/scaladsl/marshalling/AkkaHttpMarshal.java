/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl.marshalling;

import akka.http.scaladsl.model.HttpRequest;
import akka.http.scaladsl.model.HttpResponse;
import com.agent.instrumentation.akka.http102.PathMatcherUtils;
import com.agent.instrumentation.akka.http102.RequestWrapper;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

@Weave(originalName = "akka.http.scaladsl.marshalling.Marshal")
public class AkkaHttpMarshal<A> {

    public Future<HttpResponse> toResponseFor(HttpRequest request, Marshaller<A, HttpResponse> m, ExecutionContext ec) {
        NewRelic.getAgent().getTransaction().setWebRequest(new RequestWrapper(request));
        PathMatcherUtils.reset();
        return Weaver.callOriginal();
    }

}
