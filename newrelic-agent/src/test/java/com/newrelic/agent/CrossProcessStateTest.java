/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CrossProcessStateTest {
    private final MockServiceManager serviceManager = new MockServiceManager();
    private final String encodingKey = getClass().getName();
    private OutboundHeaders outboundHeaders;
    private InboundHeaders inboundHeaders;
    private Transaction tx;
    private CrossProcessConfig config;

    private CrossProcessState cps;

    @Before
    public void setup() {

        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object> emptyMap(), Collections.<String, Object> emptyMap(), null)));
        ServiceFactory.setServiceManager(serviceManager);

        outboundHeaders = mock(OutboundHeaders.class);
        inboundHeaders = mock(InboundHeaders.class);
        config = mock(CrossProcessConfig.class);
        tx = mock(Transaction.class);

        when(inboundHeaders.getHeaderType()).thenReturn(HeaderType.HTTP);
        when(outboundHeaders.getHeaderType()).thenReturn(HeaderType.HTTP);
        when(config.getEncodingKey()).thenReturn(encodingKey);
        when(config.isCrossApplicationTracing()).thenReturn(true);
        when(tx.isIgnore()).thenReturn(false);
        when(tx.getDispatcher()).thenReturn(mock(Dispatcher.class));
        when(tx.getDispatcher().getRequest()).thenReturn(mock(Request.class));
        when(tx.getDispatcher().getRequest().getHeaderType()).thenReturn(HeaderType.HTTP);
        when(tx.getCrossProcessConfig()).thenReturn(config);
        when(tx.getInboundHeaderState()).thenReturn(new InboundHeaderState(tx, null));
        when(tx.getPriorityTransactionName()).thenReturn(
                PriorityTransactionName.create("Test", "TEST", TransactionNamePriority.NONE));
        when(tx.getApplicationName()).thenReturn("TestApp");
        when(tx.getLock()).thenReturn(new Object());

        cps = CrossProcessTransactionStateImpl.create(tx);

    }

    @After
    public void verifyMocks() {
        verify(tx, Mockito.atLeastOnce()).getDispatcher();
        verifyNoMoreInteractions(outboundHeaders);
        verifyNoMoreInteractions(inboundHeaders);
        verifyNoMoreInteractions(config);
        verifyNoMoreInteractions(tx);
    }

    @Test
    public void processOutboundResponseHeaders() {
        String incomingId = "6#66";
        String obfuscatedAppData = Obfuscator.obfuscateNameUsingKey(
                "[\"6#66\",\"TestTransaction\\/name\",0.0,0.0,12345,\"5001D\",false]", encodingKey);
        cps.processOutboundResponseHeaders(null, 0);

        TransactionStats txStats = mock(TransactionStats.class);
        TransactionActivity ta = mock(TransactionActivity.class);
        when(tx.getTransactionActivity()).thenReturn(ta);
        when(ta.getTransactionStats()).thenReturn(txStats);
        SimpleStatsEngine statsEngine = mock(SimpleStatsEngine.class);
        when(txStats.getUnscopedStats()).thenReturn(statsEngine);
        ResponseTimeStats stats = mock(ResponseTimeStats.class);
        when(statsEngine.getOrCreateResponseTimeStats(anyString())).thenReturn(stats);

        InboundHeaderState ihs = mock(InboundHeaderState.class);
        when(ihs.getClientCrossProcessId()).thenReturn(incomingId);
        when(ihs.isTrustedCatRequest()).thenReturn(true);
        when(tx.getInboundHeaderState()).thenReturn(ihs);

        AgentConfig agentConfig = mock(AgentConfig.class);
        DistributedTracingConfig distributedTracingConfig = mock(DistributedTracingConfig.class);
        when(distributedTracingConfig.isEnabled()).thenReturn(false);
        when(agentConfig.getDistributedTracingConfig()).thenReturn(distributedTracingConfig);
        when(tx.getAgentConfig()).thenReturn(agentConfig);

        PriorityTransactionName txName = mock(PriorityTransactionName.class);
        when(tx.getPriorityTransactionName()).thenReturn(txName);
        when(txName.getName()).thenReturn("TestTransaction/name");
        when(tx.getGuid()).thenReturn("5001D");

        when(config.getCrossProcessId()).thenReturn(incomingId);

        cps.processOutboundResponseHeaders(outboundHeaders, 12345);

        verify(outboundHeaders).setHeader(eq("X-NewRelic-App-Data"), eq(obfuscatedAppData));

        cps.processOutboundResponseHeaders(outboundHeaders, 12345);
        verify(outboundHeaders, Mockito.times(2)).getHeaderType();
        verifyNoMoreInteractions(outboundHeaders);

        verify(config, atLeastOnce()).isCrossApplicationTracing();
        verify(config, atLeastOnce()).getCrossProcessId();
        verify(config, atLeastOnce()).getEncodingKey();
        verify(tx, atLeastOnce()).getAgentConfig();
        verify(tx, atLeastOnce()).getCrossProcessConfig();
        verify(tx, atLeastOnce()).getInboundHeaderState();
        verify(tx, atLeastOnce()).isIgnore();
        verify(tx, atLeastOnce()).getLock();
        verify(tx, atLeastOnce()).getGuid();
        verify(tx, atLeastOnce()).freezeTransactionName();
        verify(tx, atLeastOnce()).getRunningDurationInNanos();
        verify(tx, atLeastOnce()).getExternalTime();
        verify(tx, atLeastOnce()).getPriorityTransactionName();
        verify(txName, atLeastOnce()).getName();
        verify(tx, atLeastOnce()).getTransactionActivity();
        verify(ta, atLeastOnce()).getTransactionStats();
        verify(txStats, atLeastOnce()).getUnscopedStats();
        verify(statsEngine, atLeastOnce()).getOrCreateResponseTimeStats(anyString());
        verify(stats, atLeastOnce()).recordResponseTime(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(txStats, statsEngine, stats, txName);
    }
}
