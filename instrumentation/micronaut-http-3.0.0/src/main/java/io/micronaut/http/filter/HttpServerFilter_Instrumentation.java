/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.filter;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;

@Weave(originalName = "io.micronaut.http.filter.HttpServerFilter", type = MatchType.Interface)
public abstract class HttpServerFilter_Instrumentation {

    @Trace(dispatcher = true)
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain_Instrumentation chain) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HttpServerFilter", getClass().getSimpleName(), "doFilter");
        Transaction transaction = NewRelic.getAgent().getTransaction();
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
        return Weaver.callOriginal();
    }

}
