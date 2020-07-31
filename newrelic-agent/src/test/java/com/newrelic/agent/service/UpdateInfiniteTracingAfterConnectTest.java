/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.InfiniteTracing;
import com.newrelic.agent.service.analytics.InfiniteTracingEnabledCheck;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UpdateInfiniteTracingAfterConnectTest {

    final String appName = "fakeyMcFakeyFake";
    final String runToken = "abc123";

    @Mock
    InfiniteTracing infiniteTracing;
    @Mock
    InfiniteTracingEnabledCheck infiniteTracingEnabledCheck;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInitAfterConnect() {
        when(infiniteTracingEnabledCheck.isEnabledAndSpanEventsEnabled()).thenReturn(true);

        UpdateInfiniteTracingAfterConnect testClass = new UpdateInfiniteTracingAfterConnect(infiniteTracingEnabledCheck, infiniteTracing);

        Map<String, String> headersData = new HashMap<>();
        testClass.onEstablished(appName, runToken, headersData);
        verify(infiniteTracing).setConnectionMetadata(runToken, headersData);
        verify(infiniteTracing).start();
    }

    @Test
    public void testNoStartWithoutEventsEnabled() {
        when(infiniteTracingEnabledCheck.isEnabledAndSpanEventsEnabled()).thenReturn(false);

        UpdateInfiniteTracingAfterConnect testClass = new UpdateInfiniteTracingAfterConnect(infiniteTracingEnabledCheck, infiniteTracing);

        testClass.onEstablished(appName, runToken, Collections.<String, String>emptyMap());
        verifyNoMoreInteractions(infiniteTracing);
    }
}