package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import junit.framework.TestCase;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NRTracerBuilderTest extends TestCase {
    final TracerSharedState TRACER_SHARED_STATE = new TracerSharedState(Clock.getDefault(), IdGenerator.random(),
            Resource.empty(), () -> SpanLimits.getDefault(), Sampler.alwaysOn(), Collections.emptyList());

    public void testBuild() {
        Tracer tracer = new NRTracerBuilder(NewRelic.getAgent().getConfig(), "test-lib",
                TRACER_SHARED_STATE).build();
        assertTrue(tracer.getClass().getName(), tracer.getClass().getName().startsWith(
                "io.opentelemetry.sdk.trace.NRTracerBuilder"));
    }

    public void testBuildDisabled() {
        Config config = mock(Config.class);
        when(config.getValue("opentelemetry.instrumentation.test-lib.enabled")).thenReturn(false);
        Tracer tracer = new NRTracerBuilder(config, "test-lib",
                TRACER_SHARED_STATE).build();
        assertSame(OpenTelemetry.noop().getTracer("dude"), tracer);
    }
}