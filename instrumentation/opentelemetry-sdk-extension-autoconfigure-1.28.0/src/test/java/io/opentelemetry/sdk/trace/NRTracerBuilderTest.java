/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import junit.framework.TestCase;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;

import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_TRACES_EXCLUDE;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.OPENTELEMETRY_TRACES_INCLUDE;

public class NRTracerBuilderTest extends TestCase {
    final TracerSharedState TRACER_SHARED_STATE = new TracerSharedState(Clock.getDefault(), IdGenerator.random(),
            Resource.empty(), SpanLimits::getDefault, Sampler.alwaysOn(), Collections.emptyList());

    public void testBuild() {
        Tracer tracer = new NRTracerBuilder("test-lib",
                TRACER_SHARED_STATE).build();
        assertTrue(tracer.getClass().getName(), tracer.getClass().getName().startsWith(
                "io.opentelemetry.sdk.trace.NRTracerBuilder"));
    }

    public void testBuildDisabled() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_EXCLUDE, "")).thenReturn("test-lib");
        Mockito.when(mockAgent.getConfig().getValue(OPENTELEMETRY_TRACES_INCLUDE, "")).thenReturn("");

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            Tracer tracer = new NRTracerBuilder("test-lib",
                    TRACER_SHARED_STATE).build();
            assertSame(OpenTelemetry.noop().getTracer("dude"), tracer);
        }
    }
}
