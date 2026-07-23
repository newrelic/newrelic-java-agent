/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.quarkus.resteasy.runtime.standalone;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

@Weave(originalName = "io.quarkus.resteasy.runtime.standalone.RequestDispatcher", type = MatchType.ExactClass)
public class RequestDispatcher_Instrumentation {

    @Trace
    public void service(Context context, HttpServerRequest req, HttpServerResponse resp, HttpRequest vertxReq,
                        HttpResponse vertxResp, boolean handleNotFound, Throwable throwable) {
        if (throwable != null) {
            NewRelic.noticeError(throwable);
        }

        Weaver.callOriginal();
    }
}
