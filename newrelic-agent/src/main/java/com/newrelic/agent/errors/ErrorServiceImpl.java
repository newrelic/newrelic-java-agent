/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.Harvestable;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.StripExceptionConfig;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.ErrorEventFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.transport.HttpError;

import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;

public class ErrorServiceImpl extends AbstractService implements ErrorService, HarvestListener {

    @VisibleForTesting
    static final int ERROR_LIMIT_PER_REPORTING_PERIOD = 20;

    @VisibleForTesting
    final AtomicInteger errorCountThisHarvest = new AtomicInteger();
    final AtomicInteger expectedErrorCountThisHarvest = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final AtomicLong totalErrorCount = new AtomicLong();
    private final AtomicReferenceArray<TracedError> tracedErrors;
    private final ConcurrentHashMap<String, DistributedSamplingPriorityQueue<ErrorEvent>> reservoirForApp = new ConcurrentHashMap<>();
    private volatile ErrorCollectorConfig errorCollectorConfig;
    private volatile ErrorMessageReplacer errorMessageReplacer;
    private volatile StripExceptionConfig stripExceptionConfig;
    private volatile ErrorAnalyzer errorAnalyzer;
    private final boolean shouldRecordErrorCount;
    private volatile int maxSamplesStored;
    private final String appName;

    protected Harvestable harvestable;

    /**
     * Note that an instance of this class is created for each RPMService, which is a side effect of using
     * auto app naming and the servlet instrumentation.
     */
    public ErrorServiceImpl(String appName) {
        super(ErrorService.class.getSimpleName());
        this.appName = appName;
        errorCollectorConfig = ServiceFactory.getConfigService().getErrorCollectorConfig(appName);
        errorAnalyzer = new ErrorAnalyzerImpl(errorCollectorConfig);
        stripExceptionConfig = ServiceFactory.getConfigService().getStripExceptionConfig(appName);
        errorMessageReplacer = new ErrorMessageReplacer(stripExceptionConfig);
        tracedErrors = new AtomicReferenceArray<>(ERROR_LIMIT_PER_REPORTING_PERIOD);
        ServiceFactory.getTransactionService().addTransactionListener(new MyTransactionListener());
        ServiceFactory.getConfigService().addIAgentConfigListener(new MyConfigListener());
        shouldRecordErrorCount = !Boolean.getBoolean("com.newrelic.agent.errors.no_error_metric");
        maxSamplesStored = errorCollectorConfig.getMaxSamplesStored();
    }

    @Override
    protected void doStart() throws Exception {
        if (isEnabled()) {
            ServiceFactory.getHarvestService().addHarvestListener(this);
        }
    }

    @Override
    public boolean isEnabled() {
        return errorCollectorConfig.isEnabled();
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestable(harvestable);
    }

    @Override
    public void addHarvestableToService() {
        Harvestable harvestableToAdd = new ErrorHarvestableImpl(this, appName);
        ServiceFactory.getHarvestService().addHarvestable(harvestableToAdd);
        harvestable = harvestableToAdd;
    }

