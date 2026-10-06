/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.metrics;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ServerlessApi;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SdkMeterProviderBuilderInstrumentationTest {

    private ServerlessApi savedServerlessApi;

    @Before
    public void setUp() {
        savedServerlessApi = AgentBridge.serverlessApi;
    }

    @After
    public void tearDown() {
        AgentBridge.serverlessApi = savedServerlessApi;
    }

    @Test
    public void registerMetricReader_whenServerlessModeEnabled_addsMetricReader() {
        ServerlessApi serverlessApi = mock(ServerlessApi.class);
        when(serverlessApi.isServerlessModeEnabled()).thenReturn(true);
        AgentBridge.serverlessApi = serverlessApi;

        MetricReader reader = mock(MetricReader.class);
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class)) {
            new SdkMeterProviderBuilder_Instrumentation().registerMetricReader(reader);
        }

        verify(serverlessApi).addMetricReader(eq(reader), any());
    }

    @Test
    public void registerMetricReader_registeredCollectorForceFlushesTheReader() {
        ServerlessApi serverlessApi = mock(ServerlessApi.class);
        when(serverlessApi.isServerlessModeEnabled()).thenReturn(true);
        AgentBridge.serverlessApi = serverlessApi;

        MetricReader reader = mock(MetricReader.class);
        AtomicReference<Consumer<Object>> capturedReader = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            capturedReader.set(invocation.getArgument(1));
            return null;
        }).when(serverlessApi).addMetricReader(eq(reader), any());

        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class)) {
            new SdkMeterProviderBuilder_Instrumentation().registerMetricReader(reader);
        }
        capturedReader.get().accept(reader);

        verify(reader).forceFlush();
    }

    @Test
    public void registerMetricReader_whenServerlessModeDisabled_doesNotAddMetricReader() {
        ServerlessApi serverlessApi = mock(ServerlessApi.class);
        when(serverlessApi.isServerlessModeEnabled()).thenReturn(false);
        AgentBridge.serverlessApi = serverlessApi;

        MetricReader reader = mock(MetricReader.class);
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class)) {
            new SdkMeterProviderBuilder_Instrumentation().registerMetricReader(reader);
        }

        verify(serverlessApi, never()).addMetricReader(any(), any());
    }

    @Test
    public void registerMetricReader_alwaysCallsOriginalMethod() {
        ServerlessApi serverlessApi = mock(ServerlessApi.class);
        when(serverlessApi.isServerlessModeEnabled()).thenReturn(false);
        AgentBridge.serverlessApi = serverlessApi;

        MetricReader reader = mock(MetricReader.class);
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class)) {
            new SdkMeterProviderBuilder_Instrumentation().registerMetricReader(reader);

            weaver.verify(Weaver::callOriginal);
        }
    }
}
