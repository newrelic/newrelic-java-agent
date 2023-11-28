/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DataSenderConfig;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.LabelsConfig;
import com.newrelic.agent.config.NormalizationRuleConfig;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.errors.DeadlockTraceError;
import com.newrelic.agent.errors.ErrorMessageReplacer;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.errors.ThrowableError;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.normalization.NormalizationRule;
import com.newrelic.agent.normalization.NormalizationRuleFactory;
import com.newrelic.agent.profile.IProfile;
import com.newrelic.agent.profile.Profile;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.profile.ProfileSampler;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.rpm.RPMConnectionServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.StatsTest;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.transaction.TransactionNamingScheme;
import com.newrelic.agent.transport.DataSender;
import com.newrelic.agent.transport.DataSenderFactory;
import com.newrelic.agent.transport.DataSenderListener;
import com.newrelic.agent.transport.DataSenderWriter;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.transport.IDataSenderFactory;
import com.newrelic.agent.utilization.UtilizationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import javax.servlet.http.HttpServletResponse;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * This integration test runs against a mock collector running in embedded jetty, to test the protocol between the Java
 * agent and the RPM service.
 */
public class RPMServiceTest {

    private static final int PROXY_PORT = 3128;
    private static final String PROXY_HOST = "test-http-proxy.pdx.vm.datanerd.us";

    private static final int MOCK_COLLECTOR_HTTP_PORT = 1120;
    private static final int MOCK_COLLECTOR_HTTPS_PORT = 1123;

    private IDataSenderFactory saveDataSenderFactory;
    private final boolean skipProxy = !reachable(PROXY_HOST, PROXY_PORT);
    private MockCollector mockCollector;

    public static Map<String, Object> createStagingMap(boolean https, boolean isHighSec) {
        return createStagingMap(https, isHighSec, false);
    }

