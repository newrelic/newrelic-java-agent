/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.hc.client5.http.async;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient50.InboundWrapper;
import com.nr.agent.instrumentation.httpclient50.InstrumentationUtils;
import com.nr.agent.instrumentation.httpclient50.OutboundWrapper;
import com.nr.agent.instrumentation.httpclient50.WrappedFutureCallback;
import com.nr.agent.instrumentation.httpclient50.WrappedResponseConsumer;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.HandlerFactory;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer_Instrumentation;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.apache.hc.client5.http.async.HttpAsyncClient")
public abstract class HttpAsyncClient_Instrumentation {

    private static void doOutboundCAT(HttpRequest request) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "inside doOutboundCAT");
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(request));
    }

    @Trace(async = true)
    public <T> Future<T> execute(
            AsyncRequestProducer requestProducer,
            AsyncResponseConsumer<T> responseConsumer,
            HandlerFactory<AsyncPushConsumer> pushHandlerFactory,
            HttpContext context,
            FutureCallback<T> callback) {

        // TODO will this always work?
        HttpRequest request = ((SimpleRequestProducer_Instrumentation)requestProducer).nrRequest;
        doOutboundCAT(request);
        // TODO anything for DT?

        Token token = NewRelic.getAgent().getTransaction().getToken();  // TODO not working yet
        callback = new WrappedFutureCallback<>(request, callback, token);
        responseConsumer = new WrappedResponseConsumer(responseConsumer, token);

        NewRelic.getAgent().getLogger().log(Level.INFO, "1st tracedMethod: "+NewRelic.getAgent().getTracedMethod());

        Future<T> origResult =  Weaver.callOriginal();

        // TODO anything to do here?

        return origResult;
    }
}
