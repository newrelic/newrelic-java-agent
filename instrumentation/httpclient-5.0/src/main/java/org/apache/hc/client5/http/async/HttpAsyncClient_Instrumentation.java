/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.hc.client5.http.async;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient50.InstrumentationUtils;
import com.nr.agent.instrumentation.httpclient50.WrappedFutureCallback;
import com.nr.agent.instrumentation.httpclient50.WrappedResponseConsumer;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer_Instrumentation;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.util.concurrent.Future;
import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.apache.hc.client5.http.async.HttpAsyncClient")
public abstract class HttpAsyncClient_Instrumentation {

    @Trace(async = true)
    public <T> Future<T> execute(
            AsyncRequestProducer requestProducer,
            AsyncResponseConsumer<T> responseConsumer,
            HandlerFactory<AsyncPushConsumer> pushHandlerFactory,
            HttpContext context,
            FutureCallback<T> callback) {

        HttpRequest request = ((SimpleRequestProducer_Instrumentation)requestProducer).nrRequest;
        InstrumentationUtils.doOutboundCAT(request);
        // TODO anything for DT?

        Token token = NewRelic.getAgent().getTransaction().getToken();
        callback = new WrappedFutureCallback<>(request, callback, token);
        responseConsumer = new WrappedResponseConsumer(responseConsumer, token);

        NewRelic.getAgent().getLogger().log(Level.INFO, "1st tracedMethod: "+NewRelic.getAgent().getTracedMethod());

        Future<T> origResult =  Weaver.callOriginal();

        // TODO anything to do here?

        return origResult;
    }
}
