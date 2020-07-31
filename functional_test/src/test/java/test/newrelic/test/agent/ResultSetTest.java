/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import com.newrelic.agent.AgentHelper;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

public class ResultSetTest {

    @Test
    public void testFlyWeightResultSet() throws SQLException {
        transactionStart();
        Set<String> metrics = AgentHelper.getMetrics();
        Assert.assertTrue(metrics.toString(), metrics.contains("Datastore/ResultSet"));
        Assert.assertTrue(metrics.toString(),
                metrics.contains("OtherTransaction/Custom/test.newrelic.test.agent.ResultSetTest/transactionStart"));
    }

    @Test
    public void testConcurrentCallableInOldCode() throws ClassNotFoundException {
        // If this test fails, it means the XmlRpcPointCut is no longer being used. In this
        // case, the testFlyWeightResultSet test is not actually testing what it should.
        // It should test a pointcut being hit within a result set method.
        Class<?> daClass = this.getClass().getClassLoader().loadClass(
                "com.newrelic.agent.instrumentation.pointcuts.XmlRpcPointCut");
        Assert.assertTrue(daClass.getName().contains("XmlRpcPointCut"));
    }

    @Trace(dispatcher = true)
    public void transactionStart() throws SQLException {
        TestResultSet set = new TestResultSet();
        set.getString(0);
        set.getInt(1);
        set.getBoolean(2);
        set.next();
        set.close();
    }

    public class TestResultSet implements ResultSet {

