/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SlowQueryListener;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionThrowable;
import com.newrelic.agent.transaction.TransactionTimer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TransactionDataTest {

    private Transaction tx;

    @Before
    public void initializeTransaction() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null)));
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.setAttributesService(new AttributesService());

        tx = Mockito.mock(Transaction.class);
        Mockito.when(tx.getDispatcher()).thenReturn(Mockito.mock(Dispatcher.class));
        Mockito.when(tx.getPriorityTransactionName()).thenReturn(Mockito.mock(PriorityTransactionName.class));
        Mockito.when(tx.getRPMService()).thenReturn(Mockito.mock(IRPMService.class));
        Mockito.when(tx.getCrossProcessTransactionState()).thenReturn(Mockito.mock(CrossProcessTransactionState.class));
    }

    private TransactionData getTxData(Transaction tx) {
        int transactionSize = 0;
        return new TransactionData(tx, transactionSize);
    }

    @Test
    public void getDispatcher() {
        Dispatcher expected = Mockito.mock(Dispatcher.class);
        Mockito.when(tx.getDispatcher()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        Dispatcher result = txd.getDispatcher();
        Assert.assertSame(expected, result);
    }

    @Test
    public void getRootTracer() {
        Tracer expected = Mockito.mock(Tracer.class);
        Mockito.when(tx.getRootTracer()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        Tracer result = txd.getRootTracer();
        Assert.assertNotNull(result);
        Assert.assertSame(expected, result);
    }

    @Test
    public void getTracers() {
        Mockito.when(tx.getTracers()).thenReturn(Arrays.asList(Mockito.mock(Tracer.class)));
        TransactionData txd = getTxData(tx);
        Collection<Tracer> result = txd.getTracers();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
    }

    @Test
    public void getStartTime() {
        long expected = System.nanoTime();
        Mockito.when(tx.getWallClockStartTimeMs()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        long result = txd.getWallClockStartTimeMs();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getStartTimeInNanos() {
        long expected = System.nanoTime();
        TransactionTimer mock = Mockito.mock(TransactionTimer.class);
        Mockito.when(mock.getStartTimeInNanos()).thenReturn(expected);
        Mockito.when(tx.getTransactionTimer()).thenReturn(mock);
        TransactionData txd = getTxData(tx);
        long result = txd.getStartTimeInNanos();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getEndTimeInNanos() {
        long expected = System.nanoTime();
        TransactionTimer mock = Mockito.mock(TransactionTimer.class);
        Mockito.when(mock.getEndTimeInNanos()).thenReturn(expected);
        Mockito.when(tx.getTransactionTimer()).thenReturn(mock);
        TransactionData txd = getTxData(tx);
        long result = txd.getEndTimeInNanos();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getRequestUri() {
        String expected = String.valueOf(System.nanoTime());
        Mockito.when(tx.getDispatcher().getUri()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        String result = txd.getRequestUri(AgentConfigImpl.ATTRIBUTES);
        Assert.assertSame(expected, result);
    }

    @Test
    public void getResponseStatus() {
        int expected = (int) System.nanoTime();
        Mockito.when(tx.getStatus()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        int result = txd.getResponseStatus();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getStatusMessage() {
        String expected = String.valueOf(System.nanoTime());
        Mockito.when(tx.getStatusMessage()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        String result = txd.getStatusMessage();
        Assert.assertSame(expected, result);
    }

    @Test
    public void getApplicationName() {
        String expected = String.valueOf(System.nanoTime());
        Mockito.when(tx.getRPMService().getApplicationName()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        String result = txd.getApplicationName();
        Assert.assertSame(expected, result);
    }

    @Test
    public void getAgentConfig() {
        AgentConfig expected = Mockito.mock(AgentConfig.class);
        Mockito.when(tx.getAgentConfig()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        AgentConfig result = txd.getAgentConfig();
        Assert.assertSame(expected, result);
    }

    @Test
    public void getTransactionTracerConfig() {
        TransactionData txd = getTxData(tx);
        Assert.assertNull(txd.getTransactionTracerConfig());

        AgentConfig mock = Mockito.mock(AgentConfig.class);
        Mockito.when(tx.getAgentConfig()).thenReturn(mock);

        TransactionTracerConfig expected = Mockito.mock(TransactionTracerConfig.class);
        Mockito.when(tx.getAgentConfig().getTransactionTracerConfig()).thenReturn(expected);

        txd = getTxData(tx);
        TransactionTracerConfig result = txd.getTransactionTracerConfig();

        Assert.assertSame(expected, result);
    }

    @Test
    public void getParameters() {
        Map<String, Object> expected = new HashMap<>();
        Mockito.when(tx.getAgentAttributes()).thenReturn(expected);
        Map<String, Object> expected2 = new HashMap<>();
        Mockito.when(tx.getUserAttributes()).thenReturn(expected2);
        Map<String, String> intermediate3 = new HashMap<>();
        Map<String, Map<String, String>> expected3 = new HashMap<>();
        expected3.put("request.parameters.", intermediate3);
        Mockito.when(tx.getPrefixedAgentAttributes()).thenReturn(expected3);
        Map<String, Object> expected4 = new HashMap<>();
        Mockito.when(tx.getErrorAttributes()).thenReturn(expected4);
        TransactionData txd = getTxData(tx);
        Assert.assertSame(expected, txd.getAgentAttributes());
        Assert.assertSame(expected2, txd.getUserAttributes());
        Assert.assertSame(expected3, txd.getPrefixedAttributes());
        Assert.assertSame(expected4, txd.getErrorAttributes());
    }

    @Test
    public void getBlameMetricName() {
        String expected = String.valueOf(System.nanoTime());
        Mockito.when(tx.getPriorityTransactionName().getName()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        String result = txd.getBlameMetricName();
        Assert.assertSame(expected, result);
    }

    @Test
    public void getBlameOrRootMetricName() {
        String expected = String.valueOf(System.nanoTime());
        Tracer mock = Mockito.mock(Tracer.class);
        Mockito.when(mock.getMetricName()).thenReturn(expected);
        Mockito.when(tx.getRootTracer()).thenReturn(mock);

        TransactionData txd = getTxData(tx);
        String result = txd.getBlameOrRootMetricName();
        Assert.assertSame(expected, result);

        expected = String.valueOf(System.nanoTime());
        Mockito.when(tx.getPriorityTransactionName().getName()).thenReturn(expected);
        txd = getTxData(tx);
        result = txd.getBlameOrRootMetricName();
        Assert.assertSame(expected, result);
    }

    @Test
    public void getThrowable() {
        Throwable exception = new Exception();
        TransactionThrowable transactionThrowable = new TransactionThrowable(exception, false, null);
        Mockito.when(tx.getThrowable()).thenReturn(transactionThrowable);
        TransactionData txd = getTxData(tx);
        Assert.assertSame(exception, txd.getThrowable().throwable);
    }

    @Test
    public void getDurationInMillis() {
        long expected = System.nanoTime();
        TransactionTimer mock = Mockito.mock(TransactionTimer.class);
        Mockito.when(mock.getResponseTimeInMilliseconds()).thenReturn(expected);
        Mockito.when(tx.getTransactionTimer()).thenReturn(mock);
        TransactionData txd = getTxData(tx);
        long result = txd.getDurationInMillis();
        Assert.assertEquals(expected, result, 0);
    }

    @Test
    public void getDuration() {
        long expected = System.nanoTime();
        TransactionTimer mock = Mockito.mock(TransactionTimer.class);
        Mockito.when(mock.getResponseTimeInNanos()).thenReturn(expected);
        Mockito.when(tx.getTransactionTimer()).thenReturn(mock);
        TransactionData txd = getTxData(tx);
        long result = txd.getLegacyDuration();
        Assert.assertEquals(expected, result, 0);
    }

    @Test
    public void getGuid() {
        String expected = String.valueOf(System.nanoTime());
        Mockito.when(tx.getGuid()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        String result = txd.getGuid();
        Assert.assertSame(expected, result);
    }

    @Test
    public void isWebTransaction() {
        boolean expected = true;
        Dispatcher mock = Mockito.mock(Dispatcher.class);
        Mockito.when(tx.getDispatcher()).thenReturn(mock);
        Mockito.when(mock.isWebTransaction()).thenReturn(expected);
        TransactionData txd = getTxData(tx);
        boolean result = txd.isWebTransaction();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getSqlTracerListener() {
        Mockito.doReturn(null).when(tx).getSlowQueryListener(Mockito.anyBoolean());
        TransactionData txd = getTxData(tx);
        SlowQueryListener result = txd.getSlowQueryListener();
        Assert.assertNull(result);
    }

    @Test
    public void getSqlTracerListenerIfUsed() {
        SlowQueryListener expected = Mockito.mock(SlowQueryListener.class);
        Mockito.doReturn(expected).when(tx).getSlowQueryListener(Mockito.anyBoolean());
        TransactionData txd = getTxData(tx);
        SlowQueryListener result = txd.getSlowQueryListener();
        Assert.assertSame(expected, result);
    }

    @Test
    public void toString1() {
        String expected = String.valueOf(System.nanoTime());
        Mockito.when(tx.getDispatcher().getUri()).thenReturn(expected);

        long expectedLong = System.nanoTime();
        TransactionTimer mockTime = Mockito.mock(TransactionTimer.class);
        Mockito.when(mockTime.getResponseTimeInMilliseconds()).thenReturn(expectedLong);
        Mockito.when(tx.getTransactionTimer()).thenReturn(mockTime);
        Tracer mock = Mockito.mock(Tracer.class);
        Mockito.when(tx.getRootTracer()).thenReturn(mock);

        TransactionData txd = getTxData(tx);
        String result = txd.toString();
        Assert.assertEquals(MessageFormat.format("{0} {1}ms", expected, String.valueOf(expectedLong)), result);
    }

    @Test
    public void toString2() {
        Tracer dispatcherMock = Mockito.mock(Tracer.class);
        Mockito.when(tx.getRootTracer()).thenReturn(dispatcherMock);

        long expected = System.nanoTime();
        TransactionTimer mockTime = Mockito.mock(TransactionTimer.class);
        Mockito.when(mockTime.getResponseTimeInMilliseconds()).thenReturn(expected);
        Mockito.when(tx.getTransactionTimer()).thenReturn(mockTime);
        Tracer mock = Mockito.mock(Tracer.class);
        Mockito.when(tx.getRootTracer()).thenReturn(mock);

        Throwable ex = new Exception();
        Mockito.when(tx.getThrowable()).thenReturn(new TransactionThrowable(ex, false, null));

        TransactionData txd = getTxData(tx);
        String result = txd.toString();
        Assert.assertEquals(MessageFormat.format(" {0}ms {1}", String.valueOf(expected), ex), result);
    }

    @Test
    public void isApdexFrustrating_returnsTrue_whenErrorIsPresent() {
        Mockito.when(tx.isErrorReportableAndNotIgnored()).thenReturn(true);
        Mockito.when(tx.isErrorNotExpected()).thenReturn(true);

        TransactionData txd = getTxData(tx);
        Assert.assertTrue(txd.isApdexFrustrating());
    }

    @Test
    public void isApdexFrustrating_returnsFalse_whenNoErrorIsPresent() {
        Mockito.when(tx.isErrorReportableAndNotIgnored()).thenReturn(false);
        Mockito.when(tx.isErrorNotExpected()).thenReturn(false);

        TransactionData txd = getTxData(tx);
        Assert.assertFalse(txd.isApdexFrustrating());
    }
}
