/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Set;

public interface TransactionTracerConfig {

    /**
     * @return <code>true</code> if transaction traces are enabled
     */
    boolean isEnabled();

    /**
     * The form for sending SQL statements to New Relic.
     *
     * @return either "off", for no SQL; "raw" for SQL in its original form; or "obfuscated", to replace numeric and
     * string literals with ?.
     */
    String getRecordSql();

    /**
     * For large SQL statements, executing the regular expressions that attempt to parse exec and
     * call statements can take a significant amount of time. Setting this to true will disable the
     * execution of these complex regular expressions.
     * Default is false.
     *
     * @return true if the exec and call regular expression execution is disabled
     */
    boolean isExecCallSqlRegexDisabled();

    /**
     * The set of modules that are allowed to send up obfuscated slow query information when high_security
     * mode is enabled. If high_security mode is disabled this setting is ignored.
     *
     * @return the set of modules that are allowed to send up obfuscated slow queries when high_security is enabled
     */
    Set<String> getCollectSlowQueriesFromModules();

    /**
     * If <code>true</code>, send recorded SQL to the agent log file, but do not report it to New Relic.
     */
    boolean isLogSql();

    /**
     * The limit on the length of a SQL statement sent to New Relic.
     *
     * @return the maximum length
     */
    int getInsertSqlMaxLength();

    /**
     * The threshold for sending a transaction trace to New Relic.
     *
     * @return the transaction trace threshold in milliseconds
     */
    long getTransactionThresholdInMillis();

    /**
     * The threshold for sending a transaction trace to New Relic.
     *
     * @return the transaction trace threshold in nanoseconds
     */
    long getTransactionThresholdInNanos();

    /**
     * The threshold for sending a stack trace for a SQL query to New Relic. .
     *
     * @return the stack trace threshold in milliseconds
     */
    double getStackTraceThresholdInMillis();

    double getStackTraceThresholdInNanos();

    /**
     * The threshold for sending an explain plan for a SQL query to New Relic. .
     *
     * @return the explain plan threshold in milliseconds
     */
    double getExplainThresholdInMillis();

    double getExplainThresholdInNanos();

    /**
     * @return <code>true</code> if explain plans are enabled
     */
    boolean isExplainEnabled();

    /**
     * The limit on the number of explain plans per transaction.
     *
     * @return the maximum number of explain plans
     */
    int getMaxExplainPlans();

    /**
     * @return <code>true</code> if GC time is enabled
     */
    boolean isGCTimeEnabled();

    /**
     * The limit on the number of stack traces to store per transaction.
     *
     * @return the maximum number of stack traces
     */
    int getMaxStackTraces();

    /**
     * The limit on the number of transaction trace segments per transaction.
     *
     * @return the maximum number of segments
     */
    int getMaxSegments();

    /**
     * The limit on the number of tokens to be retrieved per transaction.
     *
     * @return the maximum number of tokens
     */
    int getMaxTokens();

    /**
     * The number of unique traces to report before starting over.
     */
    int getTopN();

    /**
     * The String that controls what information (if any) gets added to metadata comments prepended
     * to SQL prior to execution. Valid values are "svc_name"", "txn_name", "trace_id". An empty
     * value disables metadata comments
     *
     * @return the configuration for SQL metadata comments
     */
    String getSqlMetadataComments();

}