    public ErrorCollectorConfig getErrorCollectorConfig() {
        return errorCollectorConfig;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    public void setMaxSamplesStored(int maxSamplesStored) {
        this.maxSamplesStored = maxSamplesStored;
    }

    public void clearReservoir() {
        reservoirForApp.clear();
    }

    public void clearReservoir(String appName) {
        DistributedSamplingPriorityQueue<ErrorEvent> reservoir = reservoirForApp.get(appName);
        if (reservoir != null) {
            reservoir.clear();
        }
    }

    @VisibleForTesting
    public void setHarvestable(Harvestable harvestable) {
        this.harvestable = harvestable;
    }

    public void harvestEvents(final String appName) {
        boolean eventsEnabled = isEventsEnabledForApp(appName);
        if (!eventsEnabled) {
            reservoirForApp.remove(appName);
            return;
        }
        if (maxSamplesStored <= 0) {
            clearReservoir(appName);
            return;
        }

        long startTimeInNanos = System.nanoTime();

        final DistributedSamplingPriorityQueue<ErrorEvent> reservoir = reservoirForApp.put(appName,
                new DistributedSamplingPriorityQueue<ErrorEvent>(appName, "Error Service", maxSamplesStored));

        if (reservoir != null && reservoir.size() > 0) {
            try {
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(appName)
                        .sendErrorEvents(maxSamplesStored, reservoir.getNumberOfTries(), Collections.unmodifiableList(reservoir.asList()));

                final long durationInNanos = System.nanoTime() - startTimeInNanos;
                ServiceFactory.getStatsService().doStatsWork(new StatsWork() {
                    @Override
                    public void doWork(StatsEngine statsEngine) {
                        recordSupportabilityMetrics(statsEngine, durationInNanos, reservoir);
                    }

                    @Override
                    public String getAppName() {
                        return appName;
                    }
                }, reservoir.getServiceName());

                if (reservoir.size() < reservoir.getNumberOfTries()) {
                    int dropped = reservoir.getNumberOfTries() - reservoir.size();
                    Agent.LOG.log(Level.FINE, "Dropped {0} error events out of {1}.", dropped, reservoir.getNumberOfTries());
                }
            } catch (HttpError e) {
                if (!e.discardHarvestData()) {
                    Agent.LOG.log(Level.FINE, "Unable to send error events. Unsent events will be included in the next harvest.", e);
                    // Save unsent data by merging it with current data using reservoir algorithm
                    DistributedSamplingPriorityQueue<ErrorEvent> currentReservoir = reservoirForApp.get(appName);
                    currentReservoir.retryAll(reservoir);
                } else {
                    // discard harvest data
                    reservoir.clear();
                    Agent.LOG.log(Level.FINE, "Unable to send error events. Unsent events will be dropped.", e);
                }
            } catch (Exception e) {
                // discard harvest data
                reservoir.clear();
                Agent.LOG.log(Level.FINE, "Unable to send error events. Unsent events will be dropped.", e);
            }
        }
    }

    @Override
    public String getEventHarvestIntervalMetric() {
        return MetricNames.SUPPORTABILITY_ERROR_SERVICE_EVENT_HARVEST_INTERVAL;
    }

    @Override
    public String getReportPeriodInSecondsMetric() {
        return MetricNames.SUPPORTABILITY_ERROR_SERVICE_REPORT_PERIOD_IN_SECONDS;
    }

    @Override
    public String getEventHarvestLimitMetric() {
        return MetricNames.SUPPORTABILITY_ERROR_EVENT_DATA_HARVEST_LIMIT;
    }

    private void recordSupportabilityMetrics(StatsEngine statsEngine, long durationInNanos,
            DistributedSamplingPriorityQueue<ErrorEvent> reservoir) {
        statsEngine.getStats(MetricNames.SUPPORTABILITY_ERROR_SERVICE_TRANSACTION_ERROR_SENT)
                .incrementCallCount(reservoir.size());
        statsEngine.getStats(MetricNames.SUPPORTABILITY_ERROR_SERVICE_TRANSACTION_ERROR_SEEN)
                .incrementCallCount(reservoir.getNumberOfTries());
        statsEngine.getResponseTimeStats(MetricNames.SUPPORTABILITY_ERROR_SERVICE_EVENT_HARVEST_TRANSMIT)
                .recordResponseTime(durationInNanos, TimeUnit.NANOSECONDS);
    }

    // Called from beforeHarvest listener. Send traced errors for this app upstream.
    @VisibleForTesting
    public void harvestTracedErrors(String appName, StatsEngine statsEngine) {
        if (!appName.equals(this.appName)) {
            // We may have multiple app names if auto_app_naming is enabled. There will be an ErrorServiceImpl for each app name
            return;
        }

        List<TracedError> tracedErrorList = getAndClearTracedErrors(appName, statsEngine);
        if (tracedErrorList != null && tracedErrorList.size() > 0) {
            try {
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(appName)
                        .sendErrorData(tracedErrorList);
            } catch (Exception ex) {
                Agent.LOG.log(Level.FINE, ex, "Unable to send error events.");
            }
        }
    }

    private boolean isEnabledForApp(String appName) {
        return ServiceFactory.getConfigService().getErrorCollectorConfig(appName).isEnabled();
    }

    private boolean isEventsEnabledForApp(String appName) {
        return ServiceFactory.getConfigService().getErrorCollectorConfig(appName).isEventsEnabled();
    }

    @VisibleForTesting
    public void refreshErrorCollectorConfig(AgentConfig agentConfig) {
        ErrorCollectorConfig oldErrorConfig = errorCollectorConfig;
        errorCollectorConfig = agentConfig.getErrorCollectorConfig();
        errorAnalyzer = new ErrorAnalyzerImpl(errorCollectorConfig);

        if (errorCollectorConfig.isEnabled() == oldErrorConfig.isEnabled()) {
            return;
        }
        Agent.LOG.log(Level.INFO,
                errorCollectorConfig.isEnabled()
                        ? "Errors will be sent to New Relic for {0}"
                        : "Errors will not be sent to New Relic for {0}", appName);
    }

    @VisibleForTesting
    void refreshStripExceptionConfig(AgentConfig agentConfig) {
        StripExceptionConfig oldStripExceptionConfig = stripExceptionConfig;
        stripExceptionConfig = agentConfig.getStripExceptionConfig();
        if (stripExceptionConfig.isEnabled() != oldStripExceptionConfig.isEnabled()) {
            Agent.LOG.info(MessageFormat.format(
                    "Exception messages will{0} be stripped before sending to New Relic for {1}",
                    stripExceptionConfig.isEnabled() ? "" : " not", appName));
        }
        if (!stripExceptionConfig.getAllowedClasses().equals(oldStripExceptionConfig.getAllowedClasses())) {
            Agent.LOG.info(MessageFormat.format("Exception classes allowed to keep their messages updated to {0} for {1}",
                    stripExceptionConfig.getAllowedClasses().toString(), appName));
        }

        errorMessageReplacer = new ErrorMessageReplacer(stripExceptionConfig);
    }

    @Override
    public void reportErrors(TracedError... errors) {
        for (TracedError error : errors) {
            reportError(error);
        }
    }

    @Override
    public void reportError(TracedError error) {
        // This is called from a static method in this class but through the ServiceFactory,
        // so it really needs to be public and on the interface even though IJ says it doesn't.
        reportError(error, null, null);
    }

    @VisibleForTesting // Introspector subclasses this class
    protected void reportError(TracedError error, TransactionData transactionData, TransactionStats transactionStats) {
        if (error == null) {
            return;
        }
        if (error instanceof ThrowableError && getErrorAnalyzer().isIgnoredThrowable(((ThrowableError) error).getThrowable())) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Throwable throwable = ((ThrowableError) error).getThrowable();
                String errorString = throwable == null ? "" : throwable.getClass().getName();
                String msg = MessageFormat.format("Ignoring error {0} for {1}", errorString, appName);
                Agent.LOG.finer(msg);
            }
            return;
        }
        if (error.incrementsErrorMetric()) {
            errorCountThisHarvest.incrementAndGet();
        } else if (!(error instanceof DeadlockTraceError)) {
            expectedErrorCountThisHarvest.incrementAndGet();
        }
        if (!errorCollectorConfig.isEnabled() || !isEventsEnabledForApp(appName) || maxSamplesStored <= 0) {
            return;
        }