    public static Map<String, Object> createStagingMap(boolean https, boolean isHighSec, boolean putForDataSend) {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTPS_PORT);
        map.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        map.put("ca_bundle_path", "src/test/resources/server.cer");
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        map.put(AgentConfigImpl.LABELS, "one:two;three:four");
        if (isHighSec) {
            map.put("high_security", true);
        }
        if (putForDataSend) {
            map.put("put_for_data_send", true);
        }
        return map;
    }

    @Before
    public void beforeTest() throws Exception {
        mockCollector = new MockCollector(MOCK_COLLECTOR_HTTP_PORT, MOCK_COLLECTOR_HTTPS_PORT);
        saveDataSenderFactory = DataSenderFactory.getDataSenderFactory();
    }

    private boolean reachable(String host, int port) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("http", host, port, "/").openConnection();
            connection.setConnectTimeout(100);
            connection.setReadTimeout(100);
            connection.setRequestMethod("HEAD");
            connection.getResponseCode();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @After
    public void afterTest() throws Exception {
        DataSenderFactory.setDataSenderFactory(saveDataSenderFactory);
        if (mockCollector != null) {
            mockCollector.stop();
        }
    }

    private void createServiceManager(Map<String, Object> config) {
        createServiceManager(AgentConfigImpl.createAgentConfig(config), config);
    }

    private void createServiceManager(AgentConfig config, Map<String, Object> localSettings) {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(config, localSettings);
        serviceManager.setConfigService(configService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        DatabaseService dbService = new DatabaseService();
        serviceManager.setDatabaseService(dbService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        RPMConnectionService rpmConnectionService = new RPMConnectionServiceImpl();
        serviceManager.setRPMConnectionService(rpmConnectionService);

        ProfilerService profilerService = new ProfilerService();
        serviceManager.setProfilerService(profilerService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        AttributesService attService = new AttributesService();
        serviceManager.setAttributesService(attService);

        UtilizationService utilService = new UtilizationService();
        serviceManager.setUtilizationService(utilService);

        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = mock(TransactionDataToDistributedTraceIntrinsics.class);
        when(transactionDataToDistributedTraceIntrinsics.buildDistributedTracingIntrinsics(any(TransactionData.class), anyBoolean())).thenReturn(Collections.<String, Object>emptyMap());
        TransactionEventsService transactionEventsService = new TransactionEventsService(transactionDataToDistributedTraceIntrinsics);
        serviceManager.setTransactionEventsService(transactionEventsService);

        serviceManager.setSpansEventService(mock(SpanEventsServiceImpl.class));
    }

    @Test(timeout = 30000)
    public void testLaunch() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doTestLaunch();
    }

    @Test(timeout = 30000)
    public void testLaunchWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestLaunch();
    }

    private void doTestLaunch() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void testStartupOptions() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        createServiceManager(config);
        doTestStartupOptions();
    }

    @Test(timeout = 30000)
    public void testStartupOptionsWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(false, false, true);
        createServiceManager(config);
        doTestStartupOptions();
    }

    @SuppressWarnings("unchecked")
    private void doTestStartupOptions() throws Exception {
        RPMService svc = null;
        try {
            List<String> appNames = singletonList("MyApplication");
            svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
            Map<String, Object> values = svc.getStartOptions();
            Object env = values.get("environment");
            assertNotNull(env);
            Object settings = values.get("settings");
            assertNotNull(settings);
            Map<String, Object> theSettings = (Map<String, Object>) settings;
            // these two properties need to be sent up for rum
            assertEquals("rum", theSettings.get("browser_monitoring.loader"));
            assertNotNull(theSettings.get("browser_monitoring.debug"));
            assertNotNull(values.get("high_security"));
            assertFalse((Boolean) values.get("high_security"));
            assertEquals(ImmutableMap.of("one", "two", "three", "four"), ((LabelsConfig) values.get("labels")).getLabels());
        } finally {
            if (svc != null) {
                svc.shutdown();
            }
        }
    }

    @Test(timeout = 30000)
    public void testStartupOptionsHighSecurity() throws Exception {
        Map<String, Object> config = createStagingMap(true, true);
        createServiceManager(config);
        doTestStartupOptionsHighSecurity();
    }

    @Test(timeout = 30000)
    public void testStartupOptionsHighSecurityWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, true, true);
        createServiceManager(config);
        doTestStartupOptionsHighSecurity();
    }

    @SuppressWarnings("unchecked")
    private void doTestStartupOptionsHighSecurity() throws Exception {
        RPMService svc = null;
        try {
            List<String> appNames = singletonList("MyApplication");
            svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
            Map<String, Object> values = svc.getStartOptions();
            Object env = values.get("environment");
            assertNotNull(env);
            Object settings = values.get("settings");
            assertNotNull(settings);
            Map<String, Object> theSettings = (Map<String, Object>) settings;
            // these two properties need to be sent up for rum
            assertEquals("rum", theSettings.get("browser_monitoring.loader"));
            assertNotNull(theSettings.get("browser_monitoring.debug"));
            assertNotNull(values.get("high_security"));
            assertTrue((Boolean) values.get("high_security"));
            assertEquals(ImmutableMap.of("one", "two", "three", "four"), ((LabelsConfig) values.get("labels")).getLabels());
        } finally {
            if (svc != null) {
                svc.shutdown();
            }
        }
    }

    @Test(timeout = 30000)
    public void testMetricNormalizationRules() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doTestMetricNormalizationRules();
    }

    @Test(timeout = 30000)
    public void testMetricNormalizationRulesWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestMetricNormalizationRules();
    }

    private void doTestMetricNormalizationRules() throws Exception {
        List<String> appNames = new ArrayList<>(1);
        appNames.add("MyApplication");
        final AtomicReference<Map<String, Object>> data = new AtomicReference<>();
        RPMService svc = new RPMService(appNames, new ConnectionConfigListener() {
            @Override
            public AgentConfig connected(IRPMService rpmService, Map<String, Object> connectionInfo) {
                data.set(connectionInfo);
                return null;
            }
        }, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();
        List<NormalizationRule> rules = NormalizationRuleFactory.getUrlRules("MyApplication", new NormalizationRuleConfig(data.get()).getUrlRules());

        assertFalse("We are not getting metric normalization rules!", rules.isEmpty());

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void testConnectionListener() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doTestConnectionListener();
    }

    @Test(timeout = 30000)
    public void testConnectionListenerWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestConnectionListener();
    }

    private void doTestConnectionListener() throws Exception {
        final AtomicBoolean connected = new AtomicBoolean();
        ConnectionListener connectionListener = new ConnectionListener() {

            @Override
            public void connected(IRPMService rpmService, AgentConfig agentConfig) {
                connected.set(true);
            }

            @Override
            public void disconnected(IRPMService rpmService) {
            }

        };

        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, connectionListener, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();
        assertTrue(connected.get());

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void testLaunchThenAddPort() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        createServiceManager(config);
        doTestLaunchThenAddPort();
    }

    @Test(timeout = 30000)
    public void testLaunchThenAddPortWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(false, false, true);
        createServiceManager(config);
        doTestLaunchThenAddPort();
    }

    private void doTestLaunchThenAddPort() throws Exception {
        final AtomicReference<CountDownLatch> connectLatch = new AtomicReference<>(new CountDownLatch(2));
        final AtomicReference<CountDownLatch> shutdownLatch = new AtomicReference<>(new CountDownLatch(2));

        IDataSenderFactory dataSenderFactory = new IDataSenderFactory() {
            @Override
            public DataSender create(DataSenderConfig config) {
                return createMockDataSender(config);
            }

            @Override
            public DataSender create(DataSenderConfig config, DataSenderListener dataSenderListener) {
                return createMockDataSender(config);
            }

            private MockDataSender createMockDataSender(DataSenderConfig config) {
                return new MockDataSender(config) {
                    @Override
                    public Map<String, Object> connect(Map<String, Object> startupOptions) throws Exception {
                        connectLatch.get().countDown();
                        return super.connect(startupOptions);
                    }

                    @Override
                    public void shutdown(long timeMillis) throws Exception {
                        shutdownLatch.get().countDown();
                        super.shutdown(timeMillis);
                    }
                };
            }
        };

        DataSenderFactory.setDataSenderFactory(dataSenderFactory);

        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.start();
        svc.launch();

        assertEquals(1L, connectLatch.get().getCount());
        assertEquals(2L, shutdownLatch.get().getCount());

        ServiceFactory.getEnvironmentService().getEnvironment().setServerPort(8080);
        connectLatch.get().await(30, TimeUnit.SECONDS);

        assertEquals(0L, connectLatch.get().getCount());
        assertEquals(1L, shutdownLatch.get().getCount());

        svc.shutdown();
        assertEquals(0L, shutdownLatch.get().getCount());
    }

    @Test(timeout = 30000)
    public void testLaunchAndRestart() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doTestLaunchAndRestart();
    }

    @Test(timeout = 30000)
    public void testLaunchAndRestartWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestLaunchAndRestart();
    }

    private void doTestLaunchAndRestart() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        assertTrue(ServiceFactory.getTransactionTraceService().isEnabled());

        svc.reconnect();
        svc.launch();

        assertTrue(ServiceFactory.getTransactionTraceService().isEnabled());

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void harvest() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doHarvest();
    }

    @Test(timeout = 30000)
    public void harvestWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doHarvest();
    }

    private void doHarvest() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();
        synchronized (this) {
            wait(1000);
        }

        StatsEngine harvestStatsEngine = new StatsEngineImpl();
        try {
            for (int i = 0; i < 1000; i++) {
                harvestStatsEngine.getResponseTimeStats(MetricNames.EXTERNAL_ALL).recordResponseTime(66,
                        TimeUnit.MILLISECONDS);
            }
            svc.harvest(harvestStatsEngine);
            Stats stats3 = harvestStatsEngine.getStats(MetricNames.AGENT_METRICS_COUNT);
            assertEquals(0, stats3.getCallCount());

            ResponseTimeStats stats = harvestStatsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_METRIC_HARVEST_TRANSMIT);
            assertEquals(1, stats.getCallCount());
            assertTrue(stats.getTotal() > 0);
            Stats stats2 = harvestStatsEngine.getStats(MetricNames.SUPPORTABILITY_METRIC_HARVEST_COUNT);
            assertEquals(1, stats2.getCallCount());
        } finally {
            svc.shutdown();
        }
    }

    @Test(timeout = 30000)
    public void testTransactionTraces() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        createServiceManager(config);
        doTestTransactionTraces();
    }

    @Test(timeout = 30000)
    public void testTransactionTracesWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(false, false, true);
        createServiceManager(config);
        doTestTransactionTraces();
    }

    private void doTestTransactionTraces() throws Exception {
        MockDataSenderFactory dataSenderFactory = new MockDataSenderFactory();
        DataSenderFactory.setDataSenderFactory(dataSenderFactory);
        Transaction.clearTransaction();

        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "test", "()V");
        Tracer rootTracer = new BasicRequestRootTracer(Transaction.getTransaction(), sig, this, null, null,
                new SimpleMetricNameFormat("/test"));
        AgentConfig iAgentConfig = mock(AgentConfig.class);

        TransactionData data = new TransactionDataTestBuilder("unittest", iAgentConfig, rootTracer)
                .setRequestUri("/unittest")
                .build();
        List<TransactionTrace> traces = singletonList(TransactionTrace.getTransactionTrace(data));

        try {
            System.err.println("Transaction trace.  JSON: " + DataSenderWriter.toJSONString(traces));
            svc.sendTransactionTraceData(traces);
        } catch (Exception e) {
            System.err.println("Error sending transaction trace.  JSON: " + DataSenderWriter.toJSONString(traces));
            throw e;
        }

        assertEquals(traces, dataSenderFactory.getLastDataSender().getTraces());

        svc.shutdown();
    }

    @Test
    @Ignore("This test needs server side config and is therefore brittle.")
    public void transactionNamingScheme() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        createServiceManager(config);

        List<String> appNames = singletonList("Java CMS");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        assertEquals(TransactionNamingScheme.LEGACY, svc.getTransactionNamingScheme());
    }

    @Test(timeout = 30000)
    public void testSendLogEvents() throws Exception {
        Map<String, Object> map = createStagingMap(true, false);
        map.put("app_name", "Test");
        AgentConfig config = AgentConfigImpl.createAgentConfig(map);
        createServiceManager(config, map);
        doSendLogEvent();
    }

    @Test(timeout = 30000)
    public void testSendLogEventsWithPut() throws Exception {
        Map<String, Object> map = createStagingMap(true, false, true);
        map.put("app_name", "Test");
        AgentConfig config = AgentConfigImpl.createAgentConfig(map);
        createServiceManager(config, map);
        doSendLogEvent();
    }

    private void doSendLogEvent() throws Exception {
        MockDataSenderFactory dataSenderFactory = new MockDataSenderFactory();
        DataSenderFactory.setDataSenderFactory(dataSenderFactory);
        List<String> appNames = singletonList("Send Log Events Test App");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());

        svc.launch();

        LogEvent logEvent1 = new LogEvent(null, 1);
        LogEvent logEvent2 = new LogEvent(null, 2);
        List<LogEvent> logEvents = new ArrayList<>();
        logEvents.add(logEvent1);
        logEvents.add(logEvent2);

        svc.sendLogEvents(logEvents);

        List<LogEvent> seen = dataSenderFactory.getLastDataSender().getLogEvents();

        assertEquals("No log events sent currently", logEvents.size(), seen.size());
    }

    @Test(timeout = 30000)
    public void sendProfileData() throws Exception {
        Map<String, Object> map = createStagingMap(true, false);
        map.put("app_name", "Test");
        AgentConfig config = AgentConfigImpl.createAgentConfig(map);
        createServiceManager(config, map);
        doSendProfileData();
    }

    @Test(timeout = 30000)
    public void sendProfileDataWithPut() throws Exception {
        Map<String, Object> map = createStagingMap(true, false, true);
        map.put("app_name", "Test");
        AgentConfig config = AgentConfigImpl.createAgentConfig(map);
        createServiceManager(config, map);
        doSendProfileData();
    }

    private void doSendProfileData() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, Agent.isDebugEnabled(), null,
                null);
        Profile profile = new Profile(parameters);
        List<IProfile> profiles = new ArrayList<>();
        profiles.add(profile);
        ProfileSampler sampler = new ProfileSampler();
        sampler.sampleStackTraces(profiles);

        profile.start();
        profile.end();

        svc.launch();

        IProfile profile2 = new Profile(parameters);
        List<Long> ids = svc.sendProfileData(Arrays.<ProfileData>asList(profile, profile2));
        assertEquals(2, ids.size());
    }

    @Test(timeout = 30000)
    public void getApplicationName() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        createServiceManager(config);
        doGetApplicationName();
    }

    @Test(timeout = 30000)
    public void getApplicationNameWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(false, false, true);
        createServiceManager(config);
        doGetApplicationName();
    }

    private void doGetApplicationName() {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        assertEquals("MyApplication", svc.getApplicationName());
    }

    @Test(timeout = 30000)
    public void isMainApp() throws Exception {
        String appName = "My Application";
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTP_PORT);
        map.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        map.put(AgentConfigImpl.APP_NAME, appName);
        createServiceManager(map);
        doIsMainApp(appName, map);
    }

    @Test(timeout = 30000)
    public void isMainAppWithPut() throws Exception {
        String appName = "My Application";
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTP_PORT);
        map.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        map.put(AgentConfigImpl.PUT_FOR_DATA_SEND_PROPERTY, true);
        map.put(AgentConfigImpl.APP_NAME, appName);
        createServiceManager(map);
        doIsMainApp(appName, map);
    }

    private void doIsMainApp(String appName, Map<String, Object> map) {
        List<String> appNames = singletonList(appName);
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        assertTrue(svc.isMainApp());

        map.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, true);
        appNames = singletonList("Bogus");
        svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        assertFalse(svc.isMainApp());

        appNames = singletonList(appName);
        svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        assertTrue(svc.isMainApp());
    }

    @Test(timeout = 30000)
    public void getAgentCommands() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doGetAgentCommands();
    }

    @Test(timeout = 30000)
    public void getAgentCommandsWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doGetAgentCommands();
    }

    private void doGetAgentCommands() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        List<List<?>> commands = svc.getAgentCommands();

        assertEquals(0, commands.size());
    }

    @Test(timeout = 30000)
    public void sendEmptyCommandResults() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doSendEmptyCommandResults();
    }

    @Test(timeout = 30000)
    public void sendEmptyCommandResultsWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doSendEmptyCommandResults();
    }

    private void doSendEmptyCommandResults() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        Map<Long, Object> commandResults = new HashMap<>();
        svc.sendCommandResults(commandResults);
    }

    @Test(timeout = 30000)
    public void sendCommandResults() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doSendCommandResults();
    }

    @Test(timeout = 30000)
    public void sendCommandResultsWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doSendCommandResults();
    }

    private void doSendCommandResults() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        Map<Long, Object> commandResults = new HashMap<>();
        commandResults.put(8675309L, Collections.emptyMap()); // invalid id
        try {
            svc.sendCommandResults(commandResults);
        } catch (RuntimeException ignored) {
            //ignored
        }
    }

    @Test(timeout = 30000)
    public void multipleApplicationNames() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        createServiceManager(config);
        doMultipleApplicationNames();
    }

    @Test(timeout = 30000)
    public void multipleApplicationNamesWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(false, false, true);
        createServiceManager(config);
        doMultipleApplicationNames();
    }

    @SuppressWarnings("unchecked")
    private void doMultipleApplicationNames() throws Exception {
        MockDataSenderFactory dataSenderFactory = new MockDataSenderFactory();
        DataSenderFactory.setDataSenderFactory(dataSenderFactory);

        List<String> appNames = new ArrayList<>(2);
        appNames.add("MyApp1");
        appNames.add("MyApp2");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        assertEquals("MyApp1", svc.getApplicationName());
        svc.launch();

        Map<String, Object> startupOptions = dataSenderFactory.getLastDataSender().getStartupOtions();
        List<String> result = (List<String>) startupOptions.get("app_name");
        assertEquals(2, result.size());
        assertEquals("MyApp1", result.get(0));
        assertEquals("MyApp2", result.get(1));

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void testTracedErrors() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doTestTracedErrors();
    }

    @Test(timeout = 30000)
    public void testTracedErrorsWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestTracedErrors();
    }

    private void doTestTracedErrors() throws Exception {
        ErrorCollectorConfig config = mock(ErrorCollectorConfig.class);

        List<String> appNames = new ArrayList<>(1);
        appNames.add("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        final ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId(),
                100);
        Map<String, StackTraceElement[]> stackTraceMap = new HashMap<String, StackTraceElement[]>() {
            private static final long serialVersionUID = 1L;

            {
                put("dude", threadInfo.getStackTrace());
                put("dude1", threadInfo.getStackTrace());
            }
        };
        Map<String, String> parameters = Collections.emptyMap();
        svc.getErrorService().reportErrors(
                DeadlockTraceError.builder(config, null, System.currentTimeMillis())
                        .threadInfoAndStackTrace(threadInfo, stackTraceMap)
                        .errorAttributes(parameters)
                        .build(),
                ThrowableError.builder(config, null, "", new Exception("Test"), System.currentTimeMillis())
                        .build());

        svc.getErrorService().getAndClearTracedErrors();
        svc.shutdown();
    }

    @Test(timeout = 30000) // This test creates a large payload to send, lets give it some extra time
    public void testTracedErrorsSizeLimit() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doTestTracedErrorsSizeLimit();
    }

    @Test(timeout = 30000) // This test creates a large payload to send, lets give it some extra time
    public void testTracedErrorsSizeLimitWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestTracedErrorsSizeLimit();
    }

    private void doTestTracedErrorsSizeLimit() throws Exception {
        List<String> appNames = new ArrayList<>(1);
        appNames.add("MyApplication");

        final AtomicInteger errorSentCount = new AtomicInteger(0);
        RPMService svc = new RPMService(appNames, null, null, new DataSenderListener() {
            @Override
            public void dataSent(String method, String encoding, String uri, byte[] rawDataSent) {
                if (method.equals("error_data")) {
                    errorSentCount.incrementAndGet();

                    // Check that the raw data sent is less than the collector limit of 1MB (1000000 bytes)
                    assertTrue(rawDataSent.length < 1000000);
                }
            }

            @Override
            public void dataReceived(String method, String encoding, String uri, Map<?, ?> rawDataReceived) {
                if (method.equals("error_data")) {
                    // The collector should let us know it only recieved 2 error traces (instead of 5)
                    assertEquals(2L, rawDataReceived.get("return_value"));
                }
            }
        }, Collections.<AgentConnectionEstablishedListener>emptyList());
        ((MockRPMServiceManager) ServiceFactory.getRPMServiceManager()).setRPMService(svc);
        svc.launch();

        // Since the error limit per reporting period is currently 20 TracedErrors, lets add 5 traced errors that
        // are big enough to push the final traced error over the limit and thus prevent it from being sent.
        for (int i = 0; i < 5; i++) {
            // Each of these adds 249090 bytes, so we can successfully store 4 (996360 bytes -- 996468 with padding)
            // but the 5th should push it over the limit so we will end up cutting the array in half
            // (which rounds down to 2 elements).
            svc.getErrorService().reportError(new LargeStackThrowableError(null, "", new Exception("Test"),
                    System.currentTimeMillis(), null, null, null, null, null, 97500));
        }

        StatsEngineImpl harvestStatsEngine = new StatsEngineImpl();
        ((ErrorServiceImpl) svc.getErrorService()).harvestTracedErrors("MyApplication", harvestStatsEngine);
        svc.harvest(harvestStatsEngine); // This will collect the traced errors
        Thread.sleep(500);

        // one set of errors should get sent because the first will error out
        assertEquals(1, errorSentCount.get());

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void testLaunchHttps() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);
        doTestLaunchHttps();
    }

    @Test(timeout = 30000)
    public void testLaunchHttpsWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestLaunchHttps();
    }

    private void doTestLaunchHttps() throws Exception {
        List<String> appNames = new ArrayList<>(1);
        appNames.add("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void testStatusCodeSupportabilityMetrics() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);
        doTestStatusCodeSupportabilityMetrics();
    }

    private void doTestStatusCodeSupportabilityMetrics() throws Exception {
        List<String> appNames = new ArrayList<>(1);
        appNames.add("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();
        StatsEngine statsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest("MyApplication");
        MetricName metricName = MetricName.create("Supportability/Collector/HttpCode/200");
        assertTrue(statsEngine.getMetricNames().contains(metricName));
        svc.shutdown();
    }

    @Ignore
    @Test
    public void testLaunchProxyAuth() throws Exception {
        if (skipProxy) {
            return;
        }
        Map<String, Object> config = createStagingMap(true, false);

        config.put("proxy_host", PROXY_HOST);
        config.put("proxy_port", PROXY_PORT);
        config.put("proxy_user", "proxyuser");
        config.put("proxy_password", "proxyuser");

        createServiceManager(config);

        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        svc.shutdown();
    }

    @Test(expected = HttpError.class)
    public void testLaunchProxyAuthBadPassword() throws Exception {
        if (skipProxy) {
            throw new HttpError("The proxy isn't accessible.  Likely you're not on the VPN.",
                    HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED, 0);
        }
        Map<String, Object> config = createStagingMap(true, false);

        config.put("proxy_host", PROXY_HOST);
        config.put("proxy_port", 3128);
        config.put("proxy_user", "proxyuser");
        config.put("proxy_password", "dude");

        createServiceManager(config);

        doTestLaunchProxyAuthBadPassword();
    }

    @Test(expected = HttpError.class)
    public void testLaunchProxyAuthBadPasswordWithPut() throws Exception {
        if (skipProxy) {
            throw new HttpError("The proxy isn't accessible.  Likely you're not on the VPN.",
                    HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED, 0);
        }
        Map<String, Object> config = createStagingMap(true, false, true);

        config.put("proxy_host", PROXY_HOST);
        config.put("proxy_port", 3128);
        config.put("proxy_user", "proxyuser");
        config.put("proxy_password", "dude");

        createServiceManager(config);

        doTestLaunchProxyAuthBadPassword();
    }

    private void doTestLaunchProxyAuthBadPassword() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();
    }

    @Test
    @Ignore
    public void testLaunchProxy() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        config.put("proxy_host", "127.0.0.1");
        config.put("proxy_port", 8088);
        config.put("proxy_user", "dude");
        config.put("proxy_password", "test");

        createServiceManager(config);

        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        svc.shutdown();
    }

    @Test(timeout = 30000)
    public void testLaunchToSendMetricData() throws Exception {
        Map<String, Object> config = createStagingMap(true, false);
        createServiceManager(config);

        doTestLaunchToSendMetricData();
    }

    @Test(timeout = 30000)
    public void testLaunchToSendMetricDataWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(true, false, true);
        createServiceManager(config);

        doTestLaunchToSendMetricData();
    }

    private void doTestLaunchToSendMetricData() throws Exception {
        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();

        StatsEngine statsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest(null);
        List<MetricData> data = createMetricData(statsEngine, 10);
        assertNotNull(data);

        svc.shutdown();
    }

    private List<MetricData> createMetricData(StatsEngine statsEngine, int size) {
        ArrayList<MetricData> data = new ArrayList<>();
        for (float i = 0; i < size; i++) {
            String metric = "Metric " + i;
            Stats stats = StatsTest.createStats(statsEngine);
            stats.recordDataPoint(i / (System.currentTimeMillis() % 10000));
            data.add(MetricData.create(MetricName.create(metric), stats));
        }
        return data;
    }

    @Test(timeout = 30000)
    public void testCombinedSSLConfig() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTPS_PORT);
        map.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        map.put("use_private_ssl", true);
        map.put("ca_bundle_path", "src/test/resources/server.cer");
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        createServiceManager(map);

        doTestLaunchHttps();
    }


    @Test(timeout = 30000, expected = SSLHandshakeException.class)
    public void testUsePrivateSSLConfig() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTPS_PORT);
        map.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        map.put("use_private_ssl", true);
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        createServiceManager(map);

        doTestLaunchHttps();
    }

    @Test(expected = LicenseException.class)
    public void testBadLicense() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTPS_PORT);
        map.put("license_key", "xxxxxxxxxxxxxxxxxxxxxxxxxxx");
        map.put("ca_bundle_path", "src/test/resources/server.cer");
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        createServiceManager(map);

        doTestBadLicense();
    }

    @Test(expected = LicenseException.class)
    public void testBadLicenseWithPut() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTPS_PORT);
        map.put("license_key", "xxxxxxxxxxxxxxxxxxxxxxxxxxx");
        map.put("ca_bundle_path", "src/test/resources/server.cer");
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        map.put(AgentConfigImpl.PUT_FOR_DATA_SEND_PROPERTY, true);
        createServiceManager(map);

        doTestBadLicense();
    }

    @Test()
    public void testBadLicenseWithPreventReconnectFlagStopsReconnect(){
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTPS_PORT);
        map.put("license_key", "xxxxxxxxxxxxxxxxxxxxxxxxxxx");
        map.put("ca_bundle_path", "src/test/resources/server.cer");
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        createServiceManager(map);

        RPMConnectionServiceImpl mockConnectionService = mock(RPMConnectionServiceImpl.class);
        when(mockConnectionService.shouldPreventNewConnectionTask()).thenReturn(true);
        ((MockServiceManager) ServiceFactory.getServiceManager()).setRPMConnectionService(mockConnectionService);

        try {
            doTestBadLicense();
            fail();
        } catch(Exception e){
            assertTrue(e instanceof LicenseException);
            verify(mockConnectionService, times(0)).connectImmediate(any());
        }
    }

    @Test
    public void testBadLicenseWithoutPreventReconnectFlagAllowsReconnect(){
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", MOCK_COLLECTOR_HTTPS_PORT);
        map.put("license_key", "xxxxxxxxxxxxxxxxxxxxxxxxxxx");
        map.put("ca_bundle_path", "src/test/resources/server.cer");
        map.put(AgentConfigImpl.APP_NAME, "MyApplication");
        createServiceManager(map);

        RPMConnectionServiceImpl mockConnectionService = mock(RPMConnectionServiceImpl.class);
        when(mockConnectionService.shouldPreventNewConnectionTask()).thenReturn(false);
        ((MockServiceManager) ServiceFactory.getServiceManager()).setRPMConnectionService(mockConnectionService);

        try {
            doTestBadLicense();
            fail();
        } catch(Exception e){
            assertTrue(e instanceof LicenseException);
            verify(mockConnectionService, times(1)).connectImmediate(any());
        }

    }

    private void doTestBadLicense() throws Exception {
        List<String> appNames = singletonList("");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        svc.launch();
    }

    @Test(expected = ForceRestartException.class)
    public void forceRestartException() throws Exception {
        Map<String, Object> config = createStagingMap(false, false);
        createServiceManager(config);
        doForceRestartException();
    }

    @Test(expected = ForceRestartException.class)
    public void forceRestartExceptionWithPut() throws Exception {
        Map<String, Object> config = createStagingMap(false, false, true);
        createServiceManager(config);
        doForceRestartException();
    }

    private void doForceRestartException() throws Exception {
        MockDataSenderFactory dataSenderFactory = new MockDataSenderFactory();
        DataSenderFactory.setDataSenderFactory(dataSenderFactory);
        Transaction.clearTransaction();

        List<String> appNames = singletonList("MyApplication");
        RPMService svc = new RPMService(appNames, null, null, Collections.<AgentConnectionEstablishedListener>emptyList());
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "test", "()V");
        Tracer rootTracer = new BasicRequestRootTracer(Transaction.getTransaction(), sig, this, null, null,
                new SimpleMetricNameFormat("/test"));
        AgentConfig iAgentConfig = mock(AgentConfig.class);

        TransactionData data = new TransactionDataTestBuilder("unittest", iAgentConfig, rootTracer)
                .setRequestUri("/unittest")
                .build();

        List<TransactionTrace> traces = singletonList(TransactionTrace.getTransactionTrace(data));

        MockDataSender dataSender = dataSenderFactory.getLastDataSender();
        dataSender.setConnected(false);
        dataSender.setException(new ForceRestartException(""));
        CountDownLatch latch = new CountDownLatch(1);
        dataSender.setLatch(latch);
        try {
            svc.sendTransactionTraceData(traces);
        } finally {
            latch.await(10, TimeUnit.SECONDS);
            assertTrue(dataSender.isConnected());
        }
    }

    private class LargeStackThrowableError extends ThrowableError {

        private final int stackFrameCount;

        LargeStackThrowableError(String appName, String frontendMetricName, Throwable error,
                long timestamp, Map<String, Map<String, String>> prefixedParams, Map<String, Object> userParams,
                Map<String, Object> agentParams, Map<String, String> errorParams, Map<String, Object> intrinsics,
                int stackFrameCount) {

            super(mock(ErrorCollectorConfig.class), mock(ErrorMessageReplacer.class), appName, frontendMetricName, "", error, timestamp, prefixedParams,
                    userParams, agentParams, errorParams, intrinsics, null, false, null);

            this.stackFrameCount = stackFrameCount;
        }

        @Override
        public Collection<String> stackTrace() {
            List<String> largeStackTrace = new ArrayList<>();
            for (int i = 0; i < stackFrameCount; i++) {
                largeStackTrace.add("StackFramePaddingPlaceholderDataGoesHere" + i);
            }
            return largeStackTrace;
        }
    }

}
