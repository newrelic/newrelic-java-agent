/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.trace.TracerBuilder;

/**
 * Weaved to inject a New Relic Java agent implementation of an OpenTelemetry TracerBuilder
 */
@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.sdk.trace.SdkTracerProvider")
public final class SdkTracerProvider_Instrumentation {
    private final TracerSharedState sharedState = Weaver.callOriginal();

    public TracerBuilder tracerBuilder(String instrumentationScopeName) {
        final TracerBuilder tracerBuilder = Weaver.callOriginal();
        Config config = NewRelic.getAgent().getConfig();
        if (NRSpanBuilder.isSpanBuilderEnabled(config)) {
            // Generate the instrumentation module supportability metric
            NewRelic.incrementCounter("Supportability/Tracing/Java/OpenTelemetryBridge/enabled");
            // return our tracer builder instead of the OTel instance
            return new NRTracerBuilder(config, instrumentationScopeName, sharedState);
        }
        return tracerBuilder;
    }
}