        // Siphon off errors to send up as error events
        DistributedSamplingPriorityQueue<ErrorEvent> eventList = getReservoir(appName);

        ErrorEvent errorEvent = createErrorEvent(appName, error, transactionData, transactionStats);

        eventList.add(errorEvent);

        if (errorCount.get() >= ERROR_LIMIT_PER_REPORTING_PERIOD) {
            Agent.LOG.finer(MessageFormat.format("Error limit exceeded for {0}: {1}", appName, error));
            return;
        }
        int index = (int) totalErrorCount.getAndIncrement() % ERROR_LIMIT_PER_REPORTING_PERIOD;
        if (tracedErrors.compareAndSet(index, null, error)) {
            errorCount.getAndIncrement();
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.finer(MessageFormat.format("Recording error for {0} : {1}", appName, error));
            }
        }
    }

    @VisibleForTesting // Introspector subclasses this class
    protected static ErrorEvent createErrorEvent(final String theAppName, TracedError error,
            TransactionData transactionData, TransactionStats transactionStats) {
        ErrorEvent errorEvent;
        if (transactionData != null) {
            errorEvent = ErrorEventFactory.create(theAppName, error, transactionData, transactionStats);
        } else {
            errorEvent = ErrorEventFactory.create(theAppName, error, DistributedTraceServiceImpl.nextTruncatedFloat());
        }
        return errorEvent;
    }

    /**
     * Get the traced errors for this reporting period and increment the error stats.
     *
     * @param appName the name of the app we're harvesting for.
     * @return the traced errors for this reporting period.
     */
    @VisibleForTesting
    List<TracedError> getAndClearTracedErrors(String appName, StatsEngine statsEngine) {
        recordMetrics(appName, statsEngine);
        if (ServiceFactory.getRPMServiceManager()
                .getOrCreateRPMService(appName)
                .isConnected()) {
            return getAndClearTracedErrors();
        }

        return Collections.emptyList();
    }

    /**
     * Get the traced errors for this reporting period.
     *
     * @return the traced errors for this reporting period.
     */
    @VisibleForTesting
    public List<TracedError> getAndClearTracedErrors() {
        List<TracedError> errors = new ArrayList<>(ERROR_LIMIT_PER_REPORTING_PERIOD);
        for (int i = 0; i < tracedErrors.length(); i++) {
            TracedError error = tracedErrors.getAndSet(i, null);
            if (error != null) {
                errorCount.getAndDecrement();
                errors.add(error);
            }
        }
        return errors;
    }

    public ErrorAnalyzer getErrorAnalyzer() {
        return errorAnalyzer;
    }

    @VisibleForTesting
    public int getTracedErrorsCount() {
        return errorCount.get();
    }

    private void recordMetrics(final String appName, StatsEngine statsEngine) {
        if (shouldRecordErrorCount) {
            int errorCount = errorCountThisHarvest.getAndSet(0);
            statsEngine.getStats(MetricNames.ERRORS_ALL).incrementCallCount(errorCount);
            int expectedErrorCount = expectedErrorCountThisHarvest.getAndSet(0);
            statsEngine.getStats(MetricNames.ERRORS_EXPECTED_ALL).incrementCallCount(expectedErrorCount);
        }
    }

    /**
     * Check if the transaction has an error to report.
     */
    private void noticeTransaction(TransactionData td, TransactionStats transactionStats) {
        if (!appName.equals(td.getApplicationName())) {
            return;
        }
        if (!isEnabledForApp(td.getApplicationName())) {
            return;
        }

        String statusMessage = td.getStatusMessage();
        int responseStatus = td.getResponseStatus();
        Throwable throwable = td.getThrowable() == null ? null : td.getThrowable().throwable;
        final boolean isReportable = responseStatus >= HttpURLConnection.HTTP_BAD_REQUEST || throwable != null;
        if (throwable instanceof ReportableError) {
            // Someone manually called NewRelic.noticeError(String message) here
            // so we don't want to use getStrippedExceptionMessage() for replacement
            statusMessage = throwable.getMessage();
            throwable = null;
        }

        if (isReportable) {
            if (!td.hasReportableErrorThatIsNotIgnored()) {
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String errorString = throwable == null ? "" : throwable.getClass().getName();
                    String msg = MessageFormat.format("Ignoring error {0} for {1} {2} ({3})", errorString,
                            td.getRequestUri(AgentConfigImpl.ERROR_COLLECTOR), appName, responseStatus);
                    Agent.LOG.finer(msg);
                }
                return;
            }

            TracedError error = createTracedError(appName, td, throwable, responseStatus, statusMessage);
            if (shouldRecordErrorCount && error.incrementsErrorMetric()) {
                recordErrorCount(td, transactionStats);
            }

            reportError(error, td, transactionStats);
        }
    }

    private TracedError createTracedError(final String theAppName, TransactionData td, Throwable throwable, int responseStatus, String statusMessage) {
        TracedError error;
        // noticeError(expected = true)?
        boolean responseStatusExpected = errorCollectorConfig.getExpectedStatusCodes().contains(responseStatus);
        boolean throwableExpected = td.getThrowable() == null ? false : td.getThrowable().expected;
        boolean markedExpected = responseStatusExpected || throwableExpected;

        Map<String, Object> joinedIntrinsics = new HashMap<>(td.getIntrinsicAttributes());
        DistributedTraceService distributedTraceService = ServiceFactory.getDistributedTraceService();
        DistributedTracingConfig distributedTracingConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig();

        joinedIntrinsics.put("guid", td.getGuid());

        if (distributedTracingConfig.isEnabled()) {
            joinedIntrinsics.putAll(distributedTraceService.getIntrinsics(
                    td.getInboundDistributedTracePayload(), td.getGuid(), td.getTraceId(), td.getTransportType(),
                    td.getTransportDurationInMillis(), td.getLargestTransportDurationInMillis(), td.getParentId(),
                    td.getParentSpanId(), td.getPriority()));
        }

        if (throwable != null) {
            error = ThrowableError
                    .builder(errorCollectorConfig, theAppName, td.getBlameOrRootMetricName(), throwable, td.getWallClockStartTimeMs())
                    .errorMessageReplacer(errorMessageReplacer)
                    .requestUri(td.getRequestUri(AgentConfigImpl.ERROR_COLLECTOR))
                    .prefixedAttributes(td.getPrefixedAttributes())
                    .userAttributes(td.getUserAttributes())
                    .agentAttributes(td.getAgentAttributes())
                    .errorAttributes(td.getErrorAttributes())
                    .intrinsicAttributes(joinedIntrinsics)
                    .expected(markedExpected)
                    .transactionGuid(td.getGuid())
                    .build();
        } else {
            error = HttpTracedError
                    .builder(errorCollectorConfig, theAppName, td.getBlameOrRootMetricName(), td.getWallClockStartTimeMs())
                    .statusCodeAndMessage(responseStatus, statusMessage)
                    .transactionData(td)
                    .requestUri(td.getRequestUri(AgentConfigImpl.ERROR_COLLECTOR))
                    .prefixedAttributes(td.getPrefixedAttributes())
                    .userAttributes(td.getUserAttributes())
                    .agentAttributes(td.getAgentAttributes())
                    .errorAttributes(td.getErrorAttributes())
                    .intrinsicAttributes(joinedIntrinsics)
                    .expected(markedExpected)
                    .transactionGuid(td.getGuid())
                    .build();
        }
        return error;
    }

    private void recordErrorCount(TransactionData td, TransactionStats transactionStats) {
        String metricName = getErrorCountMetricName(td);
        if (metricName != null) {
            transactionStats.getUnscopedStats().getStats(metricName).incrementCallCount();
        }
        String metricNameAll = td.isWebTransaction() ? MetricNames.WEB_TRANSACTION_ERRORS_ALL
                : MetricNames.OTHER_TRANSACTION_ERRORS_ALL;
        transactionStats.getUnscopedStats().getStats(metricNameAll).incrementCallCount();
    }

    private String getErrorCountMetricName(TransactionData td) {
        String blameMetricName = td.getBlameMetricName();
        if (blameMetricName != null) {
            StringBuilder output = new StringBuilder(MetricNames.ERRORS_SLASH.length() + blameMetricName.length());
            output.append(MetricNames.ERRORS_SLASH);
            output.append(blameMetricName);
            return output.toString();
        }
        return null;
    }

    @VisibleForTesting
    public DistributedSamplingPriorityQueue<ErrorEvent> getReservoir(String appName) {
        DistributedSamplingPriorityQueue<ErrorEvent> result = reservoirForApp.get(appName);
        while (result == null) {
            // I don't think this loop can actually execute more than once, but it's prudent to assume it can.
            reservoirForApp.putIfAbsent(appName, new DistributedSamplingPriorityQueue<ErrorEvent>(appName, "Error Service", maxSamplesStored));
            result = reservoirForApp.get(appName);
        }
        return result;
    }

    /**
     * Checks if exception should be ignored.
     */
    @Override
    public void reportException(Throwable throwable) {
        if (getErrorAnalyzer().isIgnoredThrowable(throwable)) {
            Agent.LOG.finer(MessageFormat.format("Ignoring error with throwable {0} ", throwable));
        } else {
            reportException(throwable, Collections.<String, String>emptyMap(), false);
        }
    }

    /**
     * Report an exception to New Relic. Implements the public API.
     */
    @Override
    public void reportException(Throwable throwable, Map<String, ?> params, boolean expected) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null && tx.isInProgress()) {
            final TransactionActivity transactionActivity = TransactionActivity.get();
            if (transactionActivity != null && transactionActivity.getLastTracer() != null) {
                transactionActivity.getLastTracer().setNoticedError(throwable);
            }
            if (params != null) {
                tx.getErrorAttributes().putAll(params);
            }
            synchronized (tx) {
                tx.setThrowable(throwable, TransactionErrorPriority.API, expected);
            }
        } else {
            // we're not within a transaction. just report the error
            TracedError error = ThrowableError
                    .builder(errorCollectorConfig, null, "Unknown", throwable, System.currentTimeMillis())
                    .errorMessageReplacer(errorMessageReplacer)
                    .errorAttributes(params)
                    .expected(expected)
                    .build();

            reportError(error);
        }
    }

    @Override
    public void reportError(String message, Map<String, ?> params, boolean expected) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null && tx.isInProgress()) {
            final TransactionActivity transactionActivity = TransactionActivity.get();
            if (transactionActivity != null && transactionActivity.getLastTracer() != null) {
                transactionActivity.getLastTracer().setNoticedError(new ReportableError(message));
            }
            if (params != null) {
                tx.getErrorAttributes().putAll(params);
            }
            synchronized (tx) {
                tx.setThrowable(new ReportableError(message), TransactionErrorPriority.API, expected);
            }
        } else {
            // we're not within a transaction. just report the error
            TracedError error = HttpTracedError
                    .builder(errorCollectorConfig, null, "Unknown", System.currentTimeMillis())
                    .message(message)
                    .errorAttributes(params)
                    .expected(expected)
                    .build();

            reportError(error);
        }
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        harvestTracedErrors(appName, statsEngine);
    }

    @Override
    public void afterHarvest(String appName) {
    }

    private class MyTransactionListener implements TransactionListener {
        @Override
        public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
            noticeTransaction(transactionData, transactionStats);
        }
    }

    private class MyConfigListener implements AgentConfigListener {
        @Override
        public void configChanged(String appName, AgentConfig agentConfig) {
            if (ErrorServiceImpl.this.appName.equals(appName)) {
                Agent.LOG.fine(MessageFormat.format("Error service received configuration change notification for {0}", appName));
                refreshErrorCollectorConfig(agentConfig);
                refreshStripExceptionConfig(agentConfig);
            }
        }
    }

    public static Collection<? extends PointCut> getEnabledErrorHandlerPointCuts() {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        Object exceptionHandlers = config.getErrorCollectorConfig().getExceptionHandlers();

        Collection<PointCut> pointcuts = new ArrayList<>();
        if (exceptionHandlers instanceof Collection<?>) {
            for (Object sigObject : ((Collection<?>) exceptionHandlers)) {
                if (sigObject instanceof ExceptionHandlerSignature) {
                    ExceptionHandlerSignature exHandlerSig = (ExceptionHandlerSignature) sigObject;

                    String msg = MessageFormat.format("Instrumenting exception handler signature {0}",
                            exHandlerSig.toString());
                    Agent.LOG.finer(msg);
                    ExceptionHandlerPointCut pc = new ExceptionHandlerPointCut(exHandlerSig);
                    if (pc.isEnabled()) {
                        pointcuts.add(pc);
                    }
                } else if (sigObject instanceof String) {
                    ClassMethodSignature signature = PointCutFactory.parseClassMethodSignature(sigObject.toString());

                    try {
                        ExceptionHandlerSignature exHandlerSig = new ExceptionHandlerSignature(signature);
                        Agent.LOG.info(MessageFormat.format("Instrumenting exception handler signature {0}",
                                exHandlerSig.toString()));
                        ExceptionHandlerPointCut pc = new ExceptionHandlerPointCut(exHandlerSig);
                        if (pc.isEnabled()) {
                            pointcuts.add(pc);
                        }
                    } catch (InvalidMethodDescriptor e) {
                        Agent.LOG.severe(MessageFormat.format("Unable to instrument exception handler {0} : {1}",
                                sigObject.toString(), e.toString()));
                    }
                } else if (sigObject instanceof Exception) {
                    Agent.LOG.severe(MessageFormat.format("Unable to instrument exception handler : {0}",
                            sigObject.toString()));
                }
            }
        }
        return pointcuts;
    }

    @Override
    public void reportHTTPError(String message, int statusCode, String uri) {
        if (!getErrorAnalyzer().isIgnoredStatus(statusCode)) {
            TracedError error = HttpTracedError
                    .builder(errorCollectorConfig, null, "WebTransaction" + uri, System.currentTimeMillis())
                    .statusCodeAndMessage(statusCode, message)
                    .requestUri(uri)
                    .build();

            reportError(error);
            Agent.LOG.finer(MessageFormat.format("Reported HTTP error {0} with status code {1} URI {2}", message, statusCode, uri));
        } else {
            Agent.LOG.finer(MessageFormat.format("Ignoring HTTP error {0} with status code {1} URI {2}", message, statusCode, uri));
        }
    }
}
