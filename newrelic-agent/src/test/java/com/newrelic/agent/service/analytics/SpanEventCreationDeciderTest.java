/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.InfiniteTracingConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpanEventCreationDeciderTest {
    @Mock
    ConfigService configService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldAlwaysSayYesIfConfigEnabled() {
        mockInfiniteTracing(true);

        TransactionData mockTransactionData = mock(TransactionData.class);
        when(mockTransactionData.sampled()).thenReturn(true);

        SpanEventCreationDecider target = new SpanEventCreationDecider(configService);
        assertTrue(target.shouldCreateSpans(mockTransactionData));
    }

    @Test
    public void shouldSayYesIfConfigEnabledAndTransactionNotSampled() {
        mockInfiniteTracing(true);

        TransactionData mockTransactionData = mock(TransactionData.class);
        when(mockTransactionData.sampled()).thenReturn(false);

        SpanEventCreationDecider target = new SpanEventCreationDecider(configService);
        assertTrue(target.shouldCreateSpans(mockTransactionData));
    }

    @Test
    public void shouldSayYesIfConfigDisabledButTransactionSampled() {
        mockInfiniteTracing(false);

        TransactionData mockTransactionData = mock(TransactionData.class);
        when(mockTransactionData.sampled()).thenReturn(true);

        SpanEventCreationDecider target = new SpanEventCreationDecider(configService);
        assertTrue(target.shouldCreateSpans(mockTransactionData));
    }

    @Test
    public void shouldSayNoIfConfigDisabledAndTransactionNotSampled() {
        mockInfiniteTracing(false);

        TransactionData mockTransactionData = mock(TransactionData.class);
        when(mockTransactionData.sampled()).thenReturn(false);

        SpanEventCreationDecider target = new SpanEventCreationDecider(configService);
        assertFalse(target.shouldCreateSpans(mockTransactionData));
    }

    public void mockInfiniteTracing(boolean isEnabled) {
        InfiniteTracingConfig infiniteTracingConfig = mock(InfiniteTracingConfig.class);
        when(infiniteTracingConfig.isEnabled()).thenReturn(isEnabled);
        AgentConfig mockConfig = mock(AgentConfig.class);
        when(mockConfig.getInfiniteTracingConfig()).thenReturn(infiniteTracingConfig);

        when(configService.getDefaultAgentConfig()).thenReturn(mockConfig);
    }

}