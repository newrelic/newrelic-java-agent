package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.trace.TracerBuilder;

@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.sdk.trace.SdkTracerProvider")
public final class SdkTracerProvider_Instrumentation {
    public TracerBuilder tracerBuilder(String instrumentationScopeName) {
        Weaver.callOriginal();
        return new NRTracerBuilder();
    }
}
