/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DefaultStatementFactory implements StatementFactory {
    private final Pattern pattern;
    private final DefaultStatementFactory backupPattern;
    protected final String key;
    private final boolean generateMetric;
    private final boolean isExecCallSqlRegexDisabled;

    private final Set<String> DISABLEABLE_REGEX = new HashSet<>(Arrays.asList("call", "exec"));

    public DefaultStatementFactory(String key, Pattern pattern, boolean generateMetric) {
        this.key = key;
        this.pattern = pattern;
        this.generateMetric = generateMetric;
        this.backupPattern = null;
        isExecCallSqlRegexDisabled = getExecCallSqlRegexDisabled();
    }

    public DefaultStatementFactory(String key, Pattern pattern, boolean generateMetric, Pattern backupPattern) {
        this.key = key;
        this.pattern = pattern;
        this.generateMetric = generateMetric;
        this.backupPattern = new DefaultStatementFactory(key, backupPattern, generateMetric);
        isExecCallSqlRegexDisabled = getExecCallSqlRegexDisabled();
    }

    protected boolean isMetricGenerator() {
        return generateMetric;
    }

    @Override
    public ParsedDatabaseStatement parseStatement(String statement) {
        // Optimizations to prevent running complex regex when we don't need to
        if (isExecCallSqlRegexDisabled && DISABLEABLE_REGEX.contains(getOperation())) {
            return null;
        }
        if (!StringUtils.containsIgnoreCase(statement, key)) {
            return null;
        }

        Matcher matcher = pattern.matcher(statement);
        if (matcher.find()) {
            String model = matcher.groupCount() > 0 ? matcher.group(1).trim() : "unknown";
            if (model.length() == 0) {
                Agent.LOG.log(Level.FINE, MessageFormat.format(
                        "Parsed an empty model name for {0} statement : {1}", key, statement));
                return null;
            }
            model = Strings.unquoteDatabaseName(model);
            // remove brackets from metric name because they are reserved for units suffix
            model = Strings.removeBrackets(model);
            // if we aren't generating a metric, don't bother to validate the model name
            if (generateMetric && !isValidModelName(model)) {
                if (Agent.LOG.isFineEnabled()) {
                    Agent.LOG.log(Level.FINE, "Parsed an invalid model name {0} for {1} statement : {2}", model, key, statement);
                }

                model = "ParseError";
            }
            return createParsedDatabaseStatement(model);
        }
        if (backupPattern != null) {
            return backupPattern.parseStatement(statement);
        }
        return null;
    }

    protected boolean isValidModelName(String name) {
        return DefaultDatabaseStatementParser.isValidName(name);
    }

    ParsedDatabaseStatement createParsedDatabaseStatement(String model) {
        return new ParsedDatabaseStatement(model.toLowerCase(), key, generateMetric);
    }

    @Override
    public String getOperation() {
        return key;
    }

    private boolean getExecCallSqlRegexDisabled() {
        ConfigService configService = ServiceFactory.getConfigService();
        TransactionTracerConfig transactionTracerConfig = ServiceFactory.getConfigService()
                .getTransactionTracerConfig(configService.getDefaultAgentConfig().getApplicationName());

        return transactionTracerConfig.isExecCallSqlRegexDisabled();
    }
}
