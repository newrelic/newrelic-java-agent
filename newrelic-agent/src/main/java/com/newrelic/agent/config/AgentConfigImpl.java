/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.base.Joiner;
import com.newrelic.agent.Agent;
import com.newrelic.agent.autoname.ApplicationAutoName;
import com.newrelic.agent.transaction.TransactionNamingScheme;
import com.newrelic.agent.transport.DataSenderImpl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class AgentConfigImpl extends BaseConfig implements AgentConfig {

    // root configs (alphabetized)
    public static final String AGENT_ENABLED = "agent_enabled";
    public static final String APDEX_T = "apdex_t";
    public static final String API_HOST = "api_host";
    public static final String API_PORT = "api_port";
    public static final String APP_NAME = "app_name";
    public static final String ASYNC_TIMEOUT = "async_timeout";
    public static final String CA_BUNDLE_PATH = "ca_bundle_path";
    public static final String COMPRESSED_CONTENT_ENCODING_PROPERTY = "compressed_content_encoding";
    public static final String CPU_SAMPLING_ENABLED = "cpu_sampling_enabled";
    public static final String ENABLED = "enabled";
    public static final String ENABLE_AUTO_APP_NAMING = "enable_auto_app_naming";
    public static final String ENABLE_AUTO_TRANSACTION_NAMING = "enable_auto_transaction_naming";
    public static final String ENABLE_BOOTSTRAP_CLASS_INSTRUMENTATION = "enable_bootstrap_class_instrumentation";
    public static final String ENABLE_CLASS_RETRANSFORMATION = "enable_class_retransformation";
    public static final String ENABLE_CUSTOM_TRACING = "enable_custom_tracing";
    public static final String EXT_CONFIG_DIR = "extensions.dir";
    public static final String HIGH_SECURITY = "high_security";
    public static final String HOST = "host";
    public static final String IBM_WORKAROUND = "ibm_iv25688_workaround";
    public static final String IGNORE_JARS = "ignore_jars";
    public static final String INSERT_API_KEY = "insert_api_key";
    public static final String JDBC_SUPPORT = "jdbc_support";
    public static final String LABELS = "labels";
    public static final String LANGUAGE = "language";
    public static final String LICENSE_KEY = "license_key";
    public static final String LITE_MODE = "lite_mode";
    public static final String LOG_DAILY = "log_daily";
    public static final String LOG_FILE_COUNT = "log_file_count";
    public static final String LOG_FILE_NAME = "log_file_name";
    public static final String LOG_FILE_PATH = "log_file_path";
    public static final String LOG_LEVEL = "log_level";
    public static final String LOG_LIMIT = "log_limit_in_kbytes";
    public static final String MAX_STACK_TRACE_LINES = "max_stack_trace_lines";
    public static final String METRIC_INGEST_URI = "metric_ingest_uri";
    public static final String DEBUG = "newrelic.debug";
    public static final String PLATFORM_INFORMATION_ENABLED = "platform_information_enabled";
    public static final String PORT = "port";
    public static final String PROXY_HOST = "proxy_host";
    public static final String PROXY_PASS = "proxy_password";
    public static final String PROXY_PORT = "proxy_port";
    public static final String PROXY_SCHEME = "proxy_scheme";
    public static final String PROXY_USER = "proxy_user";
    public static final String PUT_FOR_DATA_SEND_PROPERTY = "put_for_data_send";
    public static final String REPORT_SQL_PARSER_ERRORS = "report_sql_parser_errors";
    public static final String LASP_TOKEN = "security_policies_token";
    public static final String SEND_DATA_ON_EXIT = "send_data_on_exit";
    public static final String SEND_DATA_ON_EXIT_THRESHOLD = "send_data_on_exit_threshold";
    public static final String SEND_ENVIRONMENT_INFO = "send_environment_info";
    public static final String SEND_JVM_PROPS = "send_jvm_props";
    public static final String SIMPLE_COMPRESSION_PROPERTY = "simple_compression";
    public static final String IS_SSL = "ssl";
    private static final String REQUEST_TIMEOUT_IN_SECONDS_PROPERTY = "timeout";
    public static final String STARTUP_LOG_LEVEL = "startup_log_level";
    public static final String STARTUP_TIMING = "startup_timing";
    public static final String STDOUT = "STDOUT";
    public static final String SYNC_STARTUP = "sync_startup";
    public static final String THREAD_CPU_TIME_ENABLED = "thread_cpu_time_enabled";
    public static final String TRACE_DATA_CALLS = "trace_data_calls";
    public static final String TRANSACTION_NAMING_SCHEME = "transaction_naming_scheme";
    public static final String TRANSACTION_SIZE_LIMIT = "transaction_size_limit";
    public static final String TRIM_STATS = "trim_stats";
    public static final String USE_PRIVATE_SSL = "use_private_ssl";
    public static final String WAIT_FOR_RPM_CONNECT = "wait_for_rpm_connect";
    public static final String WAIT_FOR_TRANSACTIONS = "wait_for_transactions";
    public static final String KEY_TRANSACTIONS = "web_transactions_apdex";

    // nested configs (alphabetized)
    public static final String ATTRIBUTES = "attributes";
    public static final String BROWSER_MONITORING = "browser_monitoring";
    public static final String CLASS_TRANSFORMER = "class_transformer";
    public static final String CROSS_APPLICATION_TRACER = "cross_application_tracer";
    public static final String CUSTOM_INSIGHT_EVENTS = "custom_insights_events";
    public static final String DATASTORE_TRACER = "datastore_tracer";
    public static final String DISTRIBUTED_TRACING = "distributed_tracing";
    public static final String ERROR_COLLECTOR = "error_collector";
    public static final String EXTENSIONS = "extensions";
    public static final String INSTRUMENTATION = "instrumentation";
    public static final String JAR_COLLECTOR = "jar_collector";
    public static final String JMX = "jmx";
    public static final String OPEN_TRACING = "open_tracing";
    public static final String REINSTRUMENT = "reinstrument";
    public static final String SLOW_SQL = "slow_sql";
    public static final String SPAN_EVENTS = "span_events";
    public static final String STRIP_EXCEPTION_MESSAGES = "strip_exception_messages";
    public static final String THREAD_PROFILER = "thread_profiler";
    public static final String TRANSACTION_EVENTS = "transaction_events"; // replaces analytics_events
    public static final String TRANSACTION_SEGMENTS = "transaction_segments";
    public static final String TRANSACTION_TRACER = "transaction_tracer";

    // defaults (alphabetized)
    public static final double DEFAULT_APDEX_T = 1.0; // 1 second
    public static final String DEFAULT_API_HOST = "rpm.newrelic.com";
    public static final String DEFAULT_CA_BUNDLE_PATH = null;
    public static final String DEFAULT_COMPRESSED_CONTENT_ENCODING = DataSenderImpl.GZIP_ENCODING;
    public static final boolean DEFAULT_CPU_SAMPLING_ENABLED = true;
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_ENABLE_AUTO_APP_NAMING = false;
    public static final boolean DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING = true;
    public static final boolean DEFAULT_ENABLE_CUSTOM_TRACING = true;
    public static final boolean DEFAULT_HIGH_SECURITY = false;
    /*
     * If a customer wants to add a . to the end of their collector hostname to avoid one DNS lookup they can configure
     * host in newrelic.yml. This value makes the default behavior always work.
     */
    public static final String DEFAULT_HOST = "collector.newrelic.com";
    public static final boolean DEFAULT_IBM_WORKAROUND = IBMUtils.getIbmWorkaroundDefault();
    public static final String DEFAULT_INSERT_API_KEY = "";
    public static final boolean DEFAULT_IS_SSL = true;
    // jdbc support
    public static final String GENERIC_JDBC_SUPPORT = "generic";
    public static final String DEFAULT_JDBC_SUPPORT = GENERIC_JDBC_SUPPORT;
    public static final String DEFAULT_LANGUAGE = "java";
    public static final boolean DEFAULT_LOG_DAILY = false;
    public static final int DEFAULT_LOG_FILE_COUNT = 1;
    public static final String DEFAULT_LOG_FILE_NAME = "newrelic_agent.log";
    public static final String DEFAULT_LOG_LEVEL = "info";
    public static final int DEFAULT_LOG_LIMIT = 0;
    public static final int DEFAULT_MAX_STACK_TRACE_LINES = 30;
    public static final String DEFAULT_METRIC_INGEST_URI = "https://metric-api.newrelic.com";
    public static final boolean DEFAULT_PLATFORM_INFORMATION_ENABLED = true;
    public static final int DEFAULT_PORT = 80;
    public static final String DEFAULT_PROXY_HOST = null;
    public static final int DEFAULT_PROXY_PORT = 8080;
    public static final String DEFAULT_PROXY_SCHEME = null;
    public static final boolean DEFAULT_PUT_FOR_DATA_SEND_ENABLED = false;
    public static final String DEFAULT_SECURITY_POLICIES_TOKEN = "";
    public static final boolean DEFAULT_SEND_DATA_ON_EXIT = false;
    public static final int DEFAULT_SEND_DATA_ON_EXIT_THRESHOLD = 60;
    public static final boolean DEFAULT_SEND_ENVIRONMENT_INFO = true;
    public static final boolean DEFAULT_SIMPLE_COMPRESSION_ENABLED = false;
    public static final int DEFAULT_SSL_PORT = 443;
    public static final boolean DEFAULT_STARTUP_TIMING = true;
    public static final boolean DEFAULT_SYNC_STARTUP = false;
    public static final boolean DEFAULT_TRACE_DATA_CALLS = false;
    public static final int DEFAULT_TRANSACTION_SIZE_LIMIT = 2000;
    public static final boolean DEFAULT_TRIM_STATS = true;
    public static final boolean DEFAULT_WAIT_FOR_RPM_CONNECT = true;
    public static final int DEFAULT_WAIT_FOR_TRANSACTIONS = 0;
    private static final int DEFAULT_REQUEST_TIMEOUT_IN_SECONDS = 120;

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.";

    // per protocol 15+, region aware license keys must match this regex before constructing collector host
    public static final Pattern REGION_AWARE = Pattern.compile("^.+?x");

    // root configs (alphabetized)
    private final long apdexTInMillis;
    private final String appName;
    private final List<String> appNames;
    private final boolean autoAppNamingEnabled;
    private final boolean autoTransactionNamingEnabled;
    private final String caBundlePath;
    private final String compressedContentEncoding;
    private final boolean cpuSamplingEnabled;
    private final boolean customInstrumentationEditorAllowed;
    private final boolean customParameters;
    private final boolean debug;
    private final boolean enabled;
    private final boolean genericJdbcSupportEnabled;
    private final boolean highSecurity;
    private final String host;
    private final boolean ibmWorkaroundEnabled;
    private final List<String> ignoreJars;
    private final String insertApiKey;
    private final boolean isApdexTSet;
    private final boolean isSSL;
    private final HashSet<String> jdbcSupport;
    private final String licenseKey;
    private final boolean litemode;
    private final boolean logDaily;
    private final String logLevel;
    private final int maxStackTraceLines;
    private final String metricIngestUri;
    private final boolean platformInformationEnabled;
    private final int port;
    private final String proxyHost;
    private final String proxyPass;
    private final Integer proxyPort;
    private final String proxyScheme;
    private final String proxyUser;
    private final boolean putForDataSend;
    private final int segmentTimeoutInSec;
    private final String securityPoliciesToken;
    private final boolean sendJvmProps;
    private final boolean simpleCompression;
    private final boolean startupTimingEnabled;
    private final int tokenTimeoutInSec;
    private final TransactionNamingScheme transactionNamingMode;
    private final int transactionSizeLimit;
    private final boolean trimStats;
    private final boolean waitForRPMConnect;
    private final int waitForTransactionsInMillis;
    private final int requestTimeoutInMillis;

    // nested configs (alphabetized)
    private final AttributesConfig attributesConfig;
    private final AuditModeConfig auditModeConfig;
    private final TransactionTracerConfigImpl backgroundTransactionTracerConfig;
    private final BrowserMonitoringConfig browserMonitoringConfig;
    private final ClassTransformerConfig classTransformerConfig;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final CrossProcessConfig crossProcessConfig;
    private final DatastoreConfig datastoreConfig;
    private final DistributedTracingConfig distributedTracingConfig;
    private final ErrorCollectorConfig errorCollectorConfig;
    private final ExtensionsConfig extensionsConfig;
    private final ExternalTracerConfig externalTracerConfig;
    private final InfiniteTracingConfig infiniteTracingConfig;
    private final InsightsConfig insightsConfig;
    private final Config instrumentationConfig;
    private final JarCollectorConfig jarCollectorConfig;
    private final JmxConfig jmxConfig;
    private final KeyTransactionConfig keyTransactionConfig;
    private final LabelsConfig labelsConfig;
    private final NormalizationRuleConfig normalizationRuleConfig;
    private final OpenTracingConfig openTracingConfig;
    private final ReinstrumentConfig reinstrumentConfig;
    private final TransactionTracerConfigImpl requestTransactionTracerConfig;
    private final SpanEventsConfig spanEventsConfig;
    private final SqlTraceConfig sqlTraceConfig;
    private final StripExceptionConfig stripExceptionConfig;
    private final ThreadProfilerConfig threadProfilerConfig;
    private final TransactionEventsConfig transactionEventsConfig;
    private final TransactionTracerConfigImpl transactionTracerConfig;
    private final UtilizationDataConfig utilizationConfig;

    private final Map<String, Object> flattenedProperties;
    private final CommandParserConfig commandParserConfig;
    private final ApplicationAutoName applicationAutoName;

    public static AgentConfig createAgentConfig(Map<String, Object> settings) {
        return createAgentConfig(settings, EnvironmentFacade.getInstance());
    }

    static AgentConfig createAgentConfig(Map<String, Object> settings, EnvironmentFacade environmentFacade) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new AgentConfigImpl(settings, environmentFacade);
    }

    private AgentConfigImpl(Map<String, Object> props, EnvironmentFacade environmentFacade) {
        super(props, SYSTEM_PROPERTY_ROOT);
        // ssl, transaction_tracer.record_sql, request atts, and message atts are all affected by high security
        highSecurity = getProperty(HIGH_SECURITY, DEFAULT_HIGH_SECURITY);
        securityPoliciesToken = getProperty(LASP_TOKEN, DEFAULT_SECURITY_POLICIES_TOKEN);
        simpleCompression = getProperty(SIMPLE_COMPRESSION_PROPERTY, DEFAULT_SIMPLE_COMPRESSION_ENABLED);
        compressedContentEncoding = initCompressedContentEncoding();
        putForDataSend = getProperty(PUT_FOR_DATA_SEND_PROPERTY, DEFAULT_PUT_FOR_DATA_SEND_ENABLED);
        isApdexTSet = getProperty(APDEX_T) != null;
        apdexTInMillis = (long) (getDoubleProperty(APDEX_T, DEFAULT_APDEX_T) * 1000L);
        debug = Boolean.getBoolean(DEBUG);
        enabled = getProperty(ENABLED, DEFAULT_ENABLED) && getProperty(AGENT_ENABLED, DEFAULT_ENABLED);
        licenseKey = getProperty(LICENSE_KEY);
        host = parseHost(licenseKey);
        isSSL = initSsl(props);
        ignoreJars = new ArrayList<>(getUniqueStrings(IGNORE_JARS, COMMA_SEPARATOR));
        insertApiKey = getProperty(INSERT_API_KEY, DEFAULT_INSERT_API_KEY);
        logLevel = initLogLevel();
        logDaily = getProperty(LOG_DAILY, DEFAULT_LOG_DAILY);
        port = getIntProperty(PORT, isSSL ? DEFAULT_SSL_PORT : DEFAULT_PORT);
        proxyHost = getProperty(PROXY_HOST, DEFAULT_PROXY_HOST);
        proxyPort = getIntProperty(PROXY_PORT, DEFAULT_PROXY_PORT);
        proxyScheme = getProperty(PROXY_SCHEME, DEFAULT_PROXY_SCHEME);
        proxyUser = getStringPropertyOrNull(PROXY_USER);
        proxyPass = getStringPropertyOrNull(PROXY_PASS);
        applicationAutoName = ApplicationAutoName.getApplicationAutoName(environmentFacade);
        appNames = getAppNames(environmentFacade);
        appName = appNames.isEmpty() ? null : appNames.get(0);
        cpuSamplingEnabled = getProperty(CPU_SAMPLING_ENABLED, DEFAULT_CPU_SAMPLING_ENABLED);
        autoAppNamingEnabled = applicationAutoName.enableAutoAppNaming() ||
                getProperty(ENABLE_AUTO_APP_NAMING, DEFAULT_ENABLE_AUTO_APP_NAMING);
        autoTransactionNamingEnabled = getProperty(ENABLE_AUTO_TRANSACTION_NAMING, DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING);
        transactionSizeLimit = getIntProperty(TRANSACTION_SIZE_LIMIT, DEFAULT_TRANSACTION_SIZE_LIMIT) * 1024;
        waitForRPMConnect = getProperty(WAIT_FOR_RPM_CONNECT, DEFAULT_WAIT_FOR_RPM_CONNECT);
        startupTimingEnabled = getProperty(STARTUP_TIMING, DEFAULT_STARTUP_TIMING);
        sendJvmProps = getProperty(SEND_JVM_PROPS, true);
        litemode = getProperty(LITE_MODE, false);
        caBundlePath = initSSLConfig();
        trimStats = getProperty(TRIM_STATS, DEFAULT_TRIM_STATS);
        platformInformationEnabled = getProperty(PLATFORM_INFORMATION_ENABLED, DEFAULT_PLATFORM_INFORMATION_ENABLED);
        ibmWorkaroundEnabled = getProperty(IBM_WORKAROUND, DEFAULT_IBM_WORKAROUND);
        transactionNamingMode = parseTransactionNamingMode();
        maxStackTraceLines = getProperty(MAX_STACK_TRACE_LINES, DEFAULT_MAX_STACK_TRACE_LINES);
        metricIngestUri = getProperty(METRIC_INGEST_URI, DEFAULT_METRIC_INGEST_URI);
        String[] jdbcSupport = getProperty(JDBC_SUPPORT, DEFAULT_JDBC_SUPPORT).split(",");
        this.jdbcSupport = new HashSet<>(Arrays.asList(jdbcSupport));
        genericJdbcSupportEnabled = this.jdbcSupport.contains(GENERIC_JDBC_SUPPORT);
        requestTimeoutInMillis = getProperty(REQUEST_TIMEOUT_IN_SECONDS_PROPERTY, DEFAULT_REQUEST_TIMEOUT_IN_SECONDS) * 1000;
        instrumentationConfig = new BaseConfig(nestedProps(INSTRUMENTATION), SYSTEM_PROPERTY_ROOT + INSTRUMENTATION);
        transactionTracerConfig = initTransactionTracerConfig(apdexTInMillis, highSecurity);
        requestTransactionTracerConfig = transactionTracerConfig.createRequestTransactionTracerConfig(apdexTInMillis, highSecurity);
        backgroundTransactionTracerConfig = transactionTracerConfig.createBackgroundTransactionTracerConfig(apdexTInMillis, highSecurity);
        errorCollectorConfig = initErrorCollectorConfig();
        extensionsConfig = initExtensionsConfig();
        threadProfilerConfig = initThreadProfilerConfig();
        keyTransactionConfig = initKeyTransactionConfig(apdexTInMillis);
        sqlTraceConfig = initSqlTraceConfig();
        auditModeConfig = initAuditModeConfig();
        browserMonitoringConfig = initBrowserMonitoringConfig();
        classTransformerConfig = initClassTransformerConfig(litemode);
        crossProcessConfig = initCrossProcessConfig();
        stripExceptionConfig = initStripExceptionConfig(highSecurity);
        labelsConfig = new LabelsConfigImpl(getProperty(LABELS));
        utilizationConfig = initUtilizationConfig();
        datastoreConfig = initDatastoreConfig();
        externalTracerConfig = initExternalTracerConfig();
        jmxConfig = initJmxConfig();
        jarCollectorConfig = initJarCollectorConfig();
        insightsConfig = initInsightsConfig();
        infiniteTracingConfig = initInfiniteTracingConfig(autoAppNamingEnabled);
        attributesConfig = initAttributesConfig();
        reinstrumentConfig = initReinstrumentConfig();
        circuitBreakerConfig = initCircuitBreakerConfig();
        segmentTimeoutInSec = initSegmentTimeout();
        tokenTimeoutInSec = initTokenTimeout();
        openTracingConfig = initOpenTracingConfig();
        distributedTracingConfig = initDistributedTracing();
        spanEventsConfig = initSpanEventsConfig(distributedTracingConfig.isEnabled());
        transactionEventsConfig = initTransactionEvents();
        commandParserConfig = initCommandParserConfig();
        normalizationRuleConfig = new NormalizationRuleConfig(props);

        Map<String, Object> flattenedProps = new HashMap<>();
        flatten("", props, flattenedProps);
        Map<String, Object> propsWithSystemProps = new HashMap<>();
        propsWithSystemProps.putAll(SystemPropertyFactory.getSystemPropertyProvider().getNewRelicPropertiesWithoutPrefix());
        propsWithSystemProps.putAll(SystemPropertyFactory.getSystemPropertyProvider().getNewRelicEnvVarsWithoutPrefix());
        flatten("", propsWithSystemProps, flattenedProps);
        checkHighSecurityPropsInFlattened(flattenedProps);
        this.flattenedProperties = Collections.unmodifiableMap(flattenedProps);
        this.waitForTransactionsInMillis = getProperty(WAIT_FOR_TRANSACTIONS, DEFAULT_WAIT_FOR_TRANSACTIONS);
        this.customInstrumentationEditorAllowed = getProperty(LaspPolicies.LASP_CUSTOM_INSTRUMENTATION_EDITOR, !highSecurity);
        this.customParameters = getProperty(LaspPolicies.LASP_CUSTOM_PARAMETERS, !highSecurity);

        if (getProperty(REPORT_SQL_PARSER_ERRORS) != null) {
            addDeprecatedProperty(new String[] { REPORT_SQL_PARSER_ERRORS }, null);
        }
    }

    private String initSSLConfig() {
        String caBundlePath = getProperty(CA_BUNDLE_PATH, DEFAULT_CA_BUNDLE_PATH);
        if ( getProperty(USE_PRIVATE_SSL) != null) {
            if ( caBundlePath != null) {
                Agent.LOG.log(Level.INFO, "use_private_ssl configuration setting has been removed.");
            } else {
                Agent.LOG.log(Level.SEVERE, "use_private_ssl configuration setting has been removed. Please use ca_bundle_path instead.");
            }
        }
        return caBundlePath;
    }

    /**
     * The license key is hex encoded, so if it contains an 'x' then it must be protocol 15+, which means the first
     * 6 characters are the region. Format is: [A-Z]{2,3}[0-9]{2} with 'x' padding until it's 6 characters long.
     * The spec only requires it to pass the regex, and not to check length requirements, to maintain flexibility.
     */
    private String parseRegion(String licenseKey) {
        if (licenseKey != null) {
            licenseKey = licenseKey.toLowerCase();
            if (REGION_AWARE.matcher(licenseKey).find()) {
                return licenseKey.substring(0, licenseKey.indexOf("x")); // don't include the x's
            }
        }
        return "";
    }

    /**
     * If host was set explicitly, then always use it and don't construct the collector host from the license key.
     * If the license key doesn't conform to protocol 15+, then return the default host, otherwise construct the new
     * host using the region section of the license key.
     */
    private String parseHost(String licenseKey) {
        String host = getProperty(HOST);
        if (host != null) {
            Agent.LOG.log(Level.INFO, "Using configured collector host: {0}", host);
            return host;
        }

        String region = parseRegion(licenseKey);
        if (region.isEmpty()) {
            Agent.LOG.log(Level.INFO, "Using default collector host: {0}", DEFAULT_HOST);
            return DEFAULT_HOST;
        }

        host = "collector." + region + ".nr-data.net";
        Agent.LOG.log(Level.INFO, "Using region aware collector host: {0}", host);

        return host;
    }

    private OpenTracingConfig initOpenTracingConfig() {
        Map<String, Object> openTracing = nestedProps(OPEN_TRACING);
        return new OpenTracingConfig(openTracing);
    }

    private DistributedTracingConfig initDistributedTracing() {
        Map<String, Object> distributedTracing = nestedProps(DISTRIBUTED_TRACING);
        return new DistributedTracingConfig(distributedTracing);
    }

    private SpanEventsConfig initSpanEventsConfig(boolean dtEnabled) {
        Map<String, Object> spanEvents = nestedProps(SPAN_EVENTS);
        return new SpanEventsConfig(spanEvents, dtEnabled);
    }

    private int initTokenTimeout() {
        if (getProperty(ASYNC_TIMEOUT) != null) {
            Agent.LOG.log(Level.INFO, "The property async_timeout is deprecated. Change to token_timeout");
            return getProperty("async_timeout", 180);
        }

        return getProperty("token_timeout", 180);
    }

    private int initSegmentTimeout() {
        // Segment API was previously known as TracedActivity.
        if (getProperty("traced_activity_timeout") != null) {
            Agent.LOG.log(Level.INFO, "The property traced_activity_timeout is deprecated. Change to segment_timeout");
            return getProperty("traced_activity_timeout", 10 * 60);
        }

        return getProperty("segment_timeout", 10 * 60);
    }

    private TransactionNamingScheme parseTransactionNamingMode() {
        TransactionNamingScheme mode = TransactionNamingScheme.LEGACY;

        String mode_name = getProperty(TRANSACTION_NAMING_SCHEME, "legacy");

        if (mode_name.equals("resource_based")) {
            mode = TransactionNamingScheme.RESOURCE_BASED;
        }

        return mode;
    }

    /*
     * This is here just in case someone uses get value to retrieve one of the properties which has been changed do to
     * high security.
     */
    private void checkHighSecurityPropsInFlattened(Map<String, Object> flattenedProps) {
        if (highSecurity && !flattenedProps.isEmpty()) {
            flattenedProps.put(AgentConfigImpl.IS_SSL, isSSL);
            flattenedProps.put("transaction_tracer.record_sql", transactionTracerConfig.getRecordSql());
        }
    }

    /*
     * The agent must always connect using SSL.
     */
    private boolean initSsl(Map<String, Object> props) {
        if (!getProperty(IS_SSL, DEFAULT_IS_SSL)) { // ssl: false is configured in yml
            Agent.LOG.log(Level.INFO, "Agent versions 4.0.0+ must connect with SSL. Agent is ignoring yml config and enabling SSL.");
            props.put(IS_SSL, DEFAULT_IS_SSL); // default is ssl: true
        }
        return DEFAULT_IS_SSL;
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> source, Map<String, Object> dest) {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            if (e.getValue() instanceof Map) {
                flatten(prefix + e.getKey() + '.', (Map<String, Object>) e.getValue(), dest);
            } else {
                Object destinationValue = dest.get(prefix + e.getKey());
                if (!(destinationValue instanceof ServerProp)) {
                    dest.put(prefix + e.getKey(), e.getValue());
                }
            }
        }
    }

    @Override
    public <T> T getValue(String path) {
        return getValue(path, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(String path, T defaultValue) {
        Object value = flattenedProperties.get(path.replaceAll("[.-]", "_"));
        if (value == null) {
            value = flattenedProperties.get(path);
        }

        if (value == null) {
            return defaultValue;
        } else if (value instanceof ServerProp) {
            value = ((ServerProp) value).getValue();
            return castValue(path, value, defaultValue);
        } else if (value instanceof String && defaultValue instanceof Boolean) {
            // As noted below, the YAML parser interprets "on" as true and "off" as false.
            // It's unclear whether we should accept the strings "on" and "off" here.
            value = Boolean.valueOf((String) value);
            return (T) value;
        } else if (value instanceof String && defaultValue instanceof Integer) {
            value = Integer.valueOf((String) value);
            return (T) value;
        } else {
            try {
                return (T) value;
            } catch (ClassCastException ccx) {
                Agent.LOG.log(Level.FINE, "Using default value \"{0}\" for \"{1}\"", defaultValue, path);
                return defaultValue;
            }
        }
    }

    // even though getProperty returns `T` (here a String), that's a compile-time check.
    // Java generics at runtime allow any return value since it's only assigned to an Object.
    @SuppressWarnings("ConstantConditions")
    private String initLogLevel() {
        Object val = getProperty(LOG_LEVEL, DEFAULT_LOG_LEVEL);
        if (val instanceof Boolean) {
            // the YAML parser interprets "on" as true and "off" as false
            return "off";
        }
        return getProperty(LOG_LEVEL, DEFAULT_LOG_LEVEL).toLowerCase();
    }

    private List<String> getAppNames(EnvironmentFacade environmentFacade) {
        final List<String> appNames = getUniqueStrings(APP_NAME, SEMI_COLON_SEPARATOR);
        if (appNames.isEmpty()) {
            String appServerAppName = applicationAutoName.getName(environmentFacade);
            if (appServerAppName != null) {
                return Arrays.asList(appServerAppName);
            }
        }
        return appNames;
    }

    private CrossProcessConfig initCrossProcessConfig() {
        Boolean prop = getProperty(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING);
        Map<String, Object> props = nestedProps(CROSS_APPLICATION_TRACER);
        if (prop != null) {
            if (props == null) {
                props = createMap();
            }
            props.put(CrossProcessConfigImpl.CROSS_APPLICATION_TRACING, prop);
        }
        return CrossProcessConfigImpl.createCrossProcessConfig(props);
    }

    private StripExceptionConfig initStripExceptionConfig(boolean highSecurity) {
        Map<String, Object> props = nestedProps(STRIP_EXCEPTION_MESSAGES);
        return StripExceptionConfigImpl.createStripExceptionConfig(props, highSecurity);
    }

    private ThreadProfilerConfig initThreadProfilerConfig() {
        Map<String, Object> props = nestedProps(THREAD_PROFILER);
        return ThreadProfilerConfigImpl.createThreadProfilerConfig(props);
    }

    private KeyTransactionConfig initKeyTransactionConfig(long apdexTInMillis) {
        Map<String, Object> props = nestedProps(KEY_TRANSACTIONS);
        return KeyTransactionConfigImpl.createKeyTransactionConfig(props, apdexTInMillis);
    }

    private TransactionTracerConfigImpl initTransactionTracerConfig(long apdexTInMillis, boolean highSecurity) {
        Map<String, Object> props = nestedProps(TRANSACTION_TRACER);
        return TransactionTracerConfigImpl.createTransactionTracerConfig(props, apdexTInMillis, highSecurity);
    }

    private ErrorCollectorConfig initErrorCollectorConfig() {
        Map<String, Object> props = nestedProps(ERROR_COLLECTOR);
        return ErrorCollectorConfigImpl.createErrorCollectorConfig(props);
    }

    private ExtensionsConfig initExtensionsConfig() {
        Map<String, Object> props = nestedProps(EXTENSIONS);
        return ExtensionsConfigImpl.createExtensionsConfig(props);
    }

    private SqlTraceConfig initSqlTraceConfig() {
        Map<String, Object> props = nestedProps(SLOW_SQL);
        SqlTraceConfig sqlTraceConfig = SqlTraceConfigImpl.createSqlTraceConfig(props);
        if (sqlTraceConfig.isUsingLongerSqlId()) {
            Agent.LOG.info("Agent is configured to use longer sql id for sql traces");
        }
        return sqlTraceConfig;
    }

    private JmxConfig initJmxConfig() {
        Map<String, Object> props = nestedProps(JMX);
        return JmxConfigImpl.createJmxConfig(props);
    }

    private JarCollectorConfig initJarCollectorConfig() {
        Map<String, Object> props = nestedProps(JAR_COLLECTOR);
        return JarCollectorConfigImpl.createJarCollectorConfig(props);
    }

    private InsightsConfig initInsightsConfig() {
        Map<String, Object> props = nestedProps(CUSTOM_INSIGHT_EVENTS);
        return InsightsConfigImpl.createInsightsConfig(props, highSecurity);
    }

    private AttributesConfig initAttributesConfig() {
        Map<String, Object> props = nestedProps(ATTRIBUTES);
        return AttributesConfigImpl.createAttributesConfig(props);
    }

    private ReinstrumentConfig initReinstrumentConfig() {
        Map<String, Object> props = nestedProps(REINSTRUMENT);
        return ReinstrumentConfigImpl.createReinstrumentConfig(props);
    }

    private AuditModeConfig initAuditModeConfig() {
        Object auditMode = getProperty(AuditModeConfig.PROPERTY_NAME);

        if (auditMode instanceof Map) {
            // New, nested "audit_mode:" property
            Map<String, Object> props = nestedProps(AuditModeConfig.PROPERTY_NAME);
            return new AuditModeConfig(props);
        } else {
            // This is the "legacy" case where "audit_mode: true" is it's own single-level config property
            boolean auditModeEnabled = getProperty(AuditModeConfig.PROPERTY_NAME, AuditModeConfig.DEFAULT_ENABLED);
            boolean traceDataCalls = getProperty(TRACE_DATA_CALLS, DEFAULT_TRACE_DATA_CALLS);
            return new AuditModeConfig(auditModeEnabled, traceDataCalls);
        }
    }

    private BrowserMonitoringConfig initBrowserMonitoringConfig() {
        Map<String, Object> props = nestedProps(BROWSER_MONITORING);
        return BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(props);
    }

    private ClassTransformerConfig initClassTransformerConfig(boolean liteMode) {
        boolean customTracingEnabled = getProperty(ENABLE_CUSTOM_TRACING, DEFAULT_ENABLE_CUSTOM_TRACING);
        Map<String, Object> props = nestedProps(CLASS_TRANSFORMER);
        return ClassTransformerConfigImpl.createClassTransformerConfig(props, customTracingEnabled, liteMode);
    }

    private CircuitBreakerConfig initCircuitBreakerConfig() {
        Map<String, Object> props = nestedProps(CircuitBreakerConfig.PROPERTY_NAME);
        return new CircuitBreakerConfig(props);
    }

    private UtilizationDataConfig initUtilizationConfig() {
        Map<String, Object> props = nestedProps(UtilizationDataConfig.PROPERTY_NAME);
        return new UtilizationDataConfig(props);
    }

    private DatastoreConfig initDatastoreConfig() {
        Map<String, Object> props = nestedProps(DatastoreConfigImpl.PROPERTY_NAME);
        return new DatastoreConfigImpl(props);
    }

    private ExternalTracerConfig initExternalTracerConfig() {
        Map<String, Object> props = nestedProps(ExternalTracerConfigImpl.PROPERTY_NAME);
        return new ExternalTracerConfigImpl(props);
    }

    private InfiniteTracingConfig initInfiniteTracingConfig(boolean autoAppNamingEnabled) {
        Map<String, Object> props = nestedProps(InfiniteTracingConfigImpl.ROOT);
        return new InfiniteTracingConfigImpl(props, autoAppNamingEnabled);
    }

    private TransactionEventsConfig initTransactionEvents() {
        Map<String, Object> transactionEvents = nestedProps(TRANSACTION_EVENTS);
        return new TransactionEventsConfig(transactionEvents);
    }

    private String initCompressedContentEncoding() {
        // The only available configuration options are gzip and deflate
        if (DataSenderImpl.DEFLATE_ENCODING.equals(getProperty(COMPRESSED_CONTENT_ENCODING_PROPERTY))) {
            return DataSenderImpl.DEFLATE_ENCODING;
        }
        return DEFAULT_COMPRESSED_CONTENT_ENCODING;
    }

    private CommandParserConfig initCommandParserConfig() {
        return new CommandParserConfigImpl(nestedProps(CommandParserConfigImpl.ROOT));
    }

    @Override
    public long getApdexTInMillis() {
        return apdexTInMillis;
    }

    @Override
    public long getApdexTInMillis(String transactionName) {
        return keyTransactionConfig.getApdexTInMillis(transactionName);
    }

    @Override
    public boolean isApdexTSet() {
        return isApdexTSet;
    }

    @Override
    public boolean isApdexTSet(String transactionName) {
        return keyTransactionConfig.isApdexTSet(transactionName);
    }

    @Override
    public boolean isAgentEnabled() {
        return enabled;
    }

    @Override
    public String getLicenseKey() {
        return licenseKey;
    }

    @Override
    public int getTimeoutInMilliseconds() {
        return requestTimeoutInMillis;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getProxyHost() {
        return proxyHost;
    }

    @Override
    public Integer getProxyPort() {
        return proxyPort;
    }

    @Override
    public String getProxyScheme() {
        return proxyScheme;
    }

    @Override
    public String getProxyUser() {
        return proxyUser;
    }

    @Override
    public String getProxyPassword() {
        return proxyPass;
    }

    @Override
    public String getApiHost() {
        return getProperty(API_HOST, DEFAULT_API_HOST);
    }

    @Override
    public int getApiPort() {
        return getProperty(API_PORT, isSSL ? DEFAULT_SSL_PORT : DEFAULT_PORT);
    }

    @Override
    public boolean isSSL() {
        return isSSL;
    }

    @Override
    public String getInsertApiKey() {
        return insertApiKey;
    }

    @Override
    public String getApplicationName() {
        return appName;
    }

    @Override
    public List<String> getApplicationNames() {
        return appNames;
    }

    @Override
    public boolean isCpuSamplingEnabled() {
        return cpuSamplingEnabled;
    }

    @Override
    public boolean isAutoAppNamingEnabled() {
        return autoAppNamingEnabled;
    }

    @Override
    public boolean isAutoTransactionNamingEnabled() {
        return autoTransactionNamingEnabled;
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    @Override
    public boolean isDebugEnabled(String key) {
        return getProperty(key + "_debug", false);
    }

    @Override
    public String getLanguage() {
        return getProperty(LANGUAGE, DEFAULT_LANGUAGE);
    }

    @Override
    public boolean isSendDataOnExit() {
        return getProperty(SEND_DATA_ON_EXIT, DEFAULT_SEND_DATA_ON_EXIT);
    }

    @Override
    public long getSendDataOnExitThresholdInMillis() {
        int valueInSecs = getIntProperty(SEND_DATA_ON_EXIT_THRESHOLD, DEFAULT_SEND_DATA_ON_EXIT_THRESHOLD);
        return TimeUnit.MILLISECONDS.convert(valueInSecs, TimeUnit.SECONDS);
    }

    @Override
    public boolean isAuditMode() {
        return auditModeConfig.isEnabled();
    }

    @Override
    public AuditModeConfig getAuditModeConfig() {
        return auditModeConfig;
    }

    @Override
    public boolean liteMode() {
        return litemode;
    }

    @Override
    public int getSegmentTimeoutInSec() {
        return segmentTimeoutInSec;
    }

    @Override
    public int getTokenTimeoutInSec() {
        return tokenTimeoutInSec;
    }

    @Override
    public int waitForTransactionsInMillis() {
        return waitForTransactionsInMillis;
    }

    @Override
    public boolean laspEnabled() {
        return !securityPoliciesToken.isEmpty();
    }

    @Override
    public String securityPoliciesToken() {
        return securityPoliciesToken;
    }

    @Override
    public boolean isCustomInstrumentationEditorAllowed() {
        return customInstrumentationEditorAllowed;
    }

    @Override
    public boolean isCustomParametersAllowed() {
        return customParameters;
    }

    @Override
    public DistributedTracingConfig getDistributedTracingConfig() {
        return distributedTracingConfig;
    }

    @Override
    public ExtensionsConfig getExtensionsConfig() {
        return extensionsConfig;
    }

    @Override
    public SpanEventsConfig getSpanEventsConfig() {
        return spanEventsConfig;
    }

    @Override
    public CommandParserConfig getCommandParserConfig() {
        return commandParserConfig;
    }

    @Override
    public InfiniteTracingConfig getInfiniteTracingConfig() {
        return infiniteTracingConfig;
    }

    private Object findPropertyInMap(String[] property, Map<String, Object> map) {
        Object result = map;
        for (String component : property) {
            if (result == null) {
                break;
            }
            if (result instanceof Map) {
                Map<?, ?> resultMap = (Map<?, ?>)result;
                result = resultMap.containsKey(component) ? resultMap.get(component) : null;
            }
        }

        return result;
    }

    @Override
    public List<String> logDeprecatedProperties(Map<String, Object> localSettings) {
        List<String> messages = new LinkedList<>();
        Joiner stringJoiner = Joiner.on(".");
        for (DeprecatedProperty deprecatedProperty : deprecatedProperties.values()) {
            String joinedName = stringJoiner.join(deprecatedProperty.propertyName);

            String format = "Configuration {0} is deprecated and will be removed in the next major version.";
            if (getPropertyFromSystemEnvironment(joinedName, null) != null) {
                format += " It was set in the environment.";
            } else if (getPropertyFromSystemProperties(joinedName, null) != null) {
                format += " It was set as a system property.";
            } else if (findPropertyInMap(deprecatedProperty.propertyName, localSettings) != null) {
                format += " It was set in the configuration file.";
            } else {
                // value was not set, so no need to warn the user.
                continue;
            }

            if (deprecatedProperty.newPropertyName != null) {
                format += " Use " + stringJoiner.join(deprecatedProperty.newPropertyName) + " instead.";
            } else {
                format += " This property is obsolete.";
            }

            messages.add(MessageFormat.format(format, joinedName));
        }
        for (String message : messages) {
            Agent.LOG.log(Level.WARNING, message);
        }

        // We logged them once on startup; we don't need to do this again when merging server config.
        clearDeprecatedProperties();
        addDeprecatedProperties = false;
        return messages;
    }

    @Override
    public int getTransactionSizeLimit() {
        return transactionSizeLimit;
    }

    @Override
    public boolean waitForRPMConnect() {
        return waitForRPMConnect;
    }

    @Override
    public boolean isSyncStartup() {
        return ApplicationAutoName.isAgentAttached() || getProperty(SYNC_STARTUP, DEFAULT_SYNC_STARTUP);
    }

    @Override
    public boolean isSendEnvironmentInfo() {
        return getProperty(SEND_ENVIRONMENT_INFO, DEFAULT_SEND_ENVIRONMENT_INFO);
    }

    @Override
    public boolean isLoggingToStdOut() {
        String logFileName = getLogFileName();
        return STDOUT.equalsIgnoreCase(logFileName);
    }

    @Override
    public int getLogFileCount() {
        return getIntProperty(LOG_FILE_COUNT, DEFAULT_LOG_FILE_COUNT);
    }

    @Override
    public String getLogFileName() {
        return getProperty(LOG_FILE_NAME, DEFAULT_LOG_FILE_NAME);
    }

    @Override
    public String getLogFilePath() {
        return getProperty(LOG_FILE_PATH);
    }

    @Override
    public String getLogLevel() {
        return logLevel;
    }

    @Override
    public int getLogLimit() {
        return getIntProperty(LOG_LIMIT, DEFAULT_LOG_LIMIT);
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig() {
        return transactionTracerConfig;
    }

    @Override
    public TransactionTracerConfig getBackgroundTransactionTracerConfig() {
        return backgroundTransactionTracerConfig;
    }

    @Override
    public TransactionTracerConfig getRequestTransactionTracerConfig() {
        return requestTransactionTracerConfig;
    }

    @Override
    public ErrorCollectorConfig getErrorCollectorConfig() {
        return errorCollectorConfig;
    }

    @Override
    public SqlTraceConfig getSqlTraceConfig() {
        return sqlTraceConfig;
    }

    @Override
    public CrossProcessConfig getCrossProcessConfig() {
        return crossProcessConfig;
    }

    @Override
    public ThreadProfilerConfig getThreadProfilerConfig() {
        return threadProfilerConfig;
    }

    @Override
    public JmxConfig getJmxConfig() {
        return jmxConfig;
    }

    @Override
    public JarCollectorConfig getJarCollectorConfig() {
        return jarCollectorConfig;
    }

    @Override
    public InsightsConfig getInsightsConfig() {
        return insightsConfig;
    }

    @Override
    public AttributesConfig getAttributesConfig() {
        return attributesConfig;
    }

    @Override
    public ReinstrumentConfig getReinstrumentConfig() {
        return reinstrumentConfig;
    }

    @Override
    public BrowserMonitoringConfig getBrowserMonitoringConfig() {
        return browserMonitoringConfig;
    }

    @Override
    public ClassTransformerConfig getClassTransformerConfig() {
        return classTransformerConfig;
    }

    /**
     * Returns the jars which should be ignored.
     */
    @Override
    public List<String> getIgnoreJars() {
        return ignoreJars;
    }

    /**
     * Gets the field obfuscateJvmProps.
     *
     * @return the obfuscateJvmProps
     */
    @Override
    public boolean isSendJvmProps() {
        return sendJvmProps;
    }

    @Override
    public String getCaBundlePath() {
        return caBundlePath;
    }

    @Override
    public boolean isLogDaily() {
        return logDaily;
    }

    @Override
    public boolean isTrimStats() {
        return trimStats;
    }

    @Override
    public boolean isPlatformInformationEnabled() {
        return platformInformationEnabled;
    }

    @Override
    public Set<String> getJDBCSupport() {
        return jdbcSupport;
    }

    @Override
    public boolean isGenericJDBCSupportEnabled() {
        return genericJdbcSupportEnabled;
    }

    @Override
    public int getMaxStackTraceLines() {
        return maxStackTraceLines;
    }

    @Override
    public Config getInstrumentationConfig() {
        return instrumentationConfig;
    }

    @Override
    public String getMetricIngestUri() {
        return metricIngestUri;
    }

    @Override
    public boolean isHighSecurity() {
        return highSecurity;
    }

    @Override
    public boolean isSimpleCompression() {
        return simpleCompression;
    }

    @Override
    public String getCompressedContentEncoding() {
        return compressedContentEncoding;
    }

    @Override
    public boolean isPutForDataSend() {
        return putForDataSend;
    }

    @Override
    public boolean getIbmWorkaroundEnabled() {
        return this.ibmWorkaroundEnabled;
    }

    @Override
    public LabelsConfig getLabelsConfig() {
        return labelsConfig;
    }

    @Override
    public NormalizationRuleConfig getNormalizationRuleConfig() {
        return normalizationRuleConfig;
    }

    @Override
    public boolean isStartupTimingEnabled() {
        return startupTimingEnabled;
    }

    @Override
    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return this.circuitBreakerConfig;
    }

    @Override
    public StripExceptionConfig getStripExceptionConfig() {
        return stripExceptionConfig;
    }

    @Override
    public TransactionNamingScheme getTransactionNamingScheme() {
        return transactionNamingMode;
    }

    @Override
    public UtilizationDataConfig getUtilizationDataConfig() {
        return utilizationConfig;
    }

    @Override
    public DatastoreConfig getDatastoreConfig() {
        return datastoreConfig;
    }

    @Override
    public ExternalTracerConfig getExternalTracerConfig() {
        return externalTracerConfig;
    }

    @Override
    public boolean openTracingEnabled() {
        return openTracingConfig.isEnabled();
    }

    @Override
    public TransactionEventsConfig getTransactionEventsConfig() {
        return transactionEventsConfig;
    }

}
