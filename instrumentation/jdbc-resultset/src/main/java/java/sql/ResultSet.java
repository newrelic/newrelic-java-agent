/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Calendar;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.Interface)
public abstract class ResultSet {
    @NewField
    private static final String METRIC_NAME = "Datastore/ResultSet";

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean next();

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract String getString(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean getBoolean(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract byte getByte(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract short getShort(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int getInt(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract long getLong(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract float getFloat(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract double getDouble(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract byte[] getBytes(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Date getDate(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Time getTime(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Timestamp getTimestamp(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.InputStream getAsciiStream(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.InputStream getBinaryStream(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract String getString(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean getBoolean(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract byte getByte(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract short getShort(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int getInt(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract long getLong(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract float getFloat(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract double getDouble(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract byte[] getBytes(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Date getDate(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Time getTime(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Timestamp getTimestamp(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.InputStream getAsciiStream(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.InputStream getUnicodeStream(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.InputStream getBinaryStream(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract SQLWarning getWarnings() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void clearWarnings() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract String getCursorName() throws SQLException;

    // abstract ResultSetMetaData getMetaData() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Object getObject(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Object getObject(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int findColumn(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.Reader getCharacterStream(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.Reader getCharacterStream(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract BigDecimal getBigDecimal(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract BigDecimal getBigDecimal(String columnLabel) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean isBeforeFirst() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean isAfterLast() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean isFirst() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean isLast() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void beforeFirst() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void afterLast() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean first() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean last() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int getRow() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean absolute(int row) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean relative(int rows) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean previous() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void setFetchDirection(int direction) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int getFetchDirection() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void setFetchSize(int rows) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int getFetchSize() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int getType() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean rowUpdated() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean rowInserted() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean rowDeleted() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNull(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBoolean(int columnIndex, boolean x) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateByte(int columnIndex, byte x) throws SQLException;

    /**
     * Updates the designated column with a <code>short</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateShort(int columnIndex, short x) throws SQLException;

    /**
     * Updates the designated column with an <code>int</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateInt(int columnIndex, int x) throws SQLException;

    /**
     * Updates the designated column with a <code>long</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateLong(int columnIndex, long x) throws SQLException;

    /**
     * Updates the designated column with a <code>float</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateFloat(int columnIndex, float x) throws SQLException;

    /**
     * Updates the designated column with a <code>double</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateDouble(int columnIndex, double x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.math.BigDecimal</code> value. The updater methods are used to
     * update column values in the current row or the insert row. The updater methods do not update the underlying
     * database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateString(int columnIndex, String x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> array value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBytes(int columnIndex, byte x[]) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateDate(int columnIndex, java.sql.Date x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateTime(int columnIndex, java.sql.Time x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code> value. The updater methods are used to
     * update column values in the current row or the insert row. The updater methods do not update the underlying
     * database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateTimestamp(int columnIndex, java.sql.Timestamp x) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have the specified number of bytes. The
     * updater methods are used to update column values in the current row or the insert row. The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have the specified number of bytes. The
     * updater methods are used to update column values in the current row or the insert row. The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have the specified number of bytes. The
     * updater methods are used to update column values in the current row or the insert row. The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateCharacterStream(int columnIndex, java.io.Reader x, int length) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * <p>
     * If the second argument is an <code>InputStream</code> then the stream must contain the number of bytes specified
     * by scaleOrLength. If the second argument is a <code>Reader</code> then the reader must contain the number of
     * characters specified by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the statement is executed.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> , this is the number of digits after the
     *        decimal point. For Java Object types <code>InputStream</code> and <code>Reader</code>, this is the length
     *        of the data in the stream or reader. For all other types, this value will be ignored.
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateObject(int columnIndex, Object x) throws SQLException;

    /**
     * Updates the designated column with a <code>null</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNull(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>boolean</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBoolean(String columnLabel, boolean x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateByte(String columnLabel, byte x) throws SQLException;

    /**
     * Updates the designated column with a <code>short</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateShort(String columnLabel, short x) throws SQLException;

    /**
     * Updates the designated column with an <code>int</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateInt(String columnLabel, int x) throws SQLException;

    /**
     * Updates the designated column with a <code>long</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateLong(String columnLabel, long x) throws SQLException;

    /**
     * Updates the designated column with a <code>float </code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateFloat(String columnLabel, float x) throws SQLException;

    /**
     * Updates the designated column with a <code>double</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateDouble(String columnLabel, double x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.BigDecimal</code> value. The updater methods are used to
     * update column values in the current row or the insert row. The updater methods do not update the underlying
     * database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateString(String columnLabel, String x) throws SQLException;

    /**
     * Updates the designated column with a byte array value.
     * 
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBytes(String columnLabel, byte x[]) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateDate(String columnLabel, java.sql.Date x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateTime(String columnLabel, java.sql.Time x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code> value. The updater methods are used to
     * update column values in the current row or the insert row. The updater methods do not update the underlying
     * database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateTimestamp(String columnLabel, java.sql.Timestamp x) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have the specified number of bytes. The
     * updater methods are used to update column values in the current row or the insert row. The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateAsciiStream(String columnLabel, java.io.InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have the specified number of bytes. The
     * updater methods are used to update column values in the current row or the insert row. The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBinaryStream(String columnLabel, java.io.InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have the specified number of bytes. The
     * updater methods are used to update column values in the current row or the insert row. The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateCharacterStream(String columnLabel, java.io.Reader reader, int length)
            throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * <p>
     * If the second argument is an <code>InputStream</code> then the stream must contain the number of bytes specified
     * by scaleOrLength. If the second argument is a <code>Reader</code> then the reader must contain the number of
     * characters specified by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the statement is executed.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> , this is the number of digits after the
     *        decimal point. For Java Object types <code>InputStream</code> and <code>Reader</code>, this is the length
     *        of the data in the stream or reader. For all other types, this value will be ignored.
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateObject(String columnLabel, Object x) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void insertRow() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateRow() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void deleteRow() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void refreshRow() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void cancelRowUpdates() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void moveToInsertRow() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void moveToCurrentRow() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Statement getStatement() throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Object getObject(int columnIndex, java.util.Map<String, Class<?>> map) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Ref getRef(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Blob getBlob(int columnIndex) throws SQLException;

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Clob getClob(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as an
     * <code>Array</code> object in the Java programming language.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return an <code>Array</code> object representing the SQL <code>ARRAY</code> value in the specified column
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Array getArray(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as an
     * <code>Object</code> in the Java programming language. If the value is an SQL <code>NULL</code>, the driver
     * returns a Java <code>null</code>. This method uses the specified <code>Map</code> object for custom mapping if
     * appropriate.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param map a <code>java.util.Map</code> object that contains the mapping from SQL type names to classes in the
     *        Java programming language
     * @return an <code>Object</code> representing the SQL value in the specified column
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Object getObject(String columnLabel, java.util.Map<String, Class<?>> map) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>Ref</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return a <code>Ref</code> object representing the SQL <code>REF</code> value in the specified column
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Ref getRef(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>Blob</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return a <code>Blob</code> object representing the SQL <code>BLOB</code> value in the specified column
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Blob getBlob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>Clob</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return a <code>Clob</code> object representing the SQL <code>CLOB</code> value in the specified column
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Clob getClob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as an
     * <code>Array</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return an <code>Array</code> object representing the SQL <code>ARRAY</code> value in the specified column
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract Array getArray(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.Date</code> object in the Java programming language. This method uses the given calendar to
     * construct an appropriate millisecond value for the date if the underlying database does not store timezone
     * information.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object; if the value is SQL <code>NULL</code>, the value
     *         returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.Date</code> object in the Java programming language. This method uses the given calendar to
     * construct an appropriate millisecond value for the date if the underlying database does not store timezone
     * information.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param cal the <code>java.util.Calendar</code> object to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object; if the value is SQL <code>NULL</code>, the value
     *         returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Date getDate(String columnLabel, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.Time</code> object in the Java programming language. This method uses the given calendar to
     * construct an appropriate millisecond value for the time if the underlying database does not store timezone
     * information.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object; if the value is SQL <code>NULL</code>, the value
     *         returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Time getTime(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.Time</code> object in the Java programming language. This method uses the given calendar to
     * construct an appropriate millisecond value for the time if the underlying database does not store timezone
     * information.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param cal the <code>java.util.Calendar</code> object to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object; if the value is SQL <code>NULL</code>, the value
     *         returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Time getTime(String columnLabel, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.Timestamp</code> object in the Java programming language. This method uses the given calendar to
     * construct an appropriate millisecond value for the timestamp if the underlying database does not store timezone
     * information.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object to use in constructing the timestamp
     * @return the column value as a <code>java.sql.Timestamp</code> object; if the value is SQL <code>NULL</code>, the
     *         value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.Timestamp</code> object in the Java programming language. This method uses the given calendar to
     * construct an appropriate millisecond value for the timestamp if the underlying database does not store timezone
     * information.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param cal the <code>java.util.Calendar</code> object to use in constructing the date
     * @return the column value as a <code>java.sql.Timestamp</code> object; if the value is SQL <code>NULL</code>, the
     *         value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid or if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.sql.Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.net.URL</code> object in the Java programming language.
     * 
     * @param columnIndex the index of the column 1 is the first, 2 is the second,...
     * @return the column value as a <code>java.net.URL</code> object; if the value is SQL <code>NULL</code>, the value
     *         returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; this method is called
     *            on a closed result set or if a URL is malformed
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.net.URL getURL(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.net.URL</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return the column value as a <code>java.net.URL</code> object; if the value is SQL <code>NULL</code>, the value
     *         returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; this method is called
     *            on a closed result set or if a URL is malformed
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.net.URL getURL(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Ref</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateRef(int columnIndex, java.sql.Ref x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Ref</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateRef(String columnLabel, java.sql.Ref x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBlob(int columnIndex, java.sql.Blob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBlob(String columnLabel, java.sql.Blob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateClob(int columnIndex, java.sql.Clob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateClob(String columnLabel, java.sql.Clob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Array</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateArray(int columnIndex, java.sql.Array x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Array</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateArray(String columnLabel, java.sql.Array x) throws SQLException;

    // ------------------------- JDBC 4.0 -----------------------------------

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.RowId</code> object in the Java programming language.
     * 
     * @param columnIndex the first column is 1, the second 2, ...
     * @return the column value; if the value is a SQL <code>NULL</code> the value returned is <code>null</code>
     * @throws SQLException if the columnIndex is not valid; if a database access error occurs or this method is called
     *         on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract RowId getRowId(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.sql.RowId</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return the column value ; if the value is a SQL <code>NULL</code> the value returned is <code>null</code>
     * @throws SQLException if the columnLabel is not valid; if a database access error occurs or this method is called
     *         on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract RowId getRowId(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>RowId</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second 2, ...
     * @param x the column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateRowId(int columnIndex, RowId x) throws SQLException;

    /**
     * Updates the designated column with a <code>RowId</code> value. The updater methods are used to update column
     * values in the current row or the insert row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateRowId(String columnLabel, RowId x) throws SQLException;

    /**
     * Retrieves the holdability of this <code>ResultSet</code> object
     * 
     * @return either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @throws SQLException if a database access error occurs or this method is called on a closed result set
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract int getHoldability() throws SQLException;

    /**
     * Retrieves whether this <code>ResultSet</code> object has been closed. A <code>ResultSet</code> is closed if the
     * method close has been called on it, or if it is automatically closed.
     * 
     * @return true if this <code>ResultSet</code> object is closed; false if it is still open
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract boolean isClosed() throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value. It is intended for use when updating
     * <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns. The updater methods are used to
     * update column values in the current row or the insert row. The updater methods do not update the underlying
     * database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second 2, ...
     * @param nString the value for the column to be updated
     * @throws SQLException if the columnIndex is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set; the result set concurrency is <code>CONCUR_READ_ONLY</code> or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNString(int columnIndex, String nString) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value. It is intended for use when updating
     * <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns. The updater methods are used to
     * update column values in the current row or the insert row. The updater methods do not update the underlying
     * database; instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param nString the value for the column to be updated
     * @throws SQLException if the columnLabel is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set; the result set concurrency is <CODE>CONCUR_READ_ONLY</code> or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNString(String columnLabel, String nString) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.NClob</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnIndex the first column is 1, the second 2, ...
     * @param nClob the value for the column to be updated
     * @throws SQLException if the columnIndex is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set; if a database access error occurs or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNClob(int columnIndex, NClob nClob) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.NClob</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param nClob the value for the column to be updated
     * @throws SQLException if the columnLabel is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set; if a database access error occurs or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNClob(String columnLabel, NClob nClob) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>NClob</code> object in the Java programming language.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>NClob</code> object representing the SQL <code>NCLOB</code> value in the specified column
     * @exception SQLException if the columnIndex is not valid; if the driver does not support national character sets;
     *            if the driver can detect that a data conversion error could occur; this method is called on a closed
     *            result set or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract NClob getNClob(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>NClob</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return a <code>NClob</code> object representing the SQL <code>NCLOB</code> value in the specified column
     * @exception SQLException if the columnLabel is not valid; if the driver does not support national character sets;
     *            if the driver can detect that a data conversion error could occur; this method is called on a closed
     *            result set or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract NClob getNClob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException if the columnIndex is not valid; if a database access error occurs or this method is called
     *         on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract SQLXML getSQLXML(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException if the columnLabel is not valid; if a database access error occurs or this method is called
     *         on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract SQLXML getSQLXML(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.SQLXML</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * <p>
     * 
     * @param columnIndex the first column is 1, the second 2, ...
     * @param xmlObject the value for the column to be updated
     * @throws SQLException if the columnIndex is not valid; if a database access error occurs; this method is called on
     *         a closed result set; the <code>java.xml.transform.Result</code>, <code>Writer</code> or
     *         <code>OutputStream</code> has not been closed for the <code>SQLXML</code> object; if there is an error
     *         processing the XML value or the result set concurrency is <code>CONCUR_READ_ONLY</code>. The
     *         <code>getCause</code> method of the exception may provide a more detailed exception, for example, if the
     *         stream does not contain valid XML.
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.SQLXML</code> value. The updater methods are used to update
     * column values in the current row or the insert row. The updater methods do not update the underlying database;
     * instead the <code>updateRow</code> or <code>insertRow</code> methods are called to update the database.
     * <p>
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param xmlObject the column value
     * @throws SQLException if the columnLabel is not valid; if a database access error occurs; this method is called on
     *         a closed result set; the <code>java.xml.transform.Result</code>, <code>Writer</code> or
     *         <code>OutputStream</code> has not been closed for the <code>SQLXML</code> object; if there is an error
     *         processing the XML value or the result set concurrency is <code>CONCUR_READ_ONLY</code>. The
     *         <code>getCause</code> method of the exception may provide a more detailed exception, for example, if the
     *         stream does not contain valid XML.
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>String</code> in the Java programming language. It is intended for use when accessing <code>NCHAR</code>,
     * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract String getNString(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>String</code> in the Java programming language. It is intended for use when accessing <code>NCHAR</code>,
     * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract String getNString(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object. It is intended for use when accessing <code>NCHAR</code>,
     * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * 
     * @return a <code>java.io.Reader</code> object that contains the column value; if the value is SQL
     *         <code>NULL</code>, the value returned is <code>null</code> in the Java programming language.
     * @param columnIndex the first column is 1, the second is 2, ...
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.Reader getNCharacterStream(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object. It is intended for use when accessing <code>NCHAR</code>,
     * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @return a <code>java.io.Reader</code> object that contains the column value; if the value is SQL
     *         <code>NULL</code>, the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract java.io.Reader getNCharacterStream(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have the specified number of bytes. The
     * driver does the necessary conversion from Java character format to the national character set in the database. It
     * is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNCharacterStream(int columnIndex, java.io.Reader x, long length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have the specified number of bytes. The
     * driver does the necessary conversion from Java character format to the national character set in the database. It
     * is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNCharacterStream(String columnLabel, java.io.Reader reader, long length)
            throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateAsciiStream(int columnIndex, java.io.InputStream x, long length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBinaryStream(int columnIndex, java.io.InputStream x, long length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateCharacterStream(int columnIndex, java.io.Reader x, long length) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateAsciiStream(String columnLabel, java.io.InputStream x, long length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBinaryStream(String columnLabel, java.io.InputStream x, long length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateCharacterStream(String columnLabel, java.io.Reader reader, long length)
            throws SQLException;

    /**
     * Updates the designated column using the given input stream, which will have the specified number of bytes.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter value to.
     * @param length the number of bytes in the parameter data.
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException;

    /**
     * Updates the designated column using the given input stream, which will have the specified number of bytes.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param inputStream An object that contains the data to set the parameter value to.
     * @param length the number of bytes in the parameter data.
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code> object, which is the given number of characters
     * long. When a very large UNICODE value is input to a <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a <code>java.io.Reader</code> object. The JDBC driver will do any necessary conversion from
     * UNICODE to the database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateClob(int columnIndex, Reader reader, long length) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code> object, which is the given number of characters
     * long. When a very large UNICODE value is input to a <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a <code>java.io.Reader</code> object. The JDBC driver will do any necessary conversion from
     * UNICODE to the database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateClob(String columnLabel, Reader reader, long length) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code> object, which is the given number of characters
     * long. When a very large UNICODE value is input to a <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a <code>java.io.Reader</code> object. The JDBC driver will do any necessary conversion from
     * UNICODE to the database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnIndex the first column is 1, the second 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if the columnIndex is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set, if a database access error occurs or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNClob(int columnIndex, Reader reader, long length) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code> object, which is the given number of characters
     * long. When a very large UNICODE value is input to a <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a <code>java.io.Reader</code> object. The JDBC driver will do any necessary conversion from
     * UNICODE to the database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if the columnLabel is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set; if a database access error occurs or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNClob(String columnLabel, Reader reader, long length) throws SQLException;

    // ---

    /**
     * Updates the designated column with a character stream value. The data will be read from the stream as needed
     * until end-of-stream is reached. The driver does the necessary conversion from Java character format to the
     * national character set in the database. It is intended for use when updating <code>NCHAR</code>,
     * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateNCharacterStream</code> which takes a length parameter.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNCharacterStream(int columnIndex, java.io.Reader x) throws SQLException;

    /**
     * Updates the designated column with a character stream value. The data will be read from the stream as needed
     * until end-of-stream is reached. The driver does the necessary conversion from Java character format to the
     * national character set in the database. It is intended for use when updating <code>NCHAR</code>,
     * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateNCharacterStream</code> which takes a length parameter.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value. The data will be read from the stream as needed until
     * end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateAsciiStream</code> which takes a length parameter.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateAsciiStream(int columnIndex, java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a binary stream value. The data will be read from the stream as needed until
     * end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateBinaryStream</code> which takes a length parameter.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBinaryStream(int columnIndex, java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a character stream value. The data will be read from the stream as needed
     * until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateCharacterStream</code> which takes a length parameter.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateCharacterStream(int columnIndex, java.io.Reader x) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value. The data will be read from the stream as needed until
     * end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateAsciiStream</code> which takes a length parameter.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateAsciiStream(String columnLabel, java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a binary stream value. The data will be read from the stream as needed until
     * end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateBinaryStream</code> which takes a length parameter.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBinaryStream(String columnLabel, java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a character stream value. The data will be read from the stream as needed
     * until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateCharacterStream</code> which takes a length parameter.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException;

    /**
     * Updates the designated column using the given input stream. The data will be read from the stream as needed until
     * end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateBlob</code> which takes a length parameter.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter value to.
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBlob(int columnIndex, InputStream inputStream) throws SQLException;

    /**
     * Updates the designated column using the given input stream. The data will be read from the stream as needed until
     * end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateBlob</code> which takes a length parameter.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param inputStream An object that contains the data to set the parameter value to.
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateBlob(String columnLabel, InputStream inputStream) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code> object. The data will be read from the stream
     * as needed until end-of-stream is reached. The JDBC driver will do any necessary conversion from UNICODE to the
     * database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateClob</code> which takes a length parameter.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateClob(int columnIndex, Reader reader) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code> object. The data will be read from the stream
     * as needed until end-of-stream is reached. The JDBC driver will do any necessary conversion from UNICODE to the
     * database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateClob</code> which takes a length parameter.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs; the result set
     *            concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateClob(String columnLabel, Reader reader) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code>
     * 
     * The data will be read from the stream as needed until end-of-stream is reached. The JDBC driver will do any
     * necessary conversion from UNICODE to the database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateNClob</code> which takes a length parameter.
     * 
     * @param columnIndex the first column is 1, the second 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if the columnIndex is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set, if a database access error occurs or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNClob(int columnIndex, Reader reader) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code> object. The data will be read from the stream
     * as needed until end-of-stream is reached. The JDBC driver will do any necessary conversion from UNICODE to the
     * database char format.
     * 
     * <p>
     * The updater methods are used to update column values in the current row or the insert row. The updater methods do
     * not update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code> methods are
     * called to update the database.
     * 
     * <P>
     * <B>Note:</B> Consult your JDBC driver documentation to determine if it might be more efficient to use a version
     * of <code>updateNClob</code> which takes a length parameter.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if the columnLabel is not valid; if the driver does not support national character sets; if
     *         the driver can detect that a data conversion error could occur; this method is called on a closed result
     *         set; if a database access error occurs or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME, leaf = true)
    public abstract void updateNClob(String columnLabel, Reader reader) throws SQLException;

    // ------------------------- JDBC 4.1 -----------------------------------

    /**
     * <p>
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object and will
     * convert from the SQL type of the column to the requested Java data type, if the conversion is supported. If the
     * conversion is not supported or null is specified for the type, a <code>SQLException</code> is thrown.
     * <p>
     * At a minimum, an implementation must support the conversions defined in Appendix B, Table B-3 and conversion of
     * appropriate user defined SQL types to a Java type which implements {@code SQLData}, or {@code Struct}. Additional
     * conversions may be supported and are vendor defined.
     * 
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param type Class representing the Java data type to convert the designated column to.
     * @return an instance of {@code type} holding the column value
     * @throws SQLException if conversion is not supported, type is null or another error occurs. The getCause() method
     *         of the exception may provide a more detailed exception, for example, if a conversion error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.7
     */
    // public <T> T getObject(int columnIndex, Class<T> type) throws SQLException;

    /**
     * <p>
     * Retrieves the value of the designated column in the current row of this <code>ResultSet</code> object and will
     * convert from the SQL type of the column to the requested Java data type, if the conversion is supported. If the
     * conversion is not supported or null is specified for the type, a <code>SQLException</code> is thrown.
     * <p>
     * At a minimum, an implementation must support the conversions defined in Appendix B, Table B-3 and conversion of
     * appropriate user defined SQL types to a Java type which implements {@code SQLData}, or {@code Struct}. Additional
     * conversions may be supported and are vendor defined.
     * 
     * @param columnLabel the label for the column specified with the SQL AS clause. If the SQL AS clause was not
     *        specified, then the label is the name of the column
     * @param type Class representing the Java data type to convert the designated column to.
     * @return an instance of {@code type} holding the column value
     * @throws SQLException if conversion is not supported, type is null or another error occurs. The getCause() method
     *         of the exception may provide a more detailed exception, for example, if a conversion error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.7
     */
    // public <T> T getObject(String columnLabel, Class<T> type) throws SQLException;

}