        public void methodCalledFromInsideResultSetMethod() {
            // this code needs to hit an old point cut
            try {
                RpcCall rpcCall = new RpcCall();
                rpcCall.invoke(null);
            } catch (Exception e) {
                // do nothing here
            }
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean next() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public void close() throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public boolean wasNull() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public String getString(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return "";
        }

        @Override
        public boolean getBoolean(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public byte getByte(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public short getShort(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public int getInt(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public long getLong(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public float getFloat(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public double getDouble(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public byte[] getBytes(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Date getDate(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Time getTime(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Timestamp getTimestamp(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public InputStream getAsciiStream(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public InputStream getUnicodeStream(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public InputStream getBinaryStream(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public boolean getBoolean(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public byte getByte(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public short getShort(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public long getLong(String columnLabel) throws SQLException {

            return 0;
        }

        @Override
        public float getFloat(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public double getDouble(String columnLabel) throws SQLException {

            return 0;
        }

        @Override
        public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public byte[] getBytes(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Date getDate(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Time getTime(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Timestamp getTimestamp(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public InputStream getAsciiStream(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public InputStream getUnicodeStream(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public InputStream getBinaryStream(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public void clearWarnings() throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public String getCursorName() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public ResultSetMetaData getMetaData() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Object getObject(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Object getObject(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public int findColumn(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public Reader getCharacterStream(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public Reader getCharacterStream(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return null;
        }

        @Override
        public boolean isBeforeFirst() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean isAfterLast() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean isFirst() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean isLast() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public void beforeFirst() throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void afterLast() throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public boolean first() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean last() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public int getRow() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public boolean absolute(int row) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean relative(int rows) throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean previous() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public void setFetchDirection(int direction) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public int getFetchDirection() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public void setFetchSize(int rows) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public int getFetchSize() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public int getType() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public int getConcurrency() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return 0;
        }

        @Override
        public boolean rowUpdated() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean rowInserted() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public boolean rowDeleted() throws SQLException {
            methodCalledFromInsideResultSetMethod();
            return false;
        }

        @Override
        public void updateNull(int columnIndex) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateBoolean(int columnIndex, boolean x) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateByte(int columnIndex, byte x) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateShort(int columnIndex, short x) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateInt(int columnIndex, int x) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateLong(int columnIndex, long x) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateFloat(int columnIndex, float x) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateDouble(int columnIndex, double x) throws SQLException {
            methodCalledFromInsideResultSetMethod();
        }

        @Override
        public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {

        }

        @Override
        public void updateString(int columnIndex, String x) throws SQLException {

        }

        @Override
        public void updateBytes(int columnIndex, byte[] x) throws SQLException {

        }

        @Override
        public void updateDate(int columnIndex, Date x) throws SQLException {

        }

        @Override
        public void updateTime(int columnIndex, Time x) throws SQLException {

        }

        @Override
        public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {

        }

        @Override
        public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {

        }

        @Override
        public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {

        }

        @Override
        public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {

        }

        @Override
        public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {

        }

        @Override
        public void updateObject(int columnIndex, Object x) throws SQLException {

        }

        @Override
        public void updateNull(String columnLabel) throws SQLException {

        }

        @Override
        public void updateBoolean(String columnLabel, boolean x) throws SQLException {

        }

        @Override
        public void updateByte(String columnLabel, byte x) throws SQLException {

        }

        @Override
        public void updateShort(String columnLabel, short x) throws SQLException {

        }

        @Override
        public void updateInt(String columnLabel, int x) throws SQLException {

        }

        @Override
        public void updateLong(String columnLabel, long x) throws SQLException {

        }

        @Override
        public void updateFloat(String columnLabel, float x) throws SQLException {

        }

        @Override
        public void updateDouble(String columnLabel, double x) throws SQLException {

        }

        @Override
        public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {

        }

        @Override
        public void updateString(String columnLabel, String x) throws SQLException {

        }

        @Override
        public void updateBytes(String columnLabel, byte[] x) throws SQLException {

        }

        @Override
        public void updateDate(String columnLabel, Date x) throws SQLException {

        }

        @Override
        public void updateTime(String columnLabel, Time x) throws SQLException {

        }

        @Override
        public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {

        }

        @Override
        public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {

        }

        @Override
        public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {

        }

        @Override
        public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {

        }

        @Override
        public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {

        }

        @Override
        public void updateObject(String columnLabel, Object x) throws SQLException {

        }

        @Override
        public void insertRow() throws SQLException {

        }

        @Override
        public void updateRow() throws SQLException {

        }

        @Override
        public void deleteRow() throws SQLException {

        }

        @Override
        public void refreshRow() throws SQLException {

        }

        @Override
        public void cancelRowUpdates() throws SQLException {

        }

        @Override
        public void moveToInsertRow() throws SQLException {

        }

        @Override
        public void moveToCurrentRow() throws SQLException {

        }

        @Override
        public Statement getStatement() throws SQLException {

            return null;
        }

        @Override
        public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {

            return null;
        }

        @Override
        public Ref getRef(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public Blob getBlob(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public Clob getClob(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public Array getArray(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {

            return null;
        }

        @Override
        public Ref getRef(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public Blob getBlob(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public Clob getClob(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public Array getArray(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public Date getDate(int columnIndex, Calendar cal) throws SQLException {

            return null;
        }

        @Override
        public Date getDate(String columnLabel, Calendar cal) throws SQLException {

            return null;
        }

        @Override
        public Time getTime(int columnIndex, Calendar cal) throws SQLException {

            return null;
        }

        @Override
        public Time getTime(String columnLabel, Calendar cal) throws SQLException {

            return null;
        }

        @Override
        public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {

            return null;
        }

        @Override
        public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {

            return null;
        }

        @Override
        public URL getURL(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public URL getURL(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public void updateRef(int columnIndex, Ref x) throws SQLException {

        }

        @Override
        public void updateRef(String columnLabel, Ref x) throws SQLException {

        }

        @Override
        public void updateBlob(int columnIndex, Blob x) throws SQLException {

        }

        @Override
        public void updateBlob(String columnLabel, Blob x) throws SQLException {

        }

        @Override
        public void updateClob(int columnIndex, Clob x) throws SQLException {

        }

        @Override
        public void updateClob(String columnLabel, Clob x) throws SQLException {

        }

        @Override
        public void updateArray(int columnIndex, Array x) throws SQLException {

        }

        @Override
        public void updateArray(String columnLabel, Array x) throws SQLException {

        }

        @Override
        public RowId getRowId(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public RowId getRowId(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public void updateRowId(int columnIndex, RowId x) throws SQLException {

        }

        @Override
        public void updateRowId(String columnLabel, RowId x) throws SQLException {

        }

        @Override
        public int getHoldability() throws SQLException {

            return 0;
        }

        @Override
        public boolean isClosed() throws SQLException {

            return false;
        }

        @Override
        public void updateNString(int columnIndex, String nString) throws SQLException {

        }

        @Override
        public void updateNString(String columnLabel, String nString) throws SQLException {

        }

        @Override
        public void updateNClob(int columnIndex, NClob nClob) throws SQLException {

        }

        @Override
        public void updateNClob(String columnLabel, NClob nClob) throws SQLException {

        }

        @Override
        public NClob getNClob(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public NClob getNClob(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public SQLXML getSQLXML(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public SQLXML getSQLXML(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {

        }

        @Override
        public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {

        }

        @Override
        public String getNString(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public String getNString(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public Reader getNCharacterStream(int columnIndex) throws SQLException {

            return null;
        }

        @Override
        public Reader getNCharacterStream(String columnLabel) throws SQLException {

            return null;
        }

        @Override
        public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {

        }

        @Override
        public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {

        }

        @Override
        public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {

        }

        @Override
        public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {

        }

        @Override
        public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {

        }

        @Override
        public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {

        }

        @Override
        public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {

        }

        @Override
        public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {

        }

        @Override
        public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {

        }

        @Override
        public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {

        }

        @Override
        public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {

        }

        @Override
        public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {

        }

        @Override
        public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {

        }

        @Override
        public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {

        }

        @Override
        public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {

        }

        @Override
        public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {

        }

        @Override
        public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {

        }

        @Override
        public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {

        }

        @Override
        public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {

        }

        @Override
        public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {

        }

        @Override
        public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {

        }

        @Override
        public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {

        }

        @Override
        public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateClob(int columnIndex, Reader reader) throws SQLException {

        }

        @Override
        public void updateClob(String columnLabel, Reader reader) throws SQLException {

        }

        @Override
        public void updateNClob(int columnIndex, Reader reader) throws SQLException {

        }

        @Override
        public void updateNClob(String columnLabel, Reader reader) throws SQLException {

        }

        @Override
        public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {

            return null;
        }

        @Override
        public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {

            return null;
        }

    }

}
