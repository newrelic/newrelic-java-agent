/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.database.SqlObfuscator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class TransactionTracerConfigImpl extends BaseConfig implements TransactionTracerConfig {

    public static final String BACKGROUND_CATEGORY_NAME = "background";
    public static final String REQUEST_CATEGORY_NAME = "request";
    public static final String APDEX_F = "apdex_f";
    public static final String CATEGORY = "category";
    public static final String CATEGORY_NAME = "name";
    public static final String COLLECT_TRACES = "collect_traces";
    public static final String ENABLED = "enabled";
    public static final String EXPLAIN_ENABLED = "explain_enabled";
    public static final String EXPLAIN_THRESHOLD = "explain_threshold";
    public static final String GC_TIME_ENABLED = "gc_time_enabled";
    public static final String INSERT_SQL_MAX_LENGTH = "insert_sql_max_length";
    public static final String LOG_SQL = "log_sql";
    public static final String MAX_EXPLAIN_PLANS = "max_explain_plans";
    public static final String MAX_STACK_TRACE = "max_stack_trace";
    public static final String OBFUSCATED_SQL_FIELDS = "obfuscated_sql_fields";
    public static final String RECORD_SQL = "record_sql";
    @Deprecated
    public static final String SLOW_QUERY_WHITELIST = "slow_query_whitelist";
    public static final String COLLECT_SLOW_QUERIES_FROM = "collect_slow_queries_from";
    public static final String SEGMENT_LIMIT = "segment_limit";
    public static final String STACK_TRACE_THRESHOLD = "stack_trace_threshold";
    public static final String TOKEN_LIMIT = "token_limit";
    public static final String TOP_N = "top_n";
    public static final String TRANSACTION_THRESHOLD = "transaction_threshold";
    public static final String EXEC_CALL_SQL_REGEX_DISABLED = "exec_call_sql_regex_disabled";
    public static final String SQL_METADATA_COMMENTS = "sql_metadata_comments";
    public static final String SQL_HASHING = "sql_hashing_enabled";

    public static final boolean DEFAULT_COLLECT_TRACES = false;
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_EXPLAIN_ENABLED = true;
    public static final double DEFAULT_EXPLAIN_THRESHOLD = 0.5d; // seconds
    public static final boolean DEFAULT_GC_TIME_ENABLED = false; // this is disabled by default because it is incorrect in all async scenarios
    public static final int DEFAULT_INSERT_SQL_MAX_LENGTH = 2000;
    public static final boolean DEFAULT_LOG_SQL = false;
    public static final int DEFAULT_MAX_EXPLAIN_PLANS = 20;
    public static final int DEFAULT_MAX_STACK_TRACE = 20;
    public static final String DEFAULT_RECORD_SQL = SqlObfuscator.OBFUSCATED_SETTING;
    public static final int DEFAULT_SEGMENT_LIMIT = 3000;
    public static final double DEFAULT_STACK_TRACE_THRESHOLD = 0.5d; // seconds
    public static final String DEFAULT_TRANSACTION_THRESHOLD = APDEX_F;
    public static final int DEFAULT_TOKEN_LIMIT = 3000;
    public static final int DEFAULT_TOP_N = 20;
    public static final boolean DEFAULT_EXEC_CALL_SQL_REGEX_DISABLED = false;
    public static final String DEFAULT_SQL_METADATA_COMMENTS = "";
    public static final boolean DEFAULT_SQL_HASHING_ENABLED = false;
    public static final int APDEX_F_MULTIPLE = 4;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.transaction_tracer.";
    public static final String CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT = "newrelic.config.transaction_tracer.category." + REQUEST_CATEGORY_NAME + ".";
    public static final String CATEGORY_BACKGROUND_SYSTEM_PROPERTY_ROOT = "newrelic.config.transaction_tracer.category." + BACKGROUND_CATEGORY_NAME + ".";

    private final boolean isEnabled;
    private final boolean isExplainEnabled;
    private final boolean isLogSql;
    private final String recordSql;
    private final Set<String> collectSlowQueriesFromModules;
    private final double explainThreshold;
    private final double explainThresholdInNanos;
    private final double stackTraceThreshold;
    private final double stackTraceThresholdInNanos;
    private final long transactionThreshold;
    private final long transactionThresholdInNanos;
    private final int insertSqlMaxLength;
    private final boolean gcTimeEnabled;
    private final int maxStackTraces;
    private final int maxSegments;
    private final int maxExplainPlans;
    private final int maxTokens;
    private final int topN;
    private final boolean isExecCallSqlRegexDisabled;
    private final String sqlMetadataComments;
    private final boolean isSqlHashingEnabled;
    protected final String inheritedFromSystemPropertyRoot;

    private TransactionTracerConfigImpl(String systemPropertyRoot, String inheritedFromSystemPropertyRoot,
            Map<String, Object> props, long apdexTInMillis, boolean highSecurity) {
        super(props, systemPropertyRoot);
        this.inheritedFromSystemPropertyRoot = inheritedFromSystemPropertyRoot;
        isEnabled = initEnabled();
        isLogSql = getProperty(LOG_SQL, DEFAULT_LOG_SQL);
        // recordSql must be off or obfuscated if high security is true
        recordSql = initRecordSql(highSecurity).intern(); // some code does an identity equals (==) on this value
        isExplainEnabled = initExplainEnabled(RecordSql.get(recordSql));
        collectSlowQueriesFromModules = initCollectSlowQueriesFrom(highSecurity);
        explainThreshold = getDoubleProperty(EXPLAIN_THRESHOLD, DEFAULT_EXPLAIN_THRESHOLD) * 1000;
        explainThresholdInNanos = TimeUnit.NANOSECONDS.convert((long) explainThreshold, TimeUnit.MILLISECONDS);
        stackTraceThreshold = getDoubleProperty(STACK_TRACE_THRESHOLD, DEFAULT_STACK_TRACE_THRESHOLD) * 1000;
        stackTraceThresholdInNanos = TimeUnit.NANOSECONDS.convert((long) stackTraceThreshold, TimeUnit.MILLISECONDS);
        transactionThreshold = initTransactionThreshold(apdexTInMillis);
        transactionThresholdInNanos = TimeUnit.NANOSECONDS.convert(transactionThreshold, TimeUnit.MILLISECONDS);
        insertSqlMaxLength = getIntProperty(INSERT_SQL_MAX_LENGTH, DEFAULT_INSERT_SQL_MAX_LENGTH);
        gcTimeEnabled = getProperty(GC_TIME_ENABLED, DEFAULT_GC_TIME_ENABLED);
        maxStackTraces = getIntProperty(MAX_STACK_TRACE, DEFAULT_MAX_STACK_TRACE);
        maxSegments = getIntProperty(SEGMENT_LIMIT, DEFAULT_SEGMENT_LIMIT);
        maxExplainPlans = getIntProperty(MAX_EXPLAIN_PLANS, DEFAULT_MAX_EXPLAIN_PLANS);
        maxTokens = getIntProperty(TOKEN_LIMIT, DEFAULT_TOKEN_LIMIT);
        topN = getIntProperty(TOP_N, DEFAULT_TOP_N);
        isExecCallSqlRegexDisabled = getProperty(EXEC_CALL_SQL_REGEX_DISABLED, DEFAULT_EXEC_CALL_SQL_REGEX_DISABLED);
        sqlMetadataComments = getProperty(SQL_METADATA_COMMENTS, DEFAULT_SQL_METADATA_COMMENTS);
        isSqlHashingEnabled = getProperty(SQL_HASHING, DEFAULT_SQL_HASHING_ENABLED);
    }

    private boolean initEnabled() {
        boolean isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        // required server property - false if subscription does not permit transaction traces
        boolean canCollectTraces = getProperty(COLLECT_TRACES, DEFAULT_COLLECT_TRACES);
        return isEnabled && canCollectTraces;
    }

    private boolean initExplainEnabled(RecordSql recordSql) {
        boolean isEnabled = getProperty(EXPLAIN_ENABLED, DEFAULT_EXPLAIN_ENABLED);
        // Explain plans should be disabled if record_sql is set to `off`
        return isEnabled && !recordSql.equals(RecordSql.off);
    }

    // even though getProperty returns `T` (here a String), that's a compile-time check.
    // Java generics at runtime allow any return value since it's only assigned to an Object.
    @SuppressWarnings("ConstantConditions")
    protected String initRecordSql(boolean highSecurity) {
        String output;
        Object val = getProperty(RECORD_SQL, DEFAULT_RECORD_SQL);
        if (val instanceof Boolean) {
            // the YAML parser interprets "on" as true and "off" as false
            output = SqlObfuscator.OFF_SETTING;
        } else {
            output = getProperty(RECORD_SQL, DEFAULT_RECORD_SQL).toLowerCase();
            if (!getUniqueStrings(OBFUSCATED_SQL_FIELDS).isEmpty()) {
                Agent.LOG.log(Level.WARNING, "The {0} setting is no longer supported.  Full SQL obfuscation is enabled.", OBFUSCATED_SQL_FIELDS);
                output = SqlObfuscator.OBFUSCATED_SETTING;
            }
        }

        // record sql needs to be off or obfuscated for high security
        if (highSecurity && !SqlObfuscator.OFF_SETTING.equals(output)) {
            output = SqlObfuscator.OBFUSCATED_SETTING;
        }
        return output;
    }

    protected Set<String> initCollectSlowQueriesFrom(boolean highSecurity) {

        if (highSecurity) {
            Collection<String> collectFromStrings = getUniqueStrings(COLLECT_SLOW_QUERIES_FROM);

            addDeprecatedProperty(
                    new String[] { AgentConfigImpl.TRANSACTION_TRACER, SLOW_QUERY_WHITELIST },
                    new String[] { AgentConfigImpl.TRANSACTION_TRACER, COLLECT_SLOW_QUERIES_FROM }
            );
            if (collectFromStrings.isEmpty()) {
                collectFromStrings = getUniqueStrings(SLOW_QUERY_WHITELIST);
            }

            return Collections.unmodifiableSet(new HashSet<>(collectFromStrings));
        }
        return Collections.emptySet();
    }

    private long initTransactionThreshold(long apdexTInMillis) {
        Object threshold = getProperty(TRANSACTION_THRESHOLD, DEFAULT_TRANSACTION_THRESHOLD);
        if (APDEX_F.equals(threshold)) {
            return apdexTInMillis * APDEX_F_MULTIPLE;
        }
        Number transactionThreshold = getProperty(TRANSACTION_THRESHOLD);
        return (long) (transactionThreshold.doubleValue() * 1000);
    }

    private Map<String, Object> initCategorySettings(String category) {
        Set<Map<String, Object>> categories = getCategories();
        for (Map<String, Object> categoryProps : categories) {
            if (category.equals(categoryProps.get(CATEGORY_NAME))) {
                return mergeSettings(getProperties(), categoryProps);
            }
        }
        return getProperties();
    }

    protected Set<Map<String, Object>> getCategories() {
        Object val = getProperty(CATEGORY);
        if (val instanceof Collection<?>) {
            return Collections.unmodifiableSet(getMapSetFromCollection((Collection<?>) val));
        }
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    protected Set<Map<String, Object>> getMapSetFromCollection(Collection<?> values) {
        Set<Map<String, Object>> result = new HashSet<>(values.size());
        for (Object value : values) {
            result.add((Map<String, Object>) value);
        }
        return result;
    }

    private Map<String, Object> mergeSettings(Map<String, Object> localSettings, Map<String, Object> serverSettings) {
        Map<String, Object> mergedSettings = new HashMap<>();
        if (localSettings != null) {
            mergedSettings.putAll(localSettings);
        }
        if (serverSettings != null) {
            mergedSettings.putAll(serverSettings);
        }
        return mergedSettings;
    }

    protected String getInheritedSystemPropertyKey(String key) {
        return inheritedFromSystemPropertyRoot == null ? null : inheritedFromSystemPropertyRoot + key;
    }

    @Override
    protected Object getPropertyFromSystemProperties(String name, Object defaultVal) {
        String key = getSystemPropertyKey(name);
        Object value = parseValue(SystemPropertyFactory.getSystemPropertyProvider().getSystemProperty(key));
        if (value != null) {
            return value;
        }

        String inheritedKey = getInheritedSystemPropertyKey(name);
        return inheritedKey == null ? null : parseValue(SystemPropertyFactory.getSystemPropertyProvider().getSystemProperty(inheritedKey));
    }

    @Override
    protected Object getPropertyFromSystemEnvironment(String name, Object defaultVal) {
        String key = getSystemPropertyKey(name);
        Object value = parseValue(SystemPropertyFactory.getSystemPropertyProvider().getEnvironmentVariable(key));
        if (value != null) {
            return value;
        }

        String inheritedKey = getInheritedSystemPropertyKey(name);
        return inheritedKey == null ? null : parseValue(SystemPropertyFactory.getSystemPropertyProvider().getEnvironmentVariable(inheritedKey));
    }

    @Override
    public boolean isExecCallSqlRegexDisabled() {
        return isExecCallSqlRegexDisabled;
    }

    @Override
    public double getExplainThresholdInMillis() {
        return explainThreshold;
    }

    @Override
    public double getExplainThresholdInNanos() {
        return explainThresholdInNanos;
    }

    @Override
    public String getRecordSql() {
        return recordSql;
    }

    @Override
    public Set<String> getCollectSlowQueriesFromModules() {
        return collectSlowQueriesFromModules;
    }

    @Override
    public double getStackTraceThresholdInMillis() {
        return stackTraceThreshold;
    }

    @Override
    public double getStackTraceThresholdInNanos() {
        return stackTraceThresholdInNanos;
    }

    @Override
    public long getTransactionThresholdInMillis() {
        return transactionThreshold;
    }

    @Override
    public long getTransactionThresholdInNanos() {
        return transactionThresholdInNanos;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isExplainEnabled() {
        return isExplainEnabled;
    }

    @Override
    public int getMaxExplainPlans() {
        return maxExplainPlans;
    }

    @Override
    public int getTopN() {
        return topN;
    }

    @Override
    public boolean isLogSql() {
        return isLogSql;
    }

    @Override
    public boolean isGCTimeEnabled() {
        return gcTimeEnabled;
    }

    @Override
    public int getInsertSqlMaxLength() {
        return insertSqlMaxLength;
    }

    @Override
    public int getMaxStackTraces() {
        return maxStackTraces;
    }

    @Override
    public int getMaxSegments() {
        return maxSegments;
    }

    @Override
    public int getMaxTokens() {
        return maxTokens;
    }

    @Override
    public String getSqlMetadataComments() {
        return sqlMetadataComments;
    }

    @Override
    public boolean isSqlHashingEnabled() {
        return isSqlHashingEnabled;
    }

    TransactionTracerConfigImpl createRequestTransactionTracerConfig(long apdexTInMillis, boolean highSecurity) {
        Map<String, Object> settings = initCategorySettings(REQUEST_CATEGORY_NAME);
        return new TransactionTracerConfigImpl(CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT, SYSTEM_PROPERTY_ROOT, settings, apdexTInMillis, highSecurity);
    }

    TransactionTracerConfigImpl createBackgroundTransactionTracerConfig(long apdexTInMillis, boolean highSecurity) {
        Map<String, Object> settings = initCategorySettings(BACKGROUND_CATEGORY_NAME);
        return new TransactionTracerConfigImpl(CATEGORY_BACKGROUND_SYSTEM_PROPERTY_ROOT, SYSTEM_PROPERTY_ROOT, settings, apdexTInMillis, highSecurity);
    }

    static TransactionTracerConfigImpl createTransactionTracerConfig(Map<String, Object> settings, long apdexTInMillis, boolean highSecurity) {
        return createTransactionTracerConfigImpl(settings, apdexTInMillis, highSecurity);
    }

    private static TransactionTracerConfigImpl createTransactionTracerConfigImpl(Map<String, Object> settings, long apdexTInMillis, boolean highSecurity) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new TransactionTracerConfigImpl(SYSTEM_PROPERTY_ROOT, null, settings, apdexTInMillis, highSecurity);
    }
}
