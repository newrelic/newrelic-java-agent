/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.marshalling;

import org.apache.pekko.http.scaladsl.model.HttpRequest;
import org.apache.pekko.http.scaladsl.model.HttpResponse;
import com.agent.instrumentation.org.apache.pekko.http.PathMatcherUtils;
import com.agent.instrumentation.org.apache.pekko.http.RequestWrapper;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

@Weave(originalName = "org.apache.pekko.http.scaladsl.marshalling.Marshal")
public class PekkoHttpMarshal<A> {

    public Future<HttpResponse> toResponseFor(HttpRequest request, Marshaller<A, HttpResponse> m, ExecutionContext ec) {
        NewRelic.getAgent().getTransaction().setWebRequest(new RequestWrapper(request));
        PathMatcherUtils.reset();
        return Weaver.callOriginal();
    }

}
