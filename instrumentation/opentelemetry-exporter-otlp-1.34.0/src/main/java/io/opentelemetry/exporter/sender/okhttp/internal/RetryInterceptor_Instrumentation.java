/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.exporter.sender.okhttp.internal;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.otlp.retry.RetryCountingChain;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

/**
 * Instruments RetryInterceptor to count retried export attempts. RetryInterceptor's own retry
 * loop calls {@code chain.proceed(chain.request())} exactly once per attempt, so substituting a
 * counting {@link Interceptor.Chain} is the only way to observe individual attempts without a
 * public retry callback.
 */
@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.exporter.sender.okhttp.internal.RetryInterceptor")
public abstract class RetryInterceptor_Instrumentation {

    public Response intercept(Interceptor.Chain chain) throws IOException {
        if (!(chain instanceof RetryCountingChain)) {
            chain = new RetryCountingChain(chain);
        }
        return Weaver.callOriginal();
    }
}
