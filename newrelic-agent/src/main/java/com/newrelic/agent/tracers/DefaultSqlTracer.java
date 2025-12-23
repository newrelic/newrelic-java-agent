/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.bridge.datastore.UnknownDatabaseVendor;
import com.newrelic.agent.sql.SqlStatementHasher;
import com.newrelic.agent.sql.SqlStatementNormalizer;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.QueryConverter;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONAware;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseStatementParser;
import com.newrelic.agent.database.DefaultExplainPlanExecutor;
import com.newrelic.agent.database.ExplainPlanExecutor;
import com.newrelic.agent.database.ParsedDatabaseStatement;
import com.newrelic.agent.database.PreparedStatementExplainPlanExecutor;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class DefaultSqlTracer extends DefaultTracer implements SqlTracer, Comparable<DefaultSqlTracer> {

    private static final String PARAMETER_REGEX = "\\?";
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEX);

    // A non skipped single quote, ie not \'. This is intended to be used on strings used as parameters, not whole queries.
    private static final Pattern UNESCAPED_QUOTE_PATTERN = Pattern.compile("(?<!\\\\)'");

    private ConnectionFactory connectionFactory = null;
    private String sql = null;
    private Object[] params = null;

    private Object sqlObject = null;
    private long rpmConnectTimestamp = 0;
    private ParsedDatabaseStatement parsedDatabaseStatement = null;
    private ExplainPlanExecutor explainPlanExecutor = null;
    private String host = null;
    private Integer port = null;
    private String identifier = null;
    private String databaseName = null;
    private String normalizedSqlHashValue = null;

    public DefaultSqlTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags) {
        super(transaction, sig, object, metricNameFormatter, tracerFlags);
    }

    public DefaultSqlTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags, long time) {
        super(transaction.getTransactionActivity(), sig, object, metricNameFormatter, tracerFlags, time);
    }

    public DefaultSqlTracer(TransactionActivity txa, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags) {
        super(txa, sig, object, metricNameFormatter, tracerFlags);
    }

    public DefaultSqlTracer(TransactionActivity txa, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags, long pStartTime) {
        super(txa, sig, object, metricNameFormatter, tracerFlags, pStartTime);
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Override
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setHost(String host) {
        this.host = host;
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Setting host to {0} {1}", this.host, this);
    }

    public void setPort(int port) {
        this.port = port;
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Setting port to {0} {1}", this.port, this);
    }

    @Override
    public String getRawSql() {
        return sql;
    }

    @Override
    public String getNormalizedSqlHashValue() {
        return normalizedSqlHashValue;
    }

    @Override
    public void provideConnection(Connection conn) {
        // provideConnection is called from weaved code that doesn't have try catch blocks, hence the try catch here
        try {
            if (conn == null) {
                return;
            }

            setConnectionFactory(JdbcHelper.getConnectionFactory(conn));
            setDatabaseName(JdbcHelper.getDatabaseName(conn));

            // TCP connections
            InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(conn);
            if (address != null) {
                setHost(address.getHostName());
                setPort(address.getPort());
                return;
            }

            String identifier;

            // In memory databases, filesystem, and unix domain socket connections.
            String cachedIdentifier = JdbcHelper.getCachedIdentifierForConnection(conn);
            if (cachedIdentifier != null) {
                identifier = cachedIdentifier;
            } else {
                identifier = JdbcHelper.parseAndCacheInMemoryIdentifier(conn);
            }

            if (identifier != null && !identifier.equals(JdbcHelper.UNKNOWN)) {
                // We only parse connection strings for in memory databases.
                setHost("localhost");
            }
            setIdentifier(identifier);
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, t, "Unable to provide connection: {0} to {1}", conn, this);
        }
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public void setRawSql(String sql) {
        this.sql = sql;
        System.out.println("DUF-- sql " + sql);

        // DUF TODO config check here
        String normalizedSql = SqlStatementNormalizer.normalizeSql(sql);
        if (!normalizedSql.isEmpty()) {
            try {
                normalizedSqlHashValue = SqlStatementHasher.hashSqlStatement(normalizedSql, MessageDigest.getInstance("MD5"));
                System.out.println("DUF-- normalizedSql " + normalizedSql);
                System.out.println("DUF-- hash " + normalizedSqlHashValue);
            } catch (NoSuchAlgorithmException ignored) {
            }
        }
    }

    @Override
    public Object[] getParams() {
        return params;
    }

    @Override
    public void setParams(Object[] params) {
        if (params != null && params.length > 0) {
            this.params = params;
        }
    }

    protected DatabaseVendor getDatabaseVendor() {
        if (connectionFactory == null) {
            if (parsedDatabaseStatement != null) {
                return parsedDatabaseStatement.getDbVendor();
            }
            return UnknownDatabaseVendor.INSTANCE;
        }

        return connectionFactory.getDatabaseVendor();
    }

    @Override
    protected void doFinish(Throwable throwable) {
        super.doFinish(throwable);

        // if an error is thrown, stick the sql up in the transaction parameters so that it'll show up in the traced
        // error
        Object sql = getSql();
        if (sql != null) {
            Transaction transaction = getTransaction();
            if (transaction != null) {
                if (getRecordSql().equals(RecordSql.raw)) {
                    // Store the raw query string
                    getTransaction().getIntrinsicAttributes().put(SQL_PARAMETER_NAME, sql.toString());
                } else if (getRecordSql().equals(RecordSql.obfuscated)) {
                    String appName = getTransaction().getApplicationName();
                    SqlQueryConverter converter = new SqlQueryConverter(appName, getDatabaseVendor());
                    String obfuscatedQueryString = converter.toObfuscatedQueryString(sql.toString());

                    // Store the obfuscated query string
                    getTransaction().getIntrinsicAttributes().put(SQL_PARAMETER_NAME, obfuscatedQueryString);
                }
            }
        }
    }

    @Override
    protected void doFinish(int opcode, Object returnValue) {
        super.doFinish(opcode, returnValue);

        Transaction transaction = getTransaction();
        if (transaction != null) {
            TransactionTracerConfig transactionTracerConfig = transaction.getTransactionTracerConfig();
            double explainThresholdInNanos = transactionTracerConfig.getExplainThresholdInNanos();

            // The string equality check here is intentional, both values are interned Strings
            if (SqlObfuscator.RAW_SETTING == transactionTracerConfig.getRecordSql()
                    || getDuration() > explainThresholdInNanos) {
                // we have to copy the parameters because they will be cleared later
                Object[] sqlParameters = params == null ? null : new Object[params.length];
                if (sqlParameters != null) {
                    System.arraycopy(params, 0, sqlParameters, 0, params.length);
                    params = sqlParameters;
                }
            }

            if (isTransactionSegment() && captureSql()) {
                this.sqlObject = getSql();
            }
            parseStatement(returnValue, transaction.getRPMService().getConnectionTimestamp());

            if (isTransactionSegment() && sql != null) {
                if (transactionTracerConfig.isExplainEnabled()) {
                    captureExplain(parsedDatabaseStatement, explainThresholdInNanos, transactionTracerConfig);
                } else {
                    if (Agent.isDebugEnabled()) {
                        String msg = MessageFormat.format("Statement exceeded threshold?: {0}",
                                getDuration() > explainThresholdInNanos);
                        Agent.LOG.finer(msg);
                    }
                }
            }
        }
    }

    @Override
    protected boolean shouldStoreStackTrace() {
        return (super.shouldStoreStackTrace() && sql != null);
    }

    @Override
    public boolean isMetricProducer() {
        return parsedDatabaseStatement != null && parsedDatabaseStatement.recordMetric();
    }

    /**
     * Need to override this method to record Datastore metrics before the scoped metric name is recorded
     */
    @Override
    protected void recordMetrics(TransactionStats transactionStats) {
        if (isMetricProducer() && getTransaction() != null) {
            String rawSql = null;
            Object sqlObject = getSql();
            if (sqlObject != null) {
                rawSql = new PreparedStatementSql(sql, params).toString();
            }

            String appName = getTransaction().getApplicationName();
            String hostToReport = DatastoreMetrics.replaceLocalhost(getHost());

            if (getIdentifier() != null) {
                this.reportAsExternal(DatastoreParameters
                        .product(getDatabaseVendor().getDatastoreVendor().name())
                        .collection(parsedDatabaseStatement.getModel())
                        .operation(parsedDatabaseStatement.getOperation())
                        .instance(hostToReport, getIdentifier())
                        .databaseName(getDatabaseName())
                        .slowQuery(rawSql, new SqlQueryConverter(appName, getDatabaseVendor()), normalizedSqlHashValue)
                        .build());
            } else {
                String portToReport = DatastoreMetrics.replacePort(getPort());
                this.reportAsExternal(DatastoreParameters
                        .product(getDatabaseVendor().getDatastoreVendor().name())
                        .collection(parsedDatabaseStatement.getModel())
                        .operation(parsedDatabaseStatement.getOperation())
                        .instance(hostToReport, portToReport)
                        .databaseName(getDatabaseName())
                        .slowQuery(rawSql, new SqlQueryConverter(appName, getDatabaseVendor()), normalizedSqlHashValue)
                        .build());
            }

            if (parsedDatabaseStatement == DatabaseStatementParser.UNPARSEABLE_STATEMENT) {
                DatastoreMetrics.unparsedQuerySupportability(getDatabaseVendor().getDatastoreVendor().name());
            }
        }
        super.recordMetrics(transactionStats);
    }

    /**
     * Returns the sql for the statement instrumented by this driver. This method is overridden by prepared statement
     * tracers which return a sql object that can delay the rendering of the full sql statement with parameters until it
     * is turned to JSON.
     *
     * @return Returns a sql object which is either a string or a {@link JSONAware} instance.
     */
    @Override
    public Object getSql() {
        if (sqlObject != null) {
            return sqlObject;
        }

        // if raw sql is enabled, return an object that can plug parameters back into the statement
        if (RecordSql.raw.equals(getRecordSql())) {
            if (params != null) {
                return new PreparedStatementSql(sql, params);
            } else {
                return sql;
            }
        } else {
            return sql;
        }
    }

    @Override
    public boolean hasExplainPlan() {
        return (getAgentAttribute(EXPLAIN_PLAN_PARAMETER_NAME) != null);
    }

    public ExplainPlanExecutor getExplainPlanExecutor() {
        return explainPlanExecutor;
    }

    protected ExplainPlanExecutor createExplainPlanExecutor(String sql) {
        if (params != null && params.length > 0) {
            return new PreparedStatementExplainPlanExecutor(this, getRawSql(), getParams(), getRecordSql());
        }
        return new DefaultExplainPlanExecutor(this, sql, getRecordSql());
    }

    private RecordSql getRecordSql() {
        Transaction tx = getTransaction();
        if (tx == null) {
            return RecordSql.off;
        }
        return RecordSql.get(tx.getTransactionTracerConfig().getRecordSql());
    }

    private boolean captureSql() {
        Transaction tx = getTransaction();
        return tx != null && SqlObfuscator.OFF_SETTING != tx.getTransactionTracerConfig().getRecordSql();
    }

    private void parseStatement(Object returnValue, long configTimestamp) {
        Transaction tx = getTransaction();
        if (tx == null) {
            // Without a transaction there is no parser
            return;
        }

        if (parsedDatabaseStatement == null) {
            ResultSetMetaData metaData = null;
            try {
                if (returnValue instanceof ResultSet) {
                    metaData = ((ResultSet) returnValue).getMetaData();
                }
            } catch (Exception e) {
                if (Agent.isDebugEnabled()) {
                    Agent.LOG.log(Level.FINER, "Unable to get the result set meta data from a statement", e);
                }
            }
            rpmConnectTimestamp = System.nanoTime();
            parsedDatabaseStatement = tx.getDatabaseStatementParser().getParsedDatabaseStatement(getDatabaseVendor(),
                    getRawSql(),  metaData);
        } else if (configTimestamp > rpmConnectTimestamp) {
            parsedDatabaseStatement = null;
            rpmConnectTimestamp = 0;
            parseStatement(returnValue, configTimestamp);
        }
    }

    private void captureExplain(ParsedDatabaseStatement parsedStatement, double explainThresholdInNanos,
            TransactionTracerConfig transactionTracerConfig) {
        Transaction tx = getTransaction();
        if (tx != null && getDuration() > explainThresholdInNanos && DatabaseStatementParser.SELECT_OPERATION.equals(
                parsedStatement.getOperation())) {

            if (tx.getTransactionCounts().getExplainPlanCount() >= transactionTracerConfig.getMaxExplainPlans()) {
                return;
            }
            if (StringUtils.isEmpty(sql)) {
                setExplainPlan("Unable to run the explain plan because we have no sql");
                return;
            }

            // get connection factory for select statements so we can run explain plans later
            try {
                if (!getDatabaseVendor().isExplainPlanSupported()) {
                    setExplainPlan("Unable to run explain plans for " + getDatabaseVendor().getName() + " databases");
                    return;
                }

                if (connectionFactory != null) {
                    explainPlanExecutor = createExplainPlanExecutor(sql);
                    if (explainPlanExecutor != null) {
                        if (Agent.LOG.isLoggable(Level.FINEST)) {
                            Agent.LOG.finest("Capturing information for explain plan");
                        }
                        tx.getTransactionCounts().incrementExplainPlanCountAndLogIfReachedMax(
                                transactionTracerConfig.getMaxExplainPlans());
                    }
                } else {
                    setExplainPlan("Unable to create a connection to run the explain plan");
                }
            } catch (Exception e) {
                String msg = MessageFormat.format("An error occurred running the explain plan: {0}", e);
                setExplainPlan(msg);
                Agent.LOG.finer(msg);
            }
        }
    }

    public void setExplainPlan(Object... explainPlan) {
        setAgentAttribute(EXPLAIN_PLAN_PARAMETER_NAME, Arrays.asList(explainPlan));
        if (getDatabaseVendor().getDatastoreVendor() != DatastoreVendor.JDBC) {
            setAgentAttribute(DATABASE_VENDOR_PARAMETER_NAME, getDatabaseVendor().getType());
            setAgentAttribute(EXPLAIN_PLAN_FORMAT_PARAMETER_NAME, getDatabaseVendor().getExplainPlanFormat());
        }
    }

    @Override
    public int compareTo(DefaultSqlTracer otherTracer) {
        long durationDifference = getDuration() - otherTracer.getDuration();
        if (durationDifference < 0) {
            return -1;
        }
        if (durationDifference > 0) {
            return 1;
        }
        return 0;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Setting identifier to {0} {1}", this.identifier, this);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Setting database name to {0} {1}", this.databaseName, this);
    }

    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Stores prepared statement sql and the parameters from a statement execution. When turned into json the full
     * parameterized sql statement is returned.
     */
    private static class PreparedStatementSql implements JSONAware {

        private final String sql;
        private final Object[] sqlParameters;

        public PreparedStatementSql(String sql, Object[] sqlParameters) {
            this.sql = sql;
            this.sqlParameters = sqlParameters;
        }

        @Override
        public String toJSONString() {
            try {
                return parameterizeSql(sql, sqlParameters);
            } catch (Exception ignored) {
                return sql;
            }
        }

        @Override
        public String toString() {
            return toJSONString();
        }

    }

    /**
     * Substitute values from the parameter map in the SQL.
     *
     * @param sql the SQL
     * @param parameters the parameter map
     * @return the parameterized SQL
     */
    public static String parameterizeSql(String sql, Object[] parameters) throws Exception {
        if (sql == null || parameters == null || parameters.length == 0) {
            return sql;
        }
        String[] pieces = PARAMETER_PATTERN.split(sql);
        StringBuilder sb = new StringBuilder(sql.length() * 2);
        for (int i = 0, j = 1; i < pieces.length; i++, j++) {
            String piece = pieces[i];
            if (j == pieces.length && sql.endsWith(piece)) {
                sb.append(piece);
            } else {
                Object val = i < parameters.length ? parameters[i] : null;
                if (val instanceof Number) {
                    sb.append(piece).append(val.toString());
                } else if (val == null) {
                    sb.append(piece).append("?");
                } else {
                    String escapedVal = escapeQuotes(val.toString());
                    sb.append(piece).append("'").append(escapedVal).append("'");
                }
            }
        }
        return sb.toString();
    }

    private static String escapeQuotes(String val) {
        if (val.indexOf('\'') == -1) {
            return val;
        }
        return UNESCAPED_QUOTE_PATTERN.matcher(val).replaceAll("''");
    }

    private static class SqlQueryConverter implements QueryConverter<String> {
        private final String appName;
        private final DatabaseVendor databaseVendor;

        public SqlQueryConverter(String appName, DatabaseVendor databaseVendor) {
            this.appName = appName;
            this.databaseVendor = databaseVendor;
        }

        @Override
        public String toRawQueryString(String rawQuery) {
            return rawQuery;
        }

        @Override
        public String toObfuscatedQueryString(String rawQuery) {
            SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getSqlObfuscator(appName);
            String dialect = databaseVendor.getType();
            return sqlObfuscator.obfuscateSql(rawQuery, dialect);
        }
    }

}
