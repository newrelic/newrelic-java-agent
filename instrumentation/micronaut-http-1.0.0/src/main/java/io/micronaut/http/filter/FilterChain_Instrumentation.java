/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.filter;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import org.reactivestreams.Publisher;

@Weave(originalName = "io.micronaut.http.filter.FilterChain", type = MatchType.Interface)
public abstract class FilterChain_Instrumentation {

    @Trace
    public Publisher<? extends HttpResponse<?>> proceed(HttpRequest<?> request) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "FilterChain", getClass().getSimpleName(), "proceed");
        return Weaver.callOriginal();
    }
}
