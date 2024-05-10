package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.trace.TracerBuilder;

@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.sdk.trace.SdkTracerProvider")
public final class SdkTracerProvider_Instrumentation {
    private final TracerSharedState sharedState = Weaver.callOriginal();

    public TracerBuilder tracerBuilder(String instrumentationScopeName) {
        final TracerBuilder tracerBuilder = Weaver.callOriginal();
        if (NRSpanBuilder.isSpanBuilderEnabled(NewRelic.getAgent().getConfig())) {
            // return our tracer builder instead of the OTel instance
            return new NRTracerBuilder(NewRelic.getAgent().getConfig(), instrumentationScopeName, sharedState);
        }
        return tracerBuilder;
    }
}
