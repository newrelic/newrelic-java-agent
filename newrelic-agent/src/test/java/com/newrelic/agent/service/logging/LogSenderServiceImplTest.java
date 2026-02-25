package com.newrelic.agent.service.logging;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.ExcludeIncludeFilter;
import com.newrelic.agent.attributes.ExcludeIncludeFilterImpl;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ApplicationLoggingConfigImpl;
import com.newrelic.agent.config.ApplicationLoggingForwardingConfig;
import com.newrelic.agent.config.ApplicationLoggingLocalDecoratingConfig;
import com.newrelic.agent.config.ApplicationLoggingMetricsConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.stats.StatsService;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogSenderServiceImplTest {

    private static final String appName = LogSenderServiceImplTest.class.getSimpleName() + "App";
    private static final HarvestService harvestService = Mockito.mock(HarvestService.class);
    private static final TransactionService txService = Mockito.mock(TransactionService.class);
    private static final StatsService statsService = Mockito.mock(StatsService.class);

    private static LogSenderServiceImpl createService() throws Exception {
        return createService(createConfig());
    }

    private static LogSenderServiceImpl createService(Map<String, Object> config) throws Exception {
        config = new HashMap<>(config);

        ServiceManager serviceManager = mock(ServiceManager.class);
        when(serviceManager.getHarvestService()).thenReturn(harvestService);
        when(serviceManager.getStatsService()).thenReturn(statsService);
        when(serviceManager.getTransactionService()).thenReturn(txService);
        when(serviceManager.getRPMServiceManager()).thenReturn(Mockito.mock(RPMServiceManager.class));
        when(serviceManager.getRPMServiceManager().getRPMService()).thenReturn(Mockito.mock(RPMService.class));
        when(serviceManager.getConfigService()).thenReturn(Mockito.mock(ConfigService.class));
        when(serviceManager.getConfigService().getDefaultAgentConfig()).thenReturn(AgentConfigImpl.createAgentConfig(config));
        when(serviceManager.getRPMServiceManager().getRPMService().getApplicationName()).thenReturn(appName);
        ServiceFactory.setServiceManager(serviceManager);

        Transaction transaction = Mockito.mock(Transaction.class);
        when(txService.getTransaction(false)).thenReturn(transaction);

        LogSenderServiceImpl logSenderService = new LogSenderServiceImpl();
        when(ServiceFactory.getServiceManager().getLogSenderService()).thenReturn(logSenderService);

        logSenderService.start();

        return logSenderService;
    }

    @Test
    public void testHarvestableConfigure() throws Exception {
        Map<String, Object> config = createConfig(true, 180);
        LogSenderServiceImpl logSenderService = createService(config);
        assertEquals(833, logSenderService.getMaxSamplesStored());
        assertEquals(5000, logSenderService.reportPeriodInMillis);

        logSenderService.addHarvestableToService(appName);
        logSenderService.configureHarvestables(60, 1);
        assertEquals(1, logSenderService.getMaxSamplesStored());
        assertEquals(60, logSenderService.reportPeriodInMillis);
    }

    @Test
    public void testHighSecurity() throws Exception {
        Map<String, Object> config = createConfig(true, 180);
        LogSenderServiceImpl logSenderService = createService(config);

        Transaction transaction = Mockito.mock(Transaction.class);
        when(ServiceFactory.getTransactionService().getTransaction(false)).thenReturn(transaction);

        LogSenderServiceImpl.TransactionLogs logs = new LogSenderServiceImpl.TransactionLogs(
                AgentConfigImpl.createAgentConfig(Collections.emptyMap()), allowAllFilter());
        when(transaction.getLogEventData()).thenReturn(logs);
        when(transaction.getApplicationName()).thenReturn(appName);
        when(transaction.isInProgress()).thenReturn(true);

        logSenderService.recordLogEvent(createAgentLogAttrs("field", "value"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field2", "value2"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field3", "value3"));

        MockRPMService analyticsData = new MockRPMService();
        when(ServiceFactory.getServiceManager().getRPMServiceManager().getRPMService(appName)).thenReturn(
                analyticsData);

        TransactionData transactionData = Mockito.mock(TransactionData.class);
        when(transactionData.getApplicationName()).thenReturn(appName);
        when(transactionData.getLogEventData()).thenReturn(logs);

        logSenderService.transactionListener.dispatcherTransactionFinished(transactionData, null);
        logSenderService.harvestHarvestables();

        assertEquals(0, analyticsData.getEvents().size());
        assertEquals(0, logs.getEventsForTesting().size());
    }

    @Test
    public void testNoTransaction() throws Exception {
        LogSenderServiceImpl logSenderService = createService();

        logSenderService.addHarvestableToService(appName);

        verify(txService, times(1)).addTransactionListener(logSenderService.transactionListener);

        logSenderService.recordLogEvent(createAgentLogAttrs("field", "value"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field2", "value2"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field3", "value3"));

        MockRPMService analyticsData = new MockRPMService();
        when(ServiceFactory.getServiceManager().getRPMServiceManager().getOrCreateRPMService(appName)).thenReturn(
                analyticsData);

        logSenderService.harvestHarvestables();

        assertEquals(3, analyticsData.getEvents().size());

        logSenderService.stop();

        verify(txService, times(1)).removeTransactionListener(logSenderService.transactionListener);
    }

    @Test
    public void testWithTransaction() throws Exception {
        LogSenderServiceImpl logSenderService = createService(createConfig(null, 180));
        Transaction transaction = Mockito.mock(Transaction.class);
        when(ServiceFactory.getTransactionService().getTransaction(false)).thenReturn(transaction);

        LogSenderServiceImpl.TransactionLogs logs = new LogSenderServiceImpl.TransactionLogs(
                AgentConfigImpl.createAgentConfig(Collections.emptyMap()), allowAllFilter());
        when(transaction.getLogEventData()).thenReturn(logs);
        when(transaction.getApplicationName()).thenReturn(appName);
        when(transaction.isInProgress()).thenReturn(true);

        logSenderService.recordLogEvent(createAgentLogAttrs("field", "value"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field2", "value2"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field3", "value3"));

        MockRPMService analyticsData = new MockRPMService();
        when(ServiceFactory.getServiceManager().getRPMServiceManager().getOrCreateRPMService(appName)).thenReturn(
                analyticsData);

        logSenderService.harvestHarvestables();

        assertEquals(0, analyticsData.getEvents().size());
        assertEquals(3, logs.getEventsForTesting().size());
    }

    @Test
    public void testTransactionLogsMaxSamplesStoredIs0() throws Exception{
        LogSenderServiceImpl logSenderService = createService(createConfig(null, 180));
        Transaction transaction = Mockito.mock(Transaction.class);
        when(ServiceFactory.getTransactionService().getTransaction(false)).thenReturn(transaction);

        Map<String, Object> settings = Collections.singletonMap("application_logging", Collections.singletonMap("forwarding", Collections.singletonMap("max_samples_stored", 0)));
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(settings);
        LogSenderServiceImpl.TransactionLogs logs = new LogSenderServiceImpl.TransactionLogs(agentConfig, allowAllFilter());
        when(transaction.getLogEventData()).thenReturn(logs);
        when(transaction.getApplicationName()).thenReturn(appName);
        when(transaction.isInProgress()).thenReturn(true);

        logSenderService.recordLogEvent(createAgentLogAttrs("field", "value"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field2", "value2"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field3", "value3"));

        MockRPMService analyticsData = new MockRPMService();
        when(ServiceFactory.getServiceManager().getRPMServiceManager().getOrCreateRPMService(appName)).thenReturn(
                analyticsData);

        logSenderService.harvestHarvestables();

        assertEquals(0, analyticsData.getEvents().size());
        assertEquals(0, logs.getEventsForTesting().size());
    }

    @Test
    public void testTransactionHarvest() throws Exception {
        LogSenderServiceImpl logSenderService = createService(createConfig(null, 180));
        logSenderService.addHarvestableToService(appName);

        Transaction transaction = Mockito.mock(Transaction.class);
        when(ServiceFactory.getTransactionService().getTransaction(false)).thenReturn(transaction);

        LogSenderServiceImpl.TransactionLogs logs = new LogSenderServiceImpl.TransactionLogs(
                AgentConfigImpl.createAgentConfig(Collections.emptyMap()), allowAllFilter());
        when(transaction.getLogEventData()).thenReturn(logs);
        when(transaction.getApplicationName()).thenReturn(appName);
        when(transaction.isInProgress()).thenReturn(true);

        logSenderService.recordLogEvent(createAgentLogAttrs("field", "value"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field2", "value2"));
        logSenderService.recordLogEvent(createAgentLogAttrs("field3", "value3"));

        // these should be filtered out
        logSenderService.recordLogEvent(null);
        logSenderService.recordLogEvent(Collections.emptyMap());

        MockRPMService analyticsData = new MockRPMService();
        when(ServiceFactory.getServiceManager().getRPMServiceManager().getOrCreateRPMService(appName)).thenReturn(
                analyticsData);

        TransactionData transactionData = Mockito.mock(TransactionData.class);
        when(transactionData.getApplicationName()).thenReturn(appName);
        when(transactionData.getLogEventData()).thenReturn(logs);

        logSenderService.transactionListener.dispatcherTransactionFinished(transactionData, null);
        logSenderService.harvestHarvestables();

        logSenderService.harvestHarvestables();

        assertEquals(3, analyticsData.getEvents().size());
    }

    @Test
    public void testMaxSamplesStored() throws Exception {
        // Config value gets scaled by report period: configValue * (reportPeriodMs / 60000)
        // With reportPeriod=5000ms: 100 * (5000/60000) = 100 * 0.0833 = 8 (rounded down)
        int configMaxSamples = 100;
        int expectedMaxSamples = 8; // After scaling with 5000ms report period

        Map<String, Object> config = createConfig(null, 180, (long) configMaxSamples);
        LogSenderServiceImpl logSenderService = createService(config);
        logSenderService.addHarvestableToService(appName);

        // Verify the scaled maxSamplesStored
        assertEquals("Config value should be scaled by report period", expectedMaxSamples, logSenderService.getMaxSamplesStored());

        // Record more logs than the max samples limit (without active transaction, they go to reservoir)
        for (int i = 0; i < expectedMaxSamples * 2; i++) {
            logSenderService.recordLogEvent(createAgentLogAttrs("field" + i, "value" + i));
        }

        MockRPMService rpmService = new MockRPMService();
        when(ServiceFactory.getServiceManager().getRPMServiceManager().getOrCreateRPMService(appName)).thenReturn(rpmService);

        // Harvest the events
        logSenderService.harvestHarvestables();

        // The reservoir should have limited to maxSamples
        assertEquals("Should send at most maxSamples events", expectedMaxSamples, rpmService.getEvents().size());

        // Note: Unlike TransactionEventsService, LogSenderService doesn't pass a separate "eventsSeen" count
        // to the RPM service. It only sends the actual events that fit in the reservoir.
        // So logSenderEventsSeen will equal the number of events sent, not the number attempted.
        assertEquals("Should track sent events", expectedMaxSamples, rpmService.getLogSenderEventsSeen());
    }

    @Test
    public void testMultipleAppNames() throws Exception {
        LogSenderServiceImpl logSenderService = createService();
        logSenderService.addHarvestableToService(appName);

        String appName2 = "SecondApp";
        String appName3 = "ThirdApp";

        // Record logs for first app
        logSenderService.recordLogEvent(createAgentLogAttrs("app1-field", "app1-value"));

        // Get reservoirs for each app - this triggers lazy initialization
        DistributedSamplingPriorityQueue<LogEvent> reservoir1 = logSenderService.getReservoir(appName);
        DistributedSamplingPriorityQueue<LogEvent> reservoir2 = logSenderService.getReservoir(appName2);
        DistributedSamplingPriorityQueue<LogEvent> reservoir3 = logSenderService.getReservoir(appName3);

        // Verify separate reservoirs were created
        assertNotNull(reservoir1);
        assertNotNull(reservoir2);
        assertNotNull(reservoir3);

        // Verify they are different instances (each app gets its own reservoir)
        assertNotNull("reservoir1 and reservoir2 should be different", reservoir1 != reservoir2 ? reservoir1 : null);
        assertNotNull("reservoir2 and reservoir3 should be different", reservoir2 != reservoir3 ? reservoir2 : null);
        assertNotNull("reservoir1 and reservoir3 should be different", reservoir1 != reservoir3 ? reservoir1 : null);

        // Verify first app has 1 event (from earlier recordLogEvent call)
        assertEquals(1, reservoir1.size());

        // Verify other apps start empty (lazy initialization creates empty reservoirs)
        assertEquals(0, reservoir2.size());
        assertEquals(0, reservoir3.size());
    }

    private static Map<String, Object> createConfig() {
        return createConfig(null, null, null);
    }

    private static Map<String, Object> createConfig(Boolean highSecurity, Integer asyncTimeout) {
        return createConfig(highSecurity, asyncTimeout, null);
    }

    private static Map<String, Object> createConfig(Boolean highSecurity, Integer asyncTimeout, Long maxSamplesStored) {
        Map<String, Object> subForwardingMap = new HashMap<>();
        subForwardingMap.put(ApplicationLoggingForwardingConfig.ENABLED, true);
        subForwardingMap.put(ApplicationLoggingForwardingConfig.MAX_SAMPLES_STORED, maxSamplesStored);

        Map<String, Object> subMetricMap = new HashMap<>();
        subMetricMap.put(ApplicationLoggingMetricsConfig.ENABLED, true);

        Map<String, Object> subDecoratingMap = new HashMap<>();
        subDecoratingMap.put(ApplicationLoggingLocalDecoratingConfig.ENABLED, true);

        Map<String, Object> loggingMap = new HashMap<>();
        loggingMap.put(ApplicationLoggingConfigImpl.FORWARDING, subForwardingMap);
        loggingMap.put(ApplicationLoggingConfigImpl.METRICS, subMetricMap);
        loggingMap.put(ApplicationLoggingConfigImpl.LOCAL_DECORATING, subDecoratingMap);

        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APPLICATION_LOGGING, loggingMap);
        if (highSecurity != null) {
            config.put(AgentConfigImpl.HIGH_SECURITY, highSecurity);
        }
        if (asyncTimeout != null) {
            config.put(AgentConfigImpl.ASYNC_TIMEOUT, asyncTimeout);
        }
        config.put(AgentConfigImpl.APP_NAME, appName);
        return config;
    }

    private static Map<LogAttributeKey, Object> createAgentLogAttrs(String key, String value) {
        LogAttributeKey logKey = new LogAttributeKey(key, LogAttributeType.AGENT);
        return ImmutableMap.of(logKey, value);
    }

    private ExcludeIncludeFilter allowAllFilter() {
        return new ExcludeIncludeFilterImpl("allowAll", Collections.emptySet(), Collections.emptySet());
    }
}