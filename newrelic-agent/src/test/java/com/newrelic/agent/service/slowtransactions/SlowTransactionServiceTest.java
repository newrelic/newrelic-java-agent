package com.newrelic.agent.service.slowtransactions;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SlowTransactionsConfig;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.transaction.PriorityTransactionName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SlowTransactionServiceTest {

    @Mock
    private AgentConfig agentConfig;

    @Mock
    private SlowTransactionsConfig slowTransactionsConfig;

    @Mock
    private ServiceManager serviceManager;

    @Mock
    private TransactionService transactionService;

    @Mock
    private HarvestService harvestService;

    @Mock
    private InsightsService insightsService;

    @Mock
    private ThreadMXBean threadMXBean;

    private SlowTransactionService service;

    @Before
    public void setup() throws Exception {
        when(agentConfig.getSlowTransactionsConfig()).thenReturn(slowTransactionsConfig);
        when(agentConfig.getMaxStackTraceLines()).thenReturn(5);
        when(slowTransactionsConfig.isEnabled()).thenReturn(true);
        when(slowTransactionsConfig.getThresholdMillis()).thenReturn(1000L);
        when(slowTransactionsConfig.evaluateCompletedTransactions()).thenReturn(true);

        when(serviceManager.getTransactionService()).thenReturn(transactionService);
        when(serviceManager.getHarvestService()).thenReturn(harvestService);
        when(serviceManager.getInsights()).thenReturn(insightsService);
        RPMServiceManager rpmServiceManager = mock(RPMServiceManager.class);
        when(serviceManager.getRPMServiceManager()).thenReturn(rpmServiceManager);
        IRPMService rpmService = mock(IRPMService.class);
        when(rpmServiceManager.getRPMService()).thenReturn(rpmService);
        when(rpmService.getApplicationName()).thenReturn("App name");

        ConfigService configService = mock(ConfigService.class);
        when(configService.getDefaultAgentConfig()).thenReturn(agentConfig);
        when(serviceManager.getConfigService()).thenReturn(configService);
        ServiceFactory.setServiceManager(serviceManager);

        service = spy(new SlowTransactionService(agentConfig, threadMXBean));
        service.start();
    }

    @Test
    public void startAndStop_Enabled() throws Exception {
        Mockito.reset(transactionService, harvestService);
        service = new SlowTransactionService(agentConfig);
        assertTrue(service.isEnabled());

        service.start();

        verify(transactionService).addTransactionListener(service);
        verify(harvestService).addHarvestListener(service);

        service.stop();

        verify(transactionService).removeTransactionListener(service);
        verify(harvestService).removeHarvestListener(service);
    }

    @Test
    public void startAndStop_Disabled() throws Exception {
        Mockito.reset(transactionService, harvestService);
        when(slowTransactionsConfig.isEnabled()).thenReturn(false);
        service = new SlowTransactionService(agentConfig);

        assertFalse(service.isEnabled());

        service.start();

        verify(transactionService, never()).addTransactionListener((ExtendedTransactionListener) any());
        verify(harvestService, never()).addHarvestListener(any());

        service.stop();

        verify(transactionService, never()).removeTransactionListener((ExtendedTransactionListener) any());
        verify(harvestService, never()).removeHarvestListener(any());
    }

    @Test
    public void dispatcherTransactionStartedCancelledFinished() {
        String t1Guid = UUID.randomUUID().toString();
        Transaction t1 = mockTransaction(t1Guid, 1);
        String t2Guid = UUID.randomUUID().toString();
        Transaction t2 = mockTransaction(t2Guid, 1);
        TransactionData t2Data = mock(TransactionData.class);
        when(t2Data.getGuid()).thenReturn(t2Guid);

        // Start t1, verify added to open
        service.dispatcherTransactionStarted(t1);
        assertEquals(ImmutableMap.of(t1Guid, t1), service.getOpenTransactions());

        // Start t2, verify added to open
        service.dispatcherTransactionStarted(t2);
        assertEquals(ImmutableMap.of(t1Guid, t1, t2Guid, t2), service.getOpenTransactions());

        // Cancel t1, verify removed from open
        service.dispatcherTransactionCancelled(t1);
        assertEquals(ImmutableMap.of(t2Guid, t2), service.getOpenTransactions());

        // Finish t2, verify removed from open
        service.dispatcherTransactionFinished(t2Data, mock(TransactionStats.class));
        assertEquals(Collections.emptyMap(), service.getOpenTransactions());
    }

    @Test
    public void beforeHarvest() {
        service.beforeHarvest("unsued", mock(StatsEngine.class));

        verify(service, times(1)).run();
    }

    @Test
    public void run_NoSlowTransactions() {
        // Start t1 and t2, which are both open for < 1000ms when run is called
        String t1Guid = UUID.randomUUID().toString();
        Transaction t1 = mockTransaction(t1Guid, 1);
        String t2Guid = UUID.randomUUID().toString();
        Transaction t2 = mockTransaction(t2Guid, 1);
        service.dispatcherTransactionStarted(t1);
        service.dispatcherTransactionStarted(t2);

        service.run();

        verify(insightsService, never()).recordCustomEvent(any(), any());
        assertEquals(ImmutableMap.of(t1Guid, t1, t2Guid, t2), service.getOpenTransactions());
    }

    @Test
    public void run_FindsSlowestTransaction() {
        // Start t1 and t2, which are both open for > 1000ms when run is called.
        // t1 has been open longer and should be reported
        String t1Guid = UUID.randomUUID().toString();
        Transaction t1 = mockTransaction(t1Guid, 2000);
        String t2Guid = UUID.randomUUID().toString();
        Transaction t2 = mockTransaction(t2Guid, 1500);
        service.dispatcherTransactionStarted(t1);
        service.dispatcherTransactionStarted(t2);

        Map<String, Object> expectedAttributes = new HashMap<>();
        expectedAttributes.put("guid", t1Guid);
        doReturn(expectedAttributes).when(service).extractMetadata(same(t1), anyLong());

        service.run();

        ArgumentCaptor<CustomInsightsEvent> eventCaptor = ArgumentCaptor.forClass(CustomInsightsEvent.class);
        verify(insightsService).storeEvent(eq("App name"), eventCaptor.capture());
        CustomInsightsEvent event = eventCaptor.getValue();
        assertEquals("SlowTransaction", event.getType());
        assertTrue(event.getTimestamp() > 0);
        assertEquals(expectedAttributes, event.getUserAttributesCopy());
        assertTrue(event.getPriority() >= 0);
    }

    @Test
    public void dispatcherTransactionFinished_withTxnThatExceedsThresholdAndNotPreviouslyReported_reportsAsSlow() {
        String t1Guid = UUID.randomUUID().toString();
        Transaction t1 = mockTransaction(t1Guid, 2000);
        service.dispatcherTransactionStarted(t1);

        Map<String, Object> expectedAttributes = new HashMap<>();
        expectedAttributes.put("guid", t1Guid);
        doReturn(expectedAttributes).when(service).extractMetadata(same(t1), anyLong());

        TransactionData mockTxnData = mock(TransactionData.class);
        when(mockTxnData.getGuid()).thenReturn(t1Guid);

        service.dispatcherTransactionFinished(mockTxnData, mock(TransactionStats.class));

        ArgumentCaptor<CustomInsightsEvent> eventCaptor = ArgumentCaptor.forClass(CustomInsightsEvent.class);
        verify(insightsService).storeEvent(eq("App name"), eventCaptor.capture());
        CustomInsightsEvent event = eventCaptor.getValue();
        assertEquals("SlowTransaction", event.getType());
        assertTrue(event.getTimestamp() > 0);
        assertEquals(expectedAttributes, event.getUserAttributesCopy());
        assertTrue(event.getPriority() >= 0);
    }

    @Test
    public void dispatcherTransactionFinished_withTxnThatExceedsThresholdAndPreviouslyReported_doesNotReportAgain() {
        String t1Guid = UUID.randomUUID().toString();

        // Simulate the transaction being reported via the harvest cycle by not calling
        // dispatcherTransactionStarted(..) which means the transaction never exists
        // in the transaction collection.

        TransactionData mockTxnData = mock(TransactionData.class);
        when(mockTxnData.getGuid()).thenReturn(t1Guid);

        service.dispatcherTransactionFinished(mockTxnData, mock(TransactionStats.class));

        verify(insightsService, times(0)).storeEvent(anyString(), any());
    }

    @Test
    public void extractMetadata() {
        String t1Guid = UUID.randomUUID().toString();
        Transaction t1 = mockTransaction(t1Guid, 2000);
        when(t1.getUserAttributes()).thenReturn(ImmutableMap.of("k1", "v1"));
        when(t1.getErrorAttributes()).thenReturn(ImmutableMap.of("k2", "v2"));
        when(t1.getAgentAttributes()).thenReturn(ImmutableMap.of("k3", "v3"));
        when(t1.getIntrinsicAttributes()).thenReturn(ImmutableMap.of("k4", "v4"));
        when(t1.getPriorityTransactionName()).thenReturn(PriorityTransactionName.create("t1 name", "t1 category", TransactionNamePriority.CUSTOM_HIGH));

        long threadId = 1L;
        when(t1.getInitiatingThreadId()).thenReturn(threadId);
        ThreadInfo threadInfo = mock(ThreadInfo.class);
        when(threadMXBean.getThreadInfo(threadId, 5)).thenReturn(threadInfo);
        when(threadInfo.getThreadName()).thenReturn("thread name");
        when(threadInfo.getThreadState()).thenReturn(Thread.State.BLOCKED);
        StackTraceElement[] stackTrace = new StackTraceElement[]{
                new StackTraceElement("declaring class 1", "method name 1", "file name 1", 1),
                new StackTraceElement("declaring class 2", "method name 2", "file name 2", 2)
        };
        when(threadInfo.getStackTrace()).thenReturn(stackTrace);

        Map<String, Object> attributes = service.extractMetadata(t1, 1000);
        assertEquals("v1", attributes.get("k1"));
        assertEquals("v2", attributes.get("k2"));
        assertEquals("v3", attributes.get("k3"));
        assertEquals("v4", attributes.get("k4"));
        assertEquals(t1Guid, attributes.get("guid"));
        assertEquals("t1 name", attributes.get("name"));
        assertEquals("t1 category", attributes.get("transactionType"));
        assertTrue((long) attributes.get("timestamp") > 0);
        assertTrue((long) attributes.get("elapsed_ms") > 0);
        assertEquals(1L, attributes.get("thread.id"));
        assertEquals("thread name", attributes.get("thread.name"));
        assertEquals("BLOCKED", attributes.get("thread.state"));
        String expectedStackTrace = "declaring class 1.method name 1(file name 1:1)\n" +
                "\tat declaring class 2.method name 2(file name 2:2)\n";
        assertEquals(expectedStackTrace, attributes.get("code.stacktrace"));
    }

    private static Transaction mockTransaction(String guid, long startedAtOffsetMillis) {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getGuid()).thenReturn(guid);
        when(transaction.getWallClockStartTimeMs()).thenReturn(System.currentTimeMillis() - startedAtOffsetMillis);
        return transaction;
    }

}
