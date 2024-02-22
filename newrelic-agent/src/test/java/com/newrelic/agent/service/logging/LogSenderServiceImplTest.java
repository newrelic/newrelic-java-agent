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
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.StatsService;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
        logSenderService.addHarvestableToService(appName);
        logSenderService.configureHarvestables(60, 1);

        assertEquals(1, logSenderService.getMaxSamplesStored());
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