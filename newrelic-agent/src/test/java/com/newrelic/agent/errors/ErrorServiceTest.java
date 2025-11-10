/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.google.common.collect.Iterables;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.ErrorCollectorConfigImpl;
import com.newrelic.agent.config.StripExceptionConfigImpl;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.EventTestHelper;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import org.json.simple.JSONArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ErrorServiceTest {
    private static final String APP_NAME = "Unit Test";

    @Before
    public void setup() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);
        Transaction.clearTransaction();
    }

    @After
    public void teardown() throws Exception {
        ServiceFactory.getServiceManager().stop();
        ServiceFactory.setServiceManager(null);
        Transaction.clearTransaction();
    }

    private void setConfigAttributes(Map<String, Object> config, boolean captureAtts, boolean includeRequest) {
        Map<String, Object> errors = new HashMap<>();
        config.put(AgentConfigImpl.ERROR_COLLECTOR, errors);
        Map<String, Object> atts = new HashMap<>();
        errors.put(AgentConfigImpl.ATTRIBUTES, atts);
        atts.put("enabled", captureAtts);
        if (includeRequest) {
            atts.put("include", "request.parameters.*");
        }
    }

    private Map<String, Object> createConfig(String ignoreErrors) {
        return createConfig(ignoreErrors, null, null, null);
    }

    private Map<String, Object> createConfig(String ignoreErrors, Boolean highSecurity, Boolean stripExceptionEnabled,
            String allowedExceptionClasses) {

        Map<String, Object> map = new HashMap<>();
        map.put("host", "staging-collector.newrelic.com");
        map.put("port", 80);
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        Map<String, Object> errorCollectorMap = new HashMap<>();
        errorCollectorMap.put("ignore_errors", ignoreErrors);
        errorCollectorMap.put("enabled", true);
        errorCollectorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
        if (ignoreErrors != null) {
            map.put("error_collector", errorCollectorMap);
        }

        if (highSecurity != null) {
            map.put(AgentConfigImpl.HIGH_SECURITY, highSecurity);
        }

        Map<String, Object> stripExceptionMap = new HashMap<>();
        if (stripExceptionEnabled != null) {
            stripExceptionMap.put(StripExceptionConfigImpl.ENABLED, stripExceptionEnabled);
        }
        if (allowedExceptionClasses != null) {
            stripExceptionMap.put(StripExceptionConfigImpl.ALLOWED_CLASSES, allowedExceptionClasses);
        }
        if (!stripExceptionMap.isEmpty()) {
            map.put(AgentConfigImpl.STRIP_EXCEPTION_MESSAGES, stripExceptionMap);
        }

        return map;
    }

    @Test
    public void isIgnoredErrorNone() throws Exception {
        TransactionData data = new TransactionData(Transaction.getTransaction(), 0);
        Assert.assertFalse(data.hasReportableErrorThatIsNotIgnored());
    }

    @Test
    public void isIgnoredErrorMatch() throws Exception {
        Throwable error = new ArrayIndexOutOfBoundsException();
        Transaction.getTransaction().setThrowable(error, TransactionErrorPriority.API, false);
        TransactionData data = new TransactionData(Transaction.getTransaction(), 0);
        Assert.assertTrue(data.hasReportableErrorThatIsNotIgnored());
    }

    @Test
    public void isIgnoredErrorNoMatch() throws Exception {
        Throwable error = new Exception();
        Transaction.getTransaction().setThrowable(error, TransactionErrorPriority.API, false);
        TransactionData data = new TransactionData(Transaction.getTransaction(), 0);
        Assert.assertFalse(data.hasReportableErrorThatIsNotIgnored());
    }

    @Test
    public void isIgnoredErrorNestedNoMatch() throws Exception {
        Map<String, Object> config = createConfig("java.lang.IllegalArgumentException");
        EventTestHelper.createServiceManager(config);

        Throwable error = new Throwable(new Exception(new ArrayIndexOutOfBoundsException()));
        Transaction.getTransaction().setThrowable(error, TransactionErrorPriority.API, false);
        TransactionData data = new TransactionData(Transaction.getTransaction(), 0);
        Assert.assertTrue(data.hasReportableErrorThatIsNotIgnored());
    }

    @Test
    public void isIgnoredErrorNestedMatchTop() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Throwable");
        EventTestHelper.createServiceManager(config);

        Throwable error = new Throwable(new Exception(new ArrayIndexOutOfBoundsException()));
        Transaction.getTransaction().setThrowable(error, TransactionErrorPriority.API, false);
        TransactionData data = new TransactionData(Transaction.getTransaction(), 0);
        Assert.assertFalse(data.hasReportableErrorThatIsNotIgnored());
    }

    @Test
    public void isIgnoredErrorNestedMatchMiddle() throws Exception {
        Throwable error = new Throwable(new Exception(new ArrayIndexOutOfBoundsException()));
        Transaction.getTransaction().setThrowable(error, TransactionErrorPriority.API, false);
        TransactionData data = new TransactionData(Transaction.getTransaction(), 0);
        Assert.assertFalse(data.hasReportableErrorThatIsNotIgnored());
    }

    @Test
    public void isIgnoredErrorNestedMatchBottom() throws Exception {
        Map<String, Object> config = createConfig("java.lang.ArrayIndexOutOfBoundsException");
        EventTestHelper.createServiceManager(config);

        Throwable error = new Throwable(new Exception(new ArrayIndexOutOfBoundsException()));
        Transaction.getTransaction().setThrowable(error, TransactionErrorPriority.API, false);
        TransactionData data = new TransactionData(Transaction.getTransaction(), 0);
        Assert.assertFalse(data.hasReportableErrorThatIsNotIgnored());
    }

    @Test
    public void configureHarvestable() {
        String appName = ServiceFactory.getRPMService().getApplicationName();
        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        errorService.setHarvestable(new ErrorHarvestableImpl(errorService, appName));
        errorService.harvestable.configure(60, 10);
        Assert.assertEquals(10, errorService.getMaxSamplesStored());
    }

    @Test
    public void reportError() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        List<TracedError> errors = new ArrayList<>(ErrorServiceImpl.ERROR_LIMIT_PER_REPORTING_PERIOD);
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        for (int i = 0; i < ErrorServiceImpl.ERROR_LIMIT_PER_REPORTING_PERIOD; i++) {
            TracedError error = HttpTracedError
                    .builder(errorCollectorConfig, null, "dude", System.currentTimeMillis())
                    .statusCodeAndMessage(403, null)
                    .requestUri("/dude")
                    .build();
            errors.add(error);
            errorService.reportError(error);
        }
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, null, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();
        errorService.reportError(error);
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals(errors.size(), actualErrors.size());
        Assert.assertTrue(actualErrors.containsAll(errors));

        error = HttpTracedError
                .builder(errorCollectorConfig, null, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();
        errorService.reportError(error);
        actualErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals(1, actualErrors.size());
        Assert.assertEquals(error, actualErrors.get(0));

        Assert.assertEquals(ErrorServiceImpl.ERROR_LIMIT_PER_REPORTING_PERIOD + 2,
                errorService.errorCountThisHarvest.get());
    }

    @Test
    public void enabled() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> errorMap = new HashMap<>();
        configMap.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, true);
        errorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);

        errorService.refreshErrorCollectorConfig(AgentConfigFactory.createAgentConfig(configMap, null, null));

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, null, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();
        errorService.reportError(error);
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals(1, actualErrors.size());
        Assert.assertEquals(1, errorService.errorCountThisHarvest.get());
    }

    @Test
    public void notEnabled() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> errorMap = new HashMap<>();
        configMap.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, false);

        errorService.refreshErrorCollectorConfig(AgentConfigFactory.createAgentConfig(configMap, null, null));

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, APP_NAME, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();

        errorService.reportError(error);
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals(0, actualErrors.size());
        Assert.assertEquals(1, errorService.errorCountThisHarvest.get());
    }

    @Test
    public void ignored() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getErrorCollectorConfig(APP_NAME);

        TracedError error = ThrowableError
                .builder(errorCollectorConfig, APP_NAME, "dude", new Exception(), System.currentTimeMillis())
                .build();
        errorService.reportError(error);
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals(0, actualErrors.size());

        Assert.assertEquals(0, errorService.errorCountThisHarvest.get());

        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> errorMap = new HashMap<>();
        configMap.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, true);
        errorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
        errorMap.put(ErrorCollectorConfigImpl.IGNORE_ERRORS, "java.lang.Exception");
        errorService.refreshErrorCollectorConfig(AgentConfigFactory.createAgentConfig(configMap, null, null));

        errorService.reportError(error);
        actualErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals(0, actualErrors.size());
        Assert.assertEquals(0, errorService.errorCountThisHarvest.get());
    }

    @Test
    public void notCollectErrors() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        Map<String, Object> localSettings = new HashMap<>();
        Map<String, Object> errorMap = new HashMap<>();
        localSettings.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, true);
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, false);

        errorService.refreshErrorCollectorConfig(AgentConfigFactory.createAgentConfig(localSettings, serverSettings, null));

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, APP_NAME, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();
        errorService.reportError(error);
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors();
        Assert.assertEquals(0, actualErrors.size());
        Assert.assertEquals(1, errorService.errorCountThisHarvest.get());
    }

    @Test
    public void harvest() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMService();
        rpmService.setIsConnected(false);
        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, APP_NAME, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();
        errorService.reportError(error);

        StatsService spy = spy(new StatsServiceImpl());
        StatsEngine statsEngine = spy.getStatsEngineForHarvest(APP_NAME);
        ((MockServiceManager) ServiceFactory.getServiceManager()).setStatsService(spy);

        List<TracedError> actualErrors = errorService.getAndClearTracedErrors(APP_NAME, statsEngine);

        Assert.assertEquals(0, actualErrors.size());
        Assert.assertEquals(1, statsEngine.getStats(MetricNames.ERRORS_ALL).getCallCount());

        rpmService.setIsConnected(true);
        actualErrors = errorService.getAndClearTracedErrors(APP_NAME, statsEngine);
        Assert.assertEquals(1, actualErrors.size());
        Assert.assertEquals(1, statsEngine.getStats(MetricNames.ERRORS_ALL).getCallCount());
    }

    @Test
    public void errorCountMetrics() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMService();
        rpmService.setIsConnected(true);

        TransactionService txService = ServiceFactory.getTransactionService();
        Throwable error = new ArrayIndexOutOfBoundsException();

        TransactionData data = createTransactionData(true, 0, error, false);
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);
        Assert.assertEquals(1,
                transactionStats.getUnscopedStats().getStats("Errors/WebTransaction/Uri/dude").getCallCount());
        Assert.assertEquals(1,
                transactionStats.getUnscopedStats().getStats(MetricNames.WEB_TRANSACTION_ERRORS_ALL).getCallCount());

        data = createTransactionData(true, 0, null, false);
        transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);
        Assert.assertEquals(0,
                transactionStats.getUnscopedStats().getStats("Errors/WebTransaction/Uri/dude").getCallCount());
        Assert.assertEquals(0,
                transactionStats.getUnscopedStats().getStats(MetricNames.WEB_TRANSACTION_ERRORS_ALL).getCallCount());

        data = createTransactionData(false, 0, error, false);
        transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);
        Assert.assertEquals(1,
                transactionStats.getUnscopedStats().getStats("Errors/OtherTransaction/Custom/dude").getCallCount());
        Assert.assertEquals(1,
                transactionStats.getUnscopedStats().getStats(MetricNames.OTHER_TRANSACTION_ERRORS_ALL).getCallCount());

        data = createTransactionData(false, 0, null, false);
        transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);
        Assert.assertEquals(0,
                transactionStats.getUnscopedStats().getStats("Errors/OtherTransaction/Custom/dude").getCallCount());
        Assert.assertEquals(0,
                transactionStats.getUnscopedStats().getStats(MetricNames.WEB_TRANSACTION_ERRORS_ALL).getCallCount());

        StatsService spy = spy(new StatsServiceImpl());
        ((MockServiceManager) ServiceFactory.getServiceManager()).setStatsService(spy);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        StatsEngine statsEngine = spy.getStatsEngineForHarvest(APP_NAME);
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors(APP_NAME, statsEngine);

        Assert.assertEquals(2, actualErrors.size());
        Assert.assertEquals(2, statsEngine.getStats(MetricNames.ERRORS_ALL).getCallCount());

        spy = spy(new StatsServiceImpl());
        ((MockServiceManager) ServiceFactory.getServiceManager()).setStatsService(spy);
        statsEngine = spy.getStatsEngineForHarvest(APP_NAME);
        actualErrors = errorService.getAndClearTracedErrors(APP_NAME, statsEngine);
        Assert.assertEquals(0, actualErrors.size());
        Assert.assertEquals(0, statsEngine.getStats(MetricNames.ERRORS_ALL).getCallCount());

        data = createTransactionData(true, 0, error, false);
        txService.transactionFinished(data, new TransactionStats());

        spy = spy(new StatsServiceImpl());
        ((MockServiceManager) ServiceFactory.getServiceManager()).setStatsService(spy);

        statsEngine = spy.getStatsEngineForHarvest(APP_NAME);
        actualErrors = errorService.getAndClearTracedErrors(APP_NAME, statsEngine);
        Assert.assertEquals(1, actualErrors.size());
        Assert.assertEquals(1, statsEngine.getStats(MetricNames.ERRORS_ALL).getCallCount());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void attributesDisabled() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        setConfigAttributes(config, false, true);
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMService();
        rpmService.setIsConnected(true);

        TransactionService txService = ServiceFactory.getTransactionService();
        Throwable error = new ArrayIndexOutOfBoundsException();

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key2", 2L);
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key3", "value3");
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("key4", "value4");
        Map<String, Object> intrinsics = new HashMap<>();
        TransactionData data = createTransactionData(true, 0, error, false, requestParams, userParams, agentParams,
                errorParams, intrinsics);
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);

        List<TracedError> actualErrors = errorService.getAndClearTracedErrors(APP_NAME, new StatsEngineImpl());
        Assert.assertEquals(1, actualErrors.size());
        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(actualErrors.get(0));
        Assert.assertNotNull(serializedError);
        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        Assert.assertEquals(2, params.size());
        Assert.assertNotNull(params.get("stack_trace"));
        Map<String, Object> intrinsicsParam = (Map<String, Object>) params.get("intrinsics");
        Assert.assertEquals(false, intrinsicsParam.get("error.expected"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void attributesDisabledWithIntrinsics() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        setConfigAttributes(config, false, true);
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMService();
        rpmService.setIsConnected(true);

        TransactionService txService = ServiceFactory.getTransactionService();
        Throwable error = new ArrayIndexOutOfBoundsException();

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key2", 2L);
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key3", "value3");
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("key4", "value4");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key5", "value5");
        TransactionData data = createTransactionData(true, 0, error, false, requestParams, userParams, agentParams,
                errorParams, intrinsics);
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);

        List<TracedError> actualErrors = errorService.getAndClearTracedErrors(APP_NAME, new StatsEngineImpl());
        Assert.assertEquals(1, actualErrors.size());
        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(actualErrors.get(0));
        Assert.assertNotNull(serializedError);
        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        Assert.assertEquals(2, params.size());
        Assert.assertNotNull(params.get("stack_trace"));
        Assert.assertNotNull(params.get("intrinsics"));
        Map<String, Object> actual = (Map<String, Object>) params.get("intrinsics");
        Assert.assertEquals(6, actual.size());
        Assert.assertEquals("value5", actual.get("key5"));
        Assert.assertEquals(false, actual.get("error.expected"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void userParametersEnabled() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        setConfigAttributes(config, true, true);
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMService();
        rpmService.setIsConnected(true);

        TransactionService txService = ServiceFactory.getTransactionService();
        Throwable error = new ArrayIndexOutOfBoundsException();

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key2", 2L);
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key3", "value3");
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("key4", "value4");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key5", "value5");
        intrinsics.put("key6", 7.77);
        intrinsics.put("key7", 18L);
        TransactionData data = createTransactionData(true, 0, error, false, requestParams, userParams, agentParams,
                errorParams, intrinsics);
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);

        List<TracedError> actualErrors = errorService.getAndClearTracedErrors(APP_NAME, new StatsEngineImpl());
        Assert.assertEquals(1, actualErrors.size());
        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(actualErrors.get(0));
        Assert.assertNotNull(serializedError);
        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        Assert.assertEquals(4, params.size());
        Assert.assertNotNull(params.get("stack_trace"));
        Assert.assertNotNull(params.get("intrinsics"));
        Assert.assertNotNull(params.get("agentAttributes"));
        Assert.assertNotNull(params.get("userAttributes"));

        Map<String, Object> actual = (Map<String, Object>) params.get("intrinsics");
        Assert.assertEquals(8, actual.size());
        Assert.assertEquals("value5", actual.get("key5"));
        Assert.assertEquals(7.77, (Double) actual.get("key6"), .001);
        Assert.assertEquals(18L, actual.get("key7"));
        Assert.assertEquals(false, actual.get("error.expected"));

        actual = (Map<String, Object>) params.get("agentAttributes");
        Assert.assertEquals(3, actual.size());
        Assert.assertEquals("value1", actual.get("request.parameters.key1"));
        Assert.assertEquals(2L, actual.get("key2"));
        Assert.assertEquals("/dude", actual.get("request.uri"));

        actual = (Map<String, Object>) params.get("userAttributes");
        Assert.assertEquals(2, actual.size());
        Assert.assertEquals("value3", actual.get("key3"));
        Assert.assertEquals("value4", actual.get("key4"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void userParametersEnabledRequestDisabled() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        setConfigAttributes(config, true, false);
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMService();
        rpmService.setIsConnected(true);

        TransactionService txService = ServiceFactory.getTransactionService();
        Throwable error = new ArrayIndexOutOfBoundsException();

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key2", 2L);
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key3", "value3");
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("key4", "value4");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key5", "value5");
        intrinsics.put("key6", 7.77);
        intrinsics.put("key7", 18L);
        TransactionData data = createTransactionData(true, 0, error, false, requestParams, userParams, agentParams,
                errorParams, intrinsics);
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(data, transactionStats);

        List<TracedError> actualErrors = errorService.getAndClearTracedErrors(APP_NAME, new StatsEngineImpl());
        Assert.assertEquals(1, actualErrors.size());
        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(actualErrors.get(0));
        Assert.assertNotNull(serializedError);
        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        Assert.assertEquals(4, params.size());
        Assert.assertNotNull(params.get("stack_trace"));
        Assert.assertNotNull(params.get("intrinsics"));
        Assert.assertNotNull(params.get("agentAttributes"));
        Assert.assertNotNull(params.get("userAttributes"));

        Map<String, Object> actual = (Map<String, Object>) params.get("intrinsics");
        Assert.assertEquals(8, actual.size());
        Assert.assertEquals("value5", actual.get("key5"));
        Assert.assertEquals(7.77, (Double) actual.get("key6"), .001);
        Assert.assertEquals(18L, actual.get("key7"));
        Assert.assertEquals(false, actual.get("error.expected"));

        actual = (Map<String, Object>) params.get("agentAttributes");
        Assert.assertEquals(2, actual.size());
        Assert.assertEquals(2L, actual.get("key2"));
        Assert.assertEquals("/dude", actual.get("request.uri"));

        actual = (Map<String, Object>) params.get("userAttributes");
        Assert.assertEquals(2, actual.size());
        Assert.assertEquals("value3", actual.get("key3"));
        Assert.assertEquals("value4", actual.get("key4"));
    }

    @Test
    public void dontReport404() {
        // 404 should not be reported by default.
        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        errorService.reportHTTPError("My Error Message", 404, "http://www.doesnotmatter.com");
        List<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
        Assert.assertTrue(tracedErrors.isEmpty());
    }

    @Test
    public void doReportFailureError() {
        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        errorService.reportHTTPError("I am a teapot", 418, "http://www.doesnotmatter.com");
        List<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
        Assert.assertFalse(tracedErrors.isEmpty());
    }

    @Test
    public void reportException() {
        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        Throwable error = new ArrayIndexOutOfBoundsException();
        errorService.reportException(error, Collections.<String, Object>emptyMap(), false);

        List<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
        errorService.getAndClearTracedErrors();
        Assert.assertFalse(tracedErrors.isEmpty());

        TracedError reportedError = tracedErrors.get(0);
        Assert.assertTrue(reportedError instanceof ThrowableError);
        ThrowableError throwableError = (ThrowableError) reportedError;
        Assert.assertEquals(throwableError.getThrowable(), error);
    }

    @Test
    public void dontReportException() throws Exception {
        Map<String, Object> config = createConfig("java.lang.RuntimeException");
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        Throwable error = new RuntimeException();
        errorService.reportException(error, Collections.<String, Object>emptyMap(), false);

        List<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
        errorService.getAndClearTracedErrors();
        Assert.assertTrue(tracedErrors.isEmpty());
    }

    /**
     * This test verifies all possible combinations of high security + strip exceptions + exceptions not to strip
     * This is the pre-expected-errors feature ("legacy") test that does not specify an error collector config.
     */
    @Test
    public void stripExceptionCombinations() throws Exception {
        stripExceptionCombinationsSharedCode(new NoOpConfigEnhancer());
    }

    /**
     * This test verifies all possible combinations of high security + strip exceptions + exceptions not to strip
     * This test is new for the "expected errors" feature. It specifies an error collector configuration with
     * non-matching expected errors and ignored classes.
     */
    @Test
    public void stripExceptionCombinationsWithNonMatchingIgnoredAndExpectedErrors() throws Exception {
        stripExceptionCombinationsSharedCode(new NonMatchingConfigEnhancer());
    }

    /**
     * This test verifies all possible combinations of high security + strip exceptions + exceptions not to strip
     * This test is new for the "expected errors" feature. It specifies an error collector configuration with
     * expected errors and ignored classes that match the class used in the test.
     */
    @Test
    public void stripExceptionCombinationsWithMatchingIgnoredAndExpectedErrors() throws Exception {
        stripExceptionCombinationsSharedCode(new MatchingConfigEnhancer());
    }

    // This is the body of all three "strip exception" tests. It iterates several configuration modes,
    // calling a method to run a test case for each. The config enhancer is passed through to the test case.
    @SuppressWarnings("unchecked")
    private void stripExceptionCombinationsSharedCode(ConfigEnhancer enhanceConfig) throws Exception {
        String exceptionMessage = "Exception Message";
        ArrayIndexOutOfBoundsException exception = new ArrayIndexOutOfBoundsException(exceptionMessage);

        Boolean[] highSecurityStates = { null, Boolean.FALSE, Boolean.TRUE }; // Unset, disabled, enabled
        for (Boolean highSecurityState : highSecurityStates) {
            Boolean[] stripExceptionStates = { null, Boolean.FALSE, Boolean.TRUE }; // Unset, disabled, enabled
            for (Boolean stripExceptionState : stripExceptionStates) {
                // No exceptions to allow unstripped (null)
                setupAndVerifyStripExceptionMessage(enhanceConfig, highSecurityState, stripExceptionState,
                        null, true, exception);

                // No exceptions to allow unstripped (empty string)
                setupAndVerifyStripExceptionMessage(enhanceConfig, highSecurityState, stripExceptionState,
                        "", true, exception);

                // Exception should not be stripped (1 item)
                setupAndVerifyStripExceptionMessage(enhanceConfig, highSecurityState, stripExceptionState,
                        "java.lang.ArrayIndexOutOfBoundsException", false, exception);

                // Exception should be stripped (1 item)
                setupAndVerifyStripExceptionMessage(enhanceConfig, highSecurityState, stripExceptionState,
                        "java.lang.IllegalStateException", true, exception);

                // Exception should be stripped (>1 item)
                setupAndVerifyStripExceptionMessage(enhanceConfig, highSecurityState, stripExceptionState,
                        "java.lang.IllegalStateException,java.lang.NullPointerException", true, exception);

                // Exception should not be stripped (>1 item)
                setupAndVerifyStripExceptionMessage(enhanceConfig, highSecurityState, stripExceptionState,
                        "java.lang.NullPointerException,java.lang.ArrayIndexOutOfBoundsException", false, exception);
            }
        }
    }

    @Test
    public void expectedThrowable() {
        ErrorService service = ServiceFactory.getRPMService().getErrorService();
        Throwable timeoutThrowable = new Throwable("Session Timeout");

        Map<String, String> atts = new HashMap<>();
        atts.put("user", "12345");

        service.reportException(timeoutThrowable, atts, true);
        List<TracedError> tracedErrors = service.getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("Session Timeout", tracedError.getMessage());
        Assert.assertTrue(tracedError.expected);
        Assert.assertFalse(tracedError.incrementsErrorMetric());
        Assert.assertEquals("12345", tracedError.getErrorAtts().get("user"));
    }

    @Test
    public void unexpectedThrowable() {
        ErrorService service = ServiceFactory.getRPMService().getErrorService();
        Throwable error = new Throwable("Database connection error");

        Map<String, String> atts = new HashMap<>();
        atts.put("user", "12345");

        service.reportException(error, atts, false);
        List<TracedError> tracedErrors = service.getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("Database connection error", tracedError.getMessage());
        Assert.assertFalse(tracedError.expected);
        Assert.assertTrue(tracedError.incrementsErrorMetric());
        Assert.assertEquals("12345", tracedError.getErrorAtts().get("user"));
    }

    @Test
    public void expectedString() {
        Map<String, String> atts = new HashMap<>();
        atts.put("user", "12345");

        ServiceFactory.getRPMService().getErrorService().reportError("I am an expected error", atts, true);
        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("I am an expected error", tracedError.getMessage());
        Assert.assertTrue(tracedError.expected);
        Assert.assertFalse(tracedError.incrementsErrorMetric());
        Assert.assertEquals("12345", tracedError.getErrorAtts().get("user"));
    }

    @Test
    public void unexpectedString() {
        Map<String, Object> atts = new HashMap<>();
        atts.put("user", "12345");
        atts.put("int", 12345);
        atts.put("float", 12.345);
        atts.put("bool", true);
        ServiceFactory.getRPMService().getErrorService().reportError("I am an unexpected error", atts, false);
        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("I am an unexpected error", tracedError.getMessage());
        Assert.assertFalse(tracedError.expected);
        Assert.assertTrue(tracedError.incrementsErrorMetric());
        Assert.assertEquals("12345", tracedError.getErrorAtts().get("user"));
        Assert.assertEquals(12345, tracedError.getErrorAtts().get("int"));
        Assert.assertEquals(12.345, tracedError.getErrorAtts().get("float"));
        Assert.assertEquals(true, tracedError.getErrorAtts().get("bool"));
    }

    @Test
    public void expectedStringInTransaction() {
        TransactionData transactionData = createTransactionData(200, new ReportableError("I am expected"), true);
        TransactionService txService = ServiceFactory.getTransactionService();
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(transactionData, transactionStats);

        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("I am expected", tracedError.getMessage());
        Assert.assertTrue(tracedError.expected);
        Assert.assertFalse(tracedError.incrementsErrorMetric());
    }

    @Test
    public void unexpectedStringInTransaction() {
        TransactionData transactionData = createTransactionData(200, new ReportableError("Surprising error"), false);
        TransactionService txService = ServiceFactory.getTransactionService();
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(transactionData, transactionStats);

        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("Surprising error", tracedError.getMessage());
        Assert.assertFalse(tracedError.expected);
        Assert.assertTrue(tracedError.incrementsErrorMetric());
    }

    @Test
    public void expectedThrowableInTransaction() {
        TransactionData transactionData = createTransactionData(200, new Throwable("I am expected"), true);
        TransactionService txService = ServiceFactory.getTransactionService();
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(transactionData, transactionStats);

        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("I am expected", tracedError.getMessage());
        Assert.assertTrue(tracedError.expected);
        Assert.assertFalse(tracedError.incrementsErrorMetric());
    }

    @Test
    public void unexpectedThrowableInTransaction() {
        TransactionData transactionData = createTransactionData(200, new Throwable("Surprising error"), false);
        TransactionService txService = ServiceFactory.getTransactionService();
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(transactionData, transactionStats);

        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("Surprising error", tracedError.getMessage());
        Assert.assertFalse(tracedError.expected);
        Assert.assertTrue(tracedError.incrementsErrorMetric());
    }

    @Test
    public void testErrorEventFasterHarvest() throws Exception {
        String appName = ServiceFactory.getRPMService().getApplicationName();

        ServiceManager serviceManager = spy(ServiceFactory.getServiceManager());
        ServiceFactory.setServiceManager(serviceManager);

        HarvestServiceImpl harvestService = spy(new HarvestServiceImpl());
        doReturn(harvestService).when(serviceManager).getHarvestService();
        doReturn(0L).when(harvestService).getInitialDelay();

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        errorService.addHarvestableToService();

        errorService.harvestable.configure(60, 10);
        Assert.assertEquals(10, errorService.getMaxSamplesStored());

        Map<String, Object> connectionInfo = new HashMap<>();
        Map<String, Object> eventHarvest = new HashMap<>();
        Map<String, Object> harvestLimits = new HashMap<>();
        eventHarvest.put("report_period_ms", 5000L); // 5 is the lowest allowable value
        eventHarvest.put("harvest_limits", harvestLimits);
        harvestLimits.put("error_event_data", 100L);
        connectionInfo.put("event_harvest_config", eventHarvest);

        harvestService.startHarvestables(ServiceFactory.getRPMService(), AgentConfigImpl.createAgentConfig(connectionInfo));
        Thread.sleep(500);

        TracedError error = ThrowableError
                .builder(ServiceFactory.getConfigService().getErrorCollectorConfig(appName), appName, "metricName", new Throwable(), 100)
                .requestUri("requestUri")
                .errorAttributes(Collections.singletonMap("test_attribute", "value"))
                .build();

        ServiceFactory.getRPMService().getErrorService().reportError(error);
        Thread.sleep(5050);
        checkForEvent();
        ((MockRPMService) ServiceFactory.getRPMService()).clearEvents();
        ServiceFactory.getRPMService().getErrorService().reportError(error);
        Thread.sleep(5050);
        checkForEvent();
    }

    @Test
    public void testDistributedTracingAttributes() throws Exception {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        config.put("distributed_tracing", dtConfig);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);
        EventTestHelper.createServiceManager(config);

        TransactionData transactionData = createTransactionData(200, new Throwable("Surprising error"), false);
        TransactionService txService = ServiceFactory.getTransactionService();
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(transactionData, transactionStats);

        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("Surprising error", tracedError.getMessage());
        Map<String, ?> intrinsicAtts = tracedError.getIntrinsicAtts();
        Assert.assertTrue(intrinsicAtts.containsKey("traceId"));
        Assert.assertTrue(intrinsicAtts.containsKey("guid"));
        Assert.assertTrue(intrinsicAtts.containsKey("priority"));
        Assert.assertTrue(intrinsicAtts.containsKey("sampled"));
    }

    @Test
    public void txnGuidIsPresentWithDistributedTracingDisabled() throws Exception {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", false);
        config.put("distributed_tracing", dtConfig);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);
        EventTestHelper.createServiceManager(config);

        TransactionData transactionData = createTransactionData(200, new Throwable("Surprising error"), false);
        TransactionService txService = ServiceFactory.getTransactionService();
        TransactionStats transactionStats = new TransactionStats();
        txService.transactionFinished(transactionData, transactionStats);

        List<TracedError> tracedErrors = ServiceFactory.getRPMService().getErrorService().getAndClearTracedErrors();
        TracedError tracedError = tracedErrors.get(0);
        Assert.assertEquals("Surprising error", tracedError.getMessage());
        Map<String, ?> intrinsicAtts = tracedError.getIntrinsicAtts();
        Assert.assertTrue(intrinsicAtts.containsKey("guid"));
    }

    @Test
    public void testAppNameFiltering() throws Exception {
        Map<String, Object> config = createConfig("java.lang.Exception");
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> errorMap = new HashMap<>();
        configMap.put(AgentConfigImpl.ERROR_COLLECTOR, errorMap);
        errorMap.put(ErrorCollectorConfigImpl.ENABLED, true);
        errorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);

        errorService.refreshErrorCollectorConfig(AgentConfigFactory.createAgentConfig(configMap, null, null));

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, null, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();
        errorService.reportError(error);
        errorService.harvestTracedErrors("some_other_app", ServiceFactory.getStatsService().getStatsEngineForHarvest("some_other_app"));
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors();
        // If the harvest above this worked, there should be no traced. But the harvest should not capture anything since it is from a different app name
        Assert.assertEquals(1, actualErrors.size());
        Assert.assertEquals(1, errorService.errorCountThisHarvest.get());
    }

    private void checkForEvent() {
        StatsEngine statsEngineForHarvest = ServiceFactory.getStatsService().getStatsEngineForHarvest(EventTestHelper.APP_NAME);
        Assert.assertTrue(statsEngineForHarvest.getStats(MetricName.create(MetricNames.SUPPORTABILITY_ERROR_SERVICE_TRANSACTION_ERROR_SENT)).hasData());
        Assert.assertTrue(statsEngineForHarvest.getStats(MetricName.create(MetricNames.SUPPORTABILITY_ERROR_SERVICE_TRANSACTION_ERROR_SEEN)).hasData());
        Assert.assertEquals(1, ((MockRPMService) ServiceFactory.getRPMService()).getEvents().size());

        ErrorEvent errorEvent = (ErrorEvent) Iterables.get(((MockRPMService) ServiceFactory.getRPMService()).getEvents(), 0);
        Assert.assertEquals(errorEvent.getUserAttributesCopy().get("test_attribute"), "value");
        Assert.assertEquals("TransactionError", errorEvent.getType());
    }

    private void setupAndVerifyStripExceptionMessage(ConfigEnhancer enhancer, Boolean highSecurity, Boolean stripException,
            String allowedExceptionClasses, boolean expectedToBeStripped, Throwable exception) throws Exception {
        Map<String, Object> config = createConfig(null, highSecurity, stripException, allowedExceptionClasses);
        enhancer.enhance(config, exception);
        EventTestHelper.createServiceManager(config);

        ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();

        MockRPMService rpmService = (MockRPMService) ServiceFactory.getRPMService();
        rpmService.setIsConnected(true);

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getErrorCollectorConfig(APP_NAME);

        TracedError error = ThrowableError
                .builder(errorCollectorConfig, APP_NAME, "dude", exception, System.currentTimeMillis())
                .errorMessageReplacer(new ErrorMessageReplacer(ServiceFactory.getConfigService().getStripExceptionConfig(APP_NAME)))
                .build();
        errorService.reportError(error);

        // Checking ...

        StatsEngineImpl statsEngine = new StatsEngineImpl();
        List<TracedError> actualErrors = errorService.getAndClearTracedErrors(APP_NAME, statsEngine);

        if (enhancer.shouldMatch()) {
            // If we supplied a configuration that ignored the error,
            // check that it worked, and we're done here. This verifies
            // the fix for JAVA-2975.
            Assert.assertEquals(0, actualErrors.size());
            return;
        }

        Assert.assertEquals(1, actualErrors.size());
        TracedError tracedError = actualErrors.get(0);

        String expectedMessage = exception.getMessage();

        // The exception message should be stripped in the following cases:
        // - High Security On (except when strip exceptions is explicitly disabled)
        // - Strip Exceptions On
        if (expectedToBeStripped && (highSecurity != null && highSecurity)) {
            if (stripException == null || stripException) {
                expectedMessage = ErrorMessageReplacer.STRIPPED_EXCEPTION_REPLACEMENT;
            }
        } else if (expectedToBeStripped && (stripException != null && stripException)) {
            expectedMessage = ErrorMessageReplacer.STRIPPED_EXCEPTION_REPLACEMENT;
        }

        Assert.assertEquals("High Security = " + (highSecurity != null ? highSecurity.toString() : "Unset")
                        + ", Strip Exceptions = " + (stripException != null ? stripException.toString() : "Unset")
                        + ", Exceptions to be allowed unstripped = " + (allowedExceptionClasses != null ? allowedExceptionClasses : "Unset"), expectedMessage,
                tracedError.getMessage());
    }

    private TransactionData createTransactionData(int responseStatus, Throwable throwable, boolean expectedError) {
        return createTransactionData(false, responseStatus, throwable, expectedError);
    }

    private TransactionData createTransactionData(boolean isWebTransaction, int responseStatus, Throwable throwable, boolean expectedError) {
        return createTransactionData(isWebTransaction, responseStatus, throwable, expectedError,
                Collections.<String, String>emptyMap(), Collections.<String, Object>emptyMap(),
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
                Collections.<String, Object>emptyMap());
    }

    private TransactionData createTransactionData(boolean isWebTransaction, int responseStatus, Throwable throwable,
            boolean expectedError, Map<String, String> requestParams, Map<String, Object> userParams, Map<String, Object> agentParams,
            Map<String, Object> errorParams, Map<String, Object> intrinsics) {
        AgentConfig iAgentConfig = mock(AgentConfig.class);
        ErrorCollectorConfig errorCollectorConfig = mock(ErrorCollectorConfig.class);
        when(iAgentConfig.getErrorCollectorConfig()).thenReturn(errorCollectorConfig);
        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setWebTransaction(isWebTransaction);
        Tracer rootTracer = new MockDispatcherTracer();
        String frontendMetricName = isWebTransaction ? "WebTransaction/Uri/dude" : "OtherTransaction/Custom/dude";

        return new TransactionDataTestBuilder(APP_NAME, iAgentConfig, rootTracer)
                .setDispatcher(dispatcher)
                .setFrontendMetricName(frontendMetricName)
                .setThrowable(throwable)
                .setExpectedError(expectedError)
                .setRequestUri("/dude")
                .setResponseStatus(responseStatus)
                .setStatusMessage("")
                .setRequestParams(requestParams)
                .setAgentParams(agentParams)
                .setUserParams(userParams)
                .setErrorParams(errorParams)
                .setIntrinsics(intrinsics)
                .build();
    }

    // An object that knows how to add "stuff" to a test configuration.
    private abstract class ConfigEnhancer {
        abstract void enhance(Map<String, Object> config, Throwable expected);

        boolean shouldMatch() {
            return false;
        }
    }

    // An enhancer that doesn't enhance - for support of legacy test case
    private class NoOpConfigEnhancer extends ConfigEnhancer {
        @Override
        void enhance(Map<String, Object> config, Throwable expected) {
        }
    }

    // Add an error collector config that doesn't match anything the test does.
    private class NonMatchingConfigEnhancer extends ConfigEnhancer {
        @Override
        void enhance(Map<String, Object> config, Throwable expected) {
            List<Map<String, String>> ignoreErrorsList = new ArrayList<>();
            Map<String, String> ignoreErrorMap = new HashMap<>();
            Map<String, String> otherIgnoreErrorMap = new HashMap<>();
            ignoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, "com.enron.power.MarketNotCorneredException");
            otherIgnoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, "com.informix.cfo.GetOutOfJailFreeCard");
            ignoreErrorsList.add(ignoreErrorMap);
            ignoreErrorsList.add(otherIgnoreErrorMap);

            Map<String, Object> errorCollectorMap = new HashMap<>();
            errorCollectorMap.put("enabled", true);
            errorCollectorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
            errorCollectorMap.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreErrorsList);

            config.put("error_collector", errorCollectorMap);
        }
    }

    // Add an error collector config that matches the exception thrown by the test, filtering it.
    private class MatchingConfigEnhancer extends ConfigEnhancer {
        @Override
        void enhance(Map<String, Object> config, Throwable expected) {
            List<Map<String, String>> ignoreErrorsList = new ArrayList<>();
            Map<String, String> ignoreErrorMap = new HashMap<>();
            Map<String, String> otherIgnoreErrorMap = new HashMap<>();
            ignoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, expected.getClass().getName());
            ignoreErrorMap.put(ErrorCollectorConfigImpl.MESSAGE, expected.getMessage());
            otherIgnoreErrorMap.put(ErrorCollectorConfigImpl.CLASS_NAME, "com.informix.cfo.GetOutOfJailFreeCard");
            ignoreErrorsList.add(ignoreErrorMap);
            ignoreErrorsList.add(otherIgnoreErrorMap);

            Map<String, Object> errorCollectorMap = new HashMap<>();
            errorCollectorMap.put("enabled", true);
            errorCollectorMap.put(ErrorCollectorConfigImpl.COLLECT_ERRORS, true);
            errorCollectorMap.put(ErrorCollectorConfigImpl.IGNORE_CLASSES, ignoreErrorsList);

            config.put("error_collector", errorCollectorMap);
        }

        @Override
        boolean shouldMatch() {
            return true;
        }
    }
}
