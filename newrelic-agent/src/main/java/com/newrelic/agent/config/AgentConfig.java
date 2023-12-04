/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.transaction.TransactionNamingScheme;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * General configuration for the agent. Access this by calling
 * {@code ServiceFactory.getConfigService().getDefaultAgentConfig()} from internal classes, or
 * {@code AgentBridge.privateApi.getAgentConfig()} from instrumentation classes. <br/>
 * <br/>
 * Adding new methods to this is no longer preferred. Please access new config using the {@link com.newrelic.api.agent.Config#getValue(String)}
 * method with the keyPath being '.' delimited hierarchy of keys, similar to the command line setting override.<br/>
 * <br/>
 * Ex:
 * {@code AgentBridge.privateApi.getAgentConfig().getValue("instrumentation.hibernate.stats_sampler.enabled", false)} <br/>
 * Ex:
 * {@code ServiceFactory.getConfigService().getDefaultAgentConfig().getValue("class_transformer.netty_dispatcher_class")}
 */
public interface AgentConfig extends com.newrelic.api.agent.Config, DataSenderConfig {

    boolean isAgentEnabled();

    /**
     * Get the main application.
     */
    String getApplicationName();

    /**
     * The applications the Agent reports data to.
     */
    List<String> getApplicationNames();

    /**
     * If <code>true</code> the application name for a web transaction is determined automatically.
     *
     * @return <code>true</code> if auto app naming is enabled
     */
    boolean isAutoAppNamingEnabled();

    /**
     * If the enable_auto_transaction_naming property is <code>true</code> the name of a web transaction is determined
     * automatically.
     *
     * @return <code>true</code> if auto transaction naming is enabled
     */
    boolean isAutoTransactionNamingEnabled();

    /**
     * Get the ApdexT value sent by New Relic, or the default value.
     */
    long getApdexTInMillis();

    /**
     * If NewRelic sent an ApdexT for a key transaction, return that; otherwise, return the ApdextT value for the
     * application.
     */
    long getApdexTInMillis(String transactionName);

    /**
     * Returns true if apdex T was specified (not just the default).
     */
    boolean isApdexTSet();

    /**
     * Has the given transaction name been configured as a Key Transaction with an ApdexT?
     */
    boolean isApdexTSet(String transactionName);

    /**
     * Get the limit on the size of a transaction trace.
     *
     * @return the size limit in bytes
     */
    int getTransactionSizeLimit();

    /**
     * If <code>true</code> the Agent connects to New Relic on startup.
     */
    boolean isSyncStartup();

    /**
     * If <code>true</code> the Agent should wait until it has connected to New Relic before processing transactions.
     * Until the Agent connects it has no URL rules or ApdexT.
     */
    boolean waitForRPMConnect();

    /**
     * Get the transaction tracer application settings.
     */
    TransactionTracerConfig getTransactionTracerConfig();

    /**
     * Get the class transformer settings.
     */
    ClassTransformerConfig getClassTransformerConfig();

    /**
     * Get the browser monitoring application settings.
     */
    BrowserMonitoringConfig getBrowserMonitoringConfig();

    /**
     * Get the transaction tracer application settings for request transactions.
     */
    TransactionTracerConfig getRequestTransactionTracerConfig();

    /**
     * Get the transaction tracer application settings for background transactions.
     */
    TransactionTracerConfig getBackgroundTransactionTracerConfig();

    /**
     * Get the error collection application settings.
     */
    ErrorCollectorConfig getErrorCollectorConfig();

    /**
     * Get the thread profiling application settings.
     */
    ThreadProfilerConfig getThreadProfilerConfig();

    /**
     * Get the sql trace application settings.
     */
    SqlTraceConfig getSqlTraceConfig();

    /**
     * Gets the JFR configuration settings.
     *
     * @return JFR configuration settings.
     */
    JfrConfig getJfrConfig();

    /**
     * Gets the JMX configuration settings.
     *
     * @return JMX configuration settings.
     */
    JmxConfig getJmxConfig();

    /**
     * Gets the JarCollectorService configuration settings.
     *
     * @return JarCollectorService configuration settings.
     */
    JarCollectorConfig getJarCollectorConfig();

    /**
     * Gets the Reinstrumentation configuration settings.
     *
     * @return Reinstrumentation configuration settings.
     */
    ReinstrumentConfig getReinstrumentConfig();

    /**
     * Get the cross process application settings.
     */
    CrossProcessConfig getCrossProcessConfig();

    /**
     * Get the Insights configuration.
     */
    InsightsConfig getInsightsConfig();

    /**
     * Get the application logging configuration.
     *
     * @return ApplicationLoggingConfig used by LogSenderService
     */
    ApplicationLoggingConfig getApplicationLoggingConfig();

    /**
     * Get the Code Level Metrics config.
     */
    CodeLevelMetricsConfig getCodeLevelMetricsConfig();

    /**
     * Get the attributes configuration.
     */
    AttributesConfig getAttributesConfig();

    /**
     * The agent language (java).
     */
    String getLanguage();

    /**
     * Get a property value.
     *
     * @return the property value or null if absent
     */
    <T> T getProperty(String prop);

    /**
     * Get a property value.
     *
     * @return the property value of the default value if absent
     */
    <T> T getProperty(String key, T defaultVal);

    boolean isSendEnvironmentInfo();

    String getApiHost();

    int getApiPort();

    boolean isDebugEnabled();

    boolean isDebugEnabled(String key);

    boolean isLoggingToStdOut();

    String getLogFileName();

    String getLogFilePath();

    String getLogLevel();

    /**
     * Jars which should be ignored by java agent and thus should not be sent up to the collector.
     *
     * @return The list of jars to be ignored.
     */
    List<String> getIgnoreJars();

    /**
     * The maximum number of kilobytes to write to any one log file. Default is 0 (no limit).
     */
    int getLogLimit();

    /**
     * The number of log files to use. Default is 1.
     */
    int getLogFileCount();

    /**
     * Logs a daily log if set to true.
     *
     * @return True if a daily log should be created.
     */
    boolean isLogDaily();

    /**
     * If true send data to the server on exit.
     */
    boolean isSendDataOnExit();

    /**
     * Get the threshold for sending data to the server on exit. Only send data if the JVM has been running longer than
     * the threshold.
     */
    long getSendDataOnExitThresholdInMillis();

    boolean isCpuSamplingEnabled();

    /**
     * Gets the field obfuscateJvmProps.
     *
     * @return the obfuscateJvmProps
     */
    boolean isSendJvmProps();

    boolean isTrimStats();

    boolean isPlatformInformationEnabled();

    Set<String> getJDBCSupport();

    boolean isGenericJDBCSupportEnabled();

    int getMaxStackTraceLines();

    Config getInstrumentationConfig();

    String getMetricIngestUri();

    String getEventIngestUri();

    boolean isHighSecurity();

    /**
     * Get the agent's label configuration.
     *
     * @return label configuration
     */
    LabelsConfig getLabelsConfig();

    NormalizationRuleConfig getNormalizationRuleConfig();

    /**
     * If true, agent startup time will be recorded and sent as a supportability metric
     *
     * @return true if startup timing is enabled, false otherwise
     */
    boolean isStartupTimingEnabled();

    CircuitBreakerConfig getCircuitBreakerConfig();

    StripExceptionConfig getStripExceptionConfig();

    TransactionNamingScheme getTransactionNamingScheme();

    UtilizationDataConfig getUtilizationDataConfig();

    DatastoreConfig getDatastoreConfig();

    ExternalTracerConfig getExternalTracerConfig();

    boolean liteMode();

    boolean legacyAsyncApiSkipSuspend();

    int getSegmentTimeoutInSec();

    int getTokenTimeoutInSec();

    boolean openTracingEnabled();

    /**
     * How long to wait (in milliseconds) for all transactions to finish before allowing the application to shutdown
     */
    int waitForTransactionsInMillis();

    boolean laspEnabled();

    String securityPoliciesToken();

    boolean isCustomInstrumentationEditorAllowed();

    boolean isCustomParametersAllowed();

    TransactionEventsConfig getTransactionEventsConfig();

    DistributedTracingConfig getDistributedTracingConfig();

    ExtensionsConfig getExtensionsConfig();

    SpanEventsConfig getSpanEventsConfig();

    List<String> logDeprecatedProperties(Map<String, Object> localSettings);

    CommandParserConfig getCommandParserConfig();

    InfiniteTracingConfig getInfiniteTracingConfig();

    SlowTransactionsConfig getSlowTransactionsConfig();

}
