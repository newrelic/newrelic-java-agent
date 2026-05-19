/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.client;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.client.KtorClientUtils;
import io.ktor.client.call.HttpClientCall;
import io.ktor.client.engine.HttpClientEngine;
import io.ktor.client.engine.HttpClientEngineConfig;
import io.ktor.client.request.HttpRequestBuilder;
import kotlin.coroutines.Continuation;

@Weave(originalName = "io.ktor.client.HttpClient")
public class HttpClient_Instrumentation {

    /**
     * Some implementations require that we do not trace downstream methods.  this is usually in the case where there isn't a instrumented
     * framework in the agent that handles the external call.  It this case we handle the external call here.
     */
    @NewField
    private boolean needsLeaf = false;

    public HttpClient_Instrumentation(HttpClientEngine clientEngine, HttpClientConfig<? extends HttpClientEngineConfig> userConfig) {
        String engineType = clientEngine.getClass().getSimpleName();
        needsLeaf = KtorClientUtils.needsLeaf(engineType);

    }

    public Object execute$ktor_client_core(HttpRequestBuilder builder, Continuation<? super HttpClientCall> continuation) {
        ExitTracer exitTracer = null;
        if (needsLeaf && builder != null) {
            exitTracer = KtorClientUtils.getExitTracer(this);
        }
        Object result = Weaver.callOriginal();
        if (exitTracer != null) {
            exitTracer.finish(0, result);
        }
        return result;
    }

}
