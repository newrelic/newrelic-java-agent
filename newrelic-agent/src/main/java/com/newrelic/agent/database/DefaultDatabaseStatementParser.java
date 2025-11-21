/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TraceMetadata;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSetMetaData;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultDatabaseStatementParser implements DatabaseStatementParser {
    static final int PATTERN_SWITCHES = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
    static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*select.*?\\sfrom[\\s\\[]+([^\\]\\s,)(;]*).*",
            PATTERN_SWITCHES);

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern NR_HINT_PATTERN = Pattern.compile(
            "\\s*/\\*\\s*nrhint\\s*:\\s*([^\\*]*)\\s*\\*/\\s*([^\\s]*).*", Pattern.DOTALL);
    private static final Pattern VALID_METRIC_NAME_MATCHER = Pattern.compile("[a-zA-Z0-9.$_@]+");
    private static final Pattern EXEC_VAR_PATTERN = Pattern.compile(
            ".*(?:exec|execute)\\s+[^\\s(,]*.*?=(?:\\s|)([^\\s]*)", PATTERN_SWITCHES);

    private final Set<String> knownOperations;
    private final List<StatementFactory> statementFactories;

    public DefaultDatabaseStatementParser() {
        // the ordering of these factories is important
        statementFactories = Arrays.asList(
                new InnerSelectStatementFactory(),
                new DefaultStatementFactory("show", Pattern.compile("^\\s*show\\s+(.*)$", PATTERN_SWITCHES), false) {
                    @Override
                    protected boolean isValidModelName(String name) {
                        return true;
                    }
                },
                // @formatter:off
                new DefaultStatementFactory(INSERT_OPERATION, Pattern.compile("^\\s*insert(?:\\s+ignore)?(?:\\s+into)?\\s+([^\\s(,;]*).*", PATTERN_SWITCHES),
                        true),
                new DefaultStatementFactory("update", Pattern.compile("^\\s*update\\s+([^\\s,;]*).*", PATTERN_SWITCHES), true),
                new DefaultStatementFactory("delete", Pattern.compile("^\\s*delete\\s*?.*?\\s+from\\s+([^\\s,(;]*).*", PATTERN_SWITCHES), true),
                new DefaultStatementFactory("with", Pattern.compile("^\\s*with\\s+(?:recursive\\s+)?([^\\s,(;]*)", PATTERN_SWITCHES), true),
                new DDLStatementFactory("create", Pattern.compile("^\\s*create\\s+procedure.*", PATTERN_SWITCHES), "Procedure"),
                new SelectVariableStatementFactory(),
                new DDLStatementFactory("drop", Pattern.compile("^\\s*drop\\s+procedure.*", PATTERN_SWITCHES), "Procedure"),
                new DDLStatementFactory("create", Pattern.compile("^\\s*create\\s+table.*", PATTERN_SWITCHES), "Table"),
                new DDLStatementFactory("drop", Pattern.compile("^\\s*drop\\s+table.*", PATTERN_SWITCHES), "Table"),
                new DefaultStatementFactory("alter", Pattern.compile("^\\s*alter\\s+([^\\s]*).*", PATTERN_SWITCHES), false),
                new DefaultStatementFactory("call", Pattern.compile(".*call\\s+([^\\s(,]*).*", PATTERN_SWITCHES), true),
                new DefaultStatementFactory("exec", Pattern.compile(".*(?:exec|execute)\\s+(?!as\\s+)([^\\s(,=;]*+);?\\s*+(?:[^=]|$).*", PATTERN_SWITCHES),
                        true, EXEC_VAR_PATTERN),
                new DefaultStatementFactory("set", Pattern.compile("^\\s*set\\s+(.*)\\s*(as|=).*", PATTERN_SWITCHES), false));
        // @formatter:on

        knownOperations = new HashSet<>();
        for (StatementFactory factory : statementFactories) {
            knownOperations.add(factory.getOperation());
        }
    }

    @Override
    public ParsedDatabaseStatement getParsedDatabaseStatement(
            DatabaseVendor databaseVendor, String statement,
            ResultSetMetaData metaData) {
        Matcher hintMatcher = NR_HINT_PATTERN.matcher(statement);
        if (hintMatcher.matches()) {
            String model = hintMatcher.group(1).trim().toLowerCase();
            String operation = hintMatcher.group(2).toLowerCase();
            if (!knownOperations.contains(operation)) {
                operation = "unknown";
            }

            return new ParsedDatabaseStatement(model, operation, true);
        }
        if (metaData != null) {
            try {
                int columnCount = metaData.getColumnCount();
                if (columnCount > 0) {
                    String tableName = metaData.getTableName(1);
                    if (!StringUtils.isEmpty(tableName)) {
                        return new ParsedDatabaseStatement(tableName.toLowerCase(), SELECT_OPERATION, true);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return parseStatement(statement);
    }

    ParsedDatabaseStatement parseStatement(String statement) {
        try {
            Agent.LOG.log(Level.INFO, "DUF-- StatementParser");
            statement = COMMENT_PATTERN.matcher(statement).replaceAll("");
            //statement = prependMetadataComment(statement);
            for (StatementFactory factory : statementFactories) {
                ParsedDatabaseStatement parsedStatement = factory.parseStatement(statement);
                if (parsedStatement != null) {
                    return parsedStatement;
                }
            }
            Agent.LOG.log(Level.FINE, "Returning UNPARSEABLE_STATEMENT for statement: {0}", statement);
            return UNPARSEABLE_STATEMENT;
        } catch (Throwable t) {
            Agent.LOG.fine(MessageFormat.format("Unable to parse sql \"{0}\" - {1}", statement, t.toString()));
            Agent.LOG.log(Level.FINER, "SQL parsing error", t);
            Agent.LOG.log(Level.FINE, t, "Returning UNPARSEABLE_STATEMENT for statement: {0}", statement);
            return UNPARSEABLE_STATEMENT;
        }
    }

    /**
     * If a transaction is in progress, prepend a comment to the statement that contains the
     * trace id, span id and app name.
     *
     * @param originalStatement the original statement to be modified
     *
     * @return the modified statement with the metadata comment if we're in a transaction
     */
    String prependMetadataComment(String originalStatement) {
        if (NewRelic.getAgent().getTransaction() != NoOpTransaction.INSTANCE) {
            TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();
            return String.format("/* nr_trace_id=%s,nr_span_id=%s,nr_service=%s */", traceMetadata.getTraceId(), traceMetadata.getSpanId(),
                    NewRelic.getAgent().getConfig().getValue("app_name"));
        }

        return originalStatement;
    }

    static boolean isValidName(String string) {
        return VALID_METRIC_NAME_MATCHER.matcher(string).matches();
    }

}
