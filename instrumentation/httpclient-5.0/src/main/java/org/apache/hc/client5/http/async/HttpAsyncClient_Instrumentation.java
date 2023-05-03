/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.hc.client5.http.async;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient50.WrappedFutureCallback;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer_Instrumentation;
import org.apache.hc.core5.http.nio.support.AbstractAsyncResponseConsumer_Instrumentation;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.util.concurrent.Future;
import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.apache.hc.client5.http.async.HttpAsyncClient")
public class HttpAsyncClient_Instrumentation {

    public <T> Future<T> execute(
            AsyncRequestProducer requestProducer,
            AsyncResponseConsumer<T> responseConsumer,
            HandlerFactory<AsyncPushConsumer> pushHandlerFactory,
            HttpContext context,
            FutureCallback<T> callback) {

        HttpRequest request = ((SimpleRequestProducer_Instrumentation)requestProducer).nrRequest;
        Token token = NewRelic.getAgent().getTransaction().getToken();
        callback = new WrappedFutureCallback<>(request, callback); // needed for finishing off the External registration
        if (responseConsumer instanceof AbstractAsyncResponseConsumer_Instrumentation) {
            AbstractAsyncResponseConsumer_Instrumentation asyncRespConsumer = (AbstractAsyncResponseConsumer_Instrumentation)responseConsumer;
            asyncRespConsumer.token = token;
        }

        NewRelic.getAgent().getLogger().log(Level.INFO, "1st tracedMethod: "+NewRelic.getAgent().getTracedMethod());

        Future<T> origResult =  Weaver.callOriginal();

        return origResult;
    }
}
