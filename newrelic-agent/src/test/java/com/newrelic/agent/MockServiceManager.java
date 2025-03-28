/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.browser.BrowserService;
import com.newrelic.agent.cache.CacheService;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.commands.CommandParser;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.JmxConfig;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.jfr.JfrService;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.language.SourceLanguageService;
import com.newrelic.agent.normalization.NormalizationService;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.reinstrument.RemoteInstrumentationService;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.agent.service.analytics.InsightsServiceImpl;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.service.logging.LogSenderService;
import com.newrelic.agent.service.logging.LogSenderServiceImpl;
import com.newrelic.agent.service.module.ClassToJarPathSubmitterImpl;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.utilization.UtilizationService;
import com.newrelic.api.agent.MetricAggregator;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class MockServiceManager extends AbstractService implements ServiceManager {

    private volatile RPMServiceManager rpmServiceManager = Mockito.mock(RPMServiceManager.class);
    private volatile CoreService coreService;
    private volatile ConfigService configService;
    private volatile ThreadService threadService;
    private volatile EnvironmentService environmentService;
    private volatile TransactionTraceService transactionTraceService;
    private volatile TransactionService transactionService;
    private volatile ProfilerService profilerService;
    private volatile RPMConnectionService rpmConnectionService;
    private volatile StatsService statsService;
    private volatile HarvestService harvestService;
    private volatile SqlTraceService sqlTraceService;
    private volatile CacheService cacheService;
    private volatile SamplerService samplerService;
    private volatile DatabaseService dbService;
    private final JarCollectorService jarCollectorService;
    private volatile TransactionEventsService transactionEventsService;
    private volatile NormalizationService normalizationService;
    private volatile ExtensionService extensionService;
    private volatile TracerService tracerService;
    private volatile CommandParser commandParser;
    private volatile RemoteInstrumentationService remoteInstrumentationService;
    private volatile ClassTransformerService classTransformerService;
    private volatile AttributesService attributesService;
    private volatile UtilizationService utilizationService;
    private volatile JmxService jmxService;
    private volatile JfrService jfrService;
    private volatile AsyncTransactionService asyncTxService;
    private volatile CircuitBreakerService circuitBreakerService;
    private volatile DistributedTraceService distributedTraceService;
    private volatile SpanEventsService spanEventsService;
    private volatile SourceLanguageService sourceLanguageService;
    private volatile InsightsService insights;
    private volatile LogSenderService logSenderService;
    private volatile ExpirationService expirationService;
    private volatile  BrowserService browserService;

    public MockServiceManager() {
        this(createConfigService());
    }

    private static ConfigService createConfigService() {
        final Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.APP_NAME, "Unit Test");
        settings.put(AgentConfigImpl.HOST, "no-collector.example.com");
        return ConfigServiceFactory.createConfigServiceUsingSettings(settings);
    }

    public MockServiceManager(ConfigService configService) {
        super(ServiceManager.class.getSimpleName());
        if (configService == null) {
            this.configService = new MockConfigService(AgentConfigImpl.createAgentConfig(new HashMap<String, Object>()));
        } else {
            this.configService = configService;
        }
        ServiceFactory.setServiceManager(this);

        coreService = Mockito.mock(CoreService.class);
        Mockito.when(coreService.isEnabled()).thenReturn(true);

        threadService = new ThreadService();
        samplerService = Mockito.mock(SamplerService.class);
        environmentService = new EnvironmentServiceImpl();
        transactionTraceService = new TransactionTraceService();
        transactionService = new TransactionService();
        asyncTxService = new AsyncTransactionService();
        profilerService = new ProfilerService();
        rpmConnectionService = Mockito.mock(RPMConnectionService.class);
        statsService = Mockito.mock(StatsService.class);
        MetricAggregator metricAggregator = Mockito.mock(MetricAggregator.class);
        Mockito.when(statsService.getMetricAggregator()).thenReturn(metricAggregator);
        harvestService = Mockito.mock(HarvestService.class);
        sqlTraceService = Mockito.mock(SqlTraceService.class);
        dbService = new DatabaseService();
        extensionService = new ExtensionService(configService, ExtensionsLoadedListener.NOOP);
        jarCollectorService = Mockito.mock(JarCollectorService.class);
        Mockito.when(jarCollectorService.getClassToJarPathSubmitter()).thenReturn(ClassToJarPathSubmitterImpl.NO_OP_INSTANCE);
        sourceLanguageService = new SourceLanguageService();
        expirationService = new ExpirationService();
        distributedTraceService = Mockito.mock(DistributedTraceService.class);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        transactionEventsService = new TransactionEventsService(transactionDataToDistributedTraceIntrinsics);
        normalizationService = Mockito.mock(NormalizationService.class);
        Mockito.when(normalizationService.getMetricNormalizer(Mockito.anyString())).thenReturn(new MockNormalizer());
        Mockito.when(normalizationService.getTransactionNormalizer(Mockito.anyString())).thenReturn(new MockNormalizer());
        Mockito.when(normalizationService.getUrlNormalizer(Mockito.anyString())).thenReturn(new MockNormalizer());
        tracerService = new TracerService();
        commandParser = new CommandParser();
        remoteInstrumentationService = Mockito.mock(RemoteInstrumentationService.class);
        classTransformerService = Mockito.mock(ClassTransformerService.class);
        JmxConfig jmxConfig = this.configService.getDefaultAgentConfig().getJmxConfig();
        jmxService = new JmxService(jmxConfig);
        circuitBreakerService = new CircuitBreakerService();
        spanEventsService = Mockito.mock(SpanEventsService.class);
        insights = Mockito.mock(InsightsServiceImpl.class);
        logSenderService = Mockito.mock(LogSenderServiceImpl.class);
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public void addService(Service service) {
    }

    @Override
    public CommandParser getCommandParser() {
        return commandParser;
    }

    public void setCommandParser(CommandParser commandParser) {
        this.commandParser = commandParser;
    }

    @Override
    public ExtensionService getExtensionService() {
        return extensionService;
    }

    public void setExtensionService(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    @Override
    public JmxService getJmxService() {
        return jmxService;
    }

    public void setJmxService(JmxService service) {
        this.jmxService = service;
    }

    @Override
    public ProfilerService getProfilerService() {
        return profilerService;
    }

    public void setProfilerService(ProfilerService profilerService) {
        this.profilerService = profilerService;
    }

    @Override
    public RPMServiceManager getRPMServiceManager() {
        return rpmServiceManager;
    }

    public void setHarvestService(HarvestService harvestService) {
        this.harvestService = harvestService;
    }

    @Override
    public HarvestService getHarvestService() {
        return harvestService;
    }

    public void setRPMServiceManager(RPMServiceManager rpmServiceManager) {
        this.rpmServiceManager = rpmServiceManager;
    }

    @Override
    public SamplerService getSamplerService() {
        return samplerService;
    }

    public void setSamplerService(SamplerService samplerService) {
        this.samplerService = samplerService;
    }

    @Override
    public Service getService(String name) {
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> getServicesConfiguration() {
        Map<String, Map<String, Object>> config = new HashMap<>();

        Map<String, Object> serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("TransactionService", serviceInfo);

        serviceInfo = new HashMap<>();
        config.put("TransactionTraceService", serviceInfo);
        serviceInfo.put("enabled", true);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("ProfilerService", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("TracerService", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("CommandParser", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("JmxService", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("ThreadService", serviceInfo);

        return config;
    }

    @Override
    public ThreadService getThreadService() {
        return threadService;
    }

    public void setThreadService(ThreadService threadService) {
        this.threadService = threadService;
    }

    @Override
    public TracerService getTracerService() {
        return tracerService;
    }

    public void setTracerService(TracerService tracerService) {
        this.tracerService = tracerService;
    }

    @Override
    public TransactionService getTransactionService() {
        return transactionService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public TransactionTraceService getTransactionTraceService() {
        return transactionTraceService;
    }

    public void setTransactionTraceService(TransactionTraceService transactionTraceService) {
        this.transactionTraceService = transactionTraceService;
    }

    @Override
    public CoreService getCoreService() {
        return coreService;
    }

    public void setCoreService(CoreService coreService) {
        this.coreService = coreService;
    }

    @Override
    protected void doStart() throws Exception {
        if (circuitBreakerService != null) {
            circuitBreakerService.start();
        }
        if (remoteInstrumentationService != null) {
            remoteInstrumentationService.start();
        }
        if (configService != null) {
            configService.start();
        }
        if (rpmConnectionService != null) {
            rpmConnectionService.start();
        }
        if (jarCollectorService != null) {
            jarCollectorService.start();
        }
        if (harvestService != null) {
            harvestService.start();
        }
        if (samplerService != null) {
            samplerService.start();
        }
        if (sqlTraceService != null) {
            sqlTraceService.start();
        }
        if (normalizationService != null) {
            normalizationService.start();
        }
        if (extensionService != null) {
            extensionService.start();
        }
        if (transactionService != null) {
            transactionService.start();
        }
        if (tracerService != null) {
            tracerService.start();
        }
        if (samplerService != null) {
            samplerService.start();
        }
        if (threadService != null) {
            threadService.start();
        }
        if (transactionTraceService != null) {
            transactionTraceService.start();
        }
        if (transactionEventsService != null) {
            transactionEventsService.start();
        }
        if (profilerService != null) {
            profilerService.start();
        }
        if (commandParser != null) {
            commandParser.start();
        }
        if (jmxService != null) {
            jmxService.start();
        }
        if (rpmServiceManager != null) {
            rpmServiceManager.start();
        }
        if (environmentService != null) {
            environmentService.start();
        }
        if (statsService != null) {
            statsService.start();
        }
        if (cacheService != null) {
            cacheService.start();
        }
        if (sourceLanguageService != null) {
            sourceLanguageService.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (circuitBreakerService != null) {
            circuitBreakerService.stop();
        }
        if (remoteInstrumentationService != null) {
            remoteInstrumentationService.stop();
        }
        if (configService != null) {
            configService.stop();
        }
        if (rpmConnectionService != null) {
            rpmConnectionService.stop();
        }
        if (jarCollectorService != null) {
            jarCollectorService.stop();
        }
        if (harvestService != null) {
            harvestService.stop();
        }
        if (samplerService != null) {
            samplerService.stop();
        }
        if (sqlTraceService != null) {
            sqlTraceService.stop();
        }
        if (normalizationService != null) {
            normalizationService.stop();
        }
        if (extensionService != null) {
            extensionService.stop();
        }
        if (transactionService != null) {
            transactionService.stop();
        }
        if (tracerService != null) {
            tracerService.stop();
        }
        if (threadService != null) {
            threadService.stop();
        }
        if (transactionTraceService != null) {
            transactionTraceService.stop();
        }
        if (transactionEventsService != null) {
            transactionEventsService.stop();
        }
        if (profilerService != null) {
            profilerService.stop();
        }
        if (commandParser != null) {
            commandParser.stop();
        }
        if (jmxService != null) {
            jmxService.stop();
        }
        if (rpmServiceManager != null) {
            rpmServiceManager.stop();
        }
        if (environmentService != null) {
            environmentService.stop();
        }
        if (statsService != null) {
            statsService.stop();
        }
        if (cacheService != null) {
            cacheService.stop();
        }
        if (sourceLanguageService != null) {
            sourceLanguageService.stop();
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public ConfigService getConfigService() {
        return configService;
    }

    public MockServiceManager setConfigService(ConfigService configService) {
        this.configService = configService;
        return this;
    }

    @Override
    public RPMConnectionService getRPMConnectionService() {
        return rpmConnectionService;
    }

    public void setRPMConnectionService(RPMConnectionService rpmConnectionService) {
        this.rpmConnectionService = rpmConnectionService;
    }

    @Override
    public EnvironmentService getEnvironmentService() {
        return environmentService;
    }

    public void setEnvironmentService(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public ClassTransformerService getClassTransformerService() {
        return classTransformerService;
    }

    public void setClassTransformerService(ClassTransformerService service) {
        classTransformerService = service;
    }

    @Override
    public StatsService getStatsService() {
        return statsService;
    }

    public void setStatsService(StatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public SqlTraceService getSqlTraceService() {
        return sqlTraceService;
    }

    @Override
    public BrowserService getBrowserService() {
        return this.browserService;
    }

    public void setBrowserService(BrowserService browserService) {
        this.browserService = browserService;
    }

    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public CacheService getCacheService() {
        return cacheService;
    }

    public void setSqlTraceService(SqlTraceService sqlTraceService) {
        this.sqlTraceService = sqlTraceService;
    }

    @Override
    public DatabaseService getDatabaseService() {
        return dbService;
    }

    public void setDatabaseService(DatabaseService dbService) {
        this.dbService = dbService;
    }

    @Override
    public NormalizationService getNormalizationService() {
        if (normalizationService == null) {
            normalizationService = new NormalizationServiceImpl();
        }
        return normalizationService;
    }

    public void setNormalizationService(NormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    @Override
    public JarCollectorService getJarCollectorService() {
        return jarCollectorService;
    }

    @Override
    public JfrService getJfrService() {
        return jfrService;
    }

    @Override
    public TransactionEventsService getTransactionEventsService() {
        return transactionEventsService;
    }

    public void setTransactionEventsService(TransactionEventsService analyticsEventsService) {
        this.transactionEventsService = analyticsEventsService;
    }

    @Override
    public RemoteInstrumentationService getRemoteInstrumentationService() {
        return remoteInstrumentationService;
    }

    public void setReinstrumentService(RemoteInstrumentationService pReinstrumentService) {
        remoteInstrumentationService = pReinstrumentService;
    }

    @Override
    public AttributesService getAttributesService() {
        return attributesService;
    }

    public void setAttributesService(AttributesService service) {
        attributesService = service;
    }

    @Override
    public InsightsService getInsights() {
        return insights;
    }

    public void setInsights(InsightsService service) {
        insights = service;
    }

    @Override
    public LogSenderService getLogSenderService() {
        return logSenderService;
    }

    public void setLogSenderService(LogSenderService service) {
        logSenderService = service;
    }

    @Override
    public CircuitBreakerService getCircuitBreakerService() {
        return circuitBreakerService;
    }

    public void setCircuitBreakerService(CircuitBreakerService newCB) {
        circuitBreakerService = newCB;
    }

    @Override
    public AsyncTransactionService getAsyncTxService() {
        return asyncTxService;
    }

    public void setAsyncTransactionService(AsyncTransactionService pAsyncTxService) {
        asyncTxService = pAsyncTxService;
    }

    @Override
    public UtilizationService getUtilizationService() {
        return utilizationService;
    }

    @Override
    public DistributedTraceService getDistributedTraceService() {
        return distributedTraceService;
    }

    @Override
    public SpanEventsService getSpanEventsService() {
        return spanEventsService;
    }

    public void setUtilizationService(UtilizationService service) {
        utilizationService = service;
    }

    public void setDistributedTraceService(DistributedTraceService distributedTraceService) {
        this.distributedTraceService = distributedTraceService;
    }

    public void setSpansEventService(SpanEventsService spanEventService) {
        this.spanEventsService = spanEventService;
    }

    @Override
    public SourceLanguageService getSourceLanguageService() {
        return sourceLanguageService;
    }

    @Override
    public ExpirationService getExpirationService() {
        return expirationService;
    }

    @Override
    public void refreshDataForCRaCRestore() {}

    public void setSourceLanguageService(SourceLanguageService sourceLanguageService) {
        this.sourceLanguageService = sourceLanguageService;
    }

    public void setExpirationService(ExpirationService expirationService) {
        this.expirationService = expirationService;
    }

}
