/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.NoOpInstrumentation;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.instrumentation.sql.NoOpTrackingSqlTracer;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TraceMethodVisitorTest {
    public Instrumentation bridgeInstrumentation = AgentBridge.instrumentation;
    public NoOpCountingInstrumentation counter = new NoOpCountingInstrumentation();

    @Before
    public void before() {
        AgentBridge.instrumentation = counter;
    }

    @After
    public void after() {
        AgentBridge.instrumentation = bridgeInstrumentation;
    }

    @Test
    public void testNoticeSql() {
        NoticeSqlClass noticeSqlClass = new NoticeSqlClass();
        Assert.assertEquals(noticeSqlClass.noticeSqlInvokeCount, counter.tracerCount);
        Assert.assertEquals(noticeSqlClass.noticeSqlDispatcherInvokeCount, 0);

        noticeSqlClass.noticeSqlMethod();
        Assert.assertEquals(noticeSqlClass.noticeSqlInvokeCount, counter.tracerCount);
        Assert.assertEquals(noticeSqlClass.noticeSqlConnection, counter.tracer.connection);
        Assert.assertEquals(noticeSqlClass.noticeSqlSql, counter.tracer.rawSql);
        Assert.assertArrayEquals(noticeSqlClass.noticeSqlParams, counter.tracer.params);
        Assert.assertEquals(noticeSqlClass.noticeSqlDispatcherInvokeCount, 0);
    }

    @Test
    public void testNoticeSqlAsDispatcher() {
        NoticeSqlClass noticeSqlClass = new NoticeSqlClass();
        Assert.assertEquals(noticeSqlClass.noticeSqlDispatcherInvokeCount, counter.tracerCount);
        Assert.assertEquals(noticeSqlClass.noticeSqlInvokeCount, 0);

        noticeSqlClass.noticeSqlDispatcherMethod();
        Assert.assertEquals(noticeSqlClass.noticeSqlDispatcherInvokeCount, counter.tracerCount);
        Assert.assertEquals(noticeSqlClass.noticeSqlConnection, counter.tracer.connection);
        Assert.assertEquals(noticeSqlClass.noticeSqlDispatcherSql, counter.tracer.rawSql);
        Assert.assertArrayEquals(noticeSqlClass.noticeSqlDispatcherParams, counter.tracer.params);
        Assert.assertEquals(noticeSqlClass.noticeSqlInvokeCount, 0);
    }

    public static class NoOpCountingInstrumentation extends NoOpInstrumentation {
        public int tracerCount = 0;
        public NoOpTrackingSqlTracer tracer; 

        @Override
        public ExitTracer createSqlTracer(Object invocationTarget, int signatureId, String metricName, int flags, String instrumentationModule) {
            tracerCount++;
            this.tracer = new NoOpTrackingSqlTracer();
            return tracer;
        }
    }

    public static class NoticeSqlClass {
        public int noticeSqlInvokeCount = 0;
        public ConnectionFactory noticeSqlConnectionFactory = new NoOpConnectionFactory();
        public Connection noticeSqlConnection = new NoOpConnection();
        public String noticeSqlSql = "SELECT * FROM noticeSql WHERE param = ?";
        public Object[] noticeSqlParams = new Object[] { "paramValue1" };
        public String host = "localhost";
        public Integer port = 12345;

        public int noticeSqlDispatcherInvokeCount = 0;
        public ConnectionFactory noticeSqlDispatcherConnectionFactory = new NoOpConnectionFactory();
        public String noticeSqlDispatcherSql = "SELECT * FROM noticeSqlDispatcher WHERE param = ?";
        public Object[] noticeSqlDispatcherParams = new Object[] { "paramValue2" };

        @Trace
        public void noticeSqlMethod() {
            noticeSqlInvokeCount++;
            DatastoreMetrics.noticeSql(noticeSqlConnection, noticeSqlSql, noticeSqlParams);
        }

        @Trace(dispatcher = true)
        public void noticeSqlDispatcherMethod() {
            noticeSqlDispatcherInvokeCount++;
            DatastoreMetrics.noticeSql(noticeSqlConnection, noticeSqlDispatcherSql, noticeSqlDispatcherParams);
        }
    }

    public static class NoOpConnectionFactory implements ConnectionFactory {
        @Override
        public Connection getConnection() throws SQLException {
            return null;
        }

        @Override
        public DatabaseVendor getDatabaseVendor() {
            return null;
        }
    }

    public static class NoOpConnection implements java.sql.Connection {

        @Override
        public Statement createStatement() throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return null;
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return null;
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {

        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return false;
        }

        @Override
        public void commit() throws SQLException {

        }

        @Override
        public void rollback() throws SQLException {

        }

        @Override
        public void close() throws SQLException {

        }

        @Override
        public boolean isClosed() throws SQLException {
            return false;
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return null;
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {

        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return false;
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {

        }

        @Override
        public String getCatalog() throws SQLException {
            return null;
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {

        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return 0;
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return null;
        }

        @Override
        public void clearWarnings() throws SQLException {

        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return null;
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return null;
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

        }

        @Override
        public void setHoldability(int holdability) throws SQLException {

        }

        @Override
        public int getHoldability() throws SQLException {
            return 0;
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return null;
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return null;
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {

        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {

        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
                throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return null;
        }

        @Override
        public Clob createClob() throws SQLException {
            return null;
        }

        @Override
        public Blob createBlob() throws SQLException {
            return null;
        }

        @Override
        public NClob createNClob() throws SQLException {
            return null;
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return null;
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return false;
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {

        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {

        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return null;
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return null;
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return null;
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return null;
        }

        @Override
        public void setSchema(String schema) throws SQLException {

        }

        @Override
        public String getSchema() throws SQLException {
            return null;
        }

        @Override
        public void abort(Executor executor) throws SQLException {

        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return 0;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }
    
}
