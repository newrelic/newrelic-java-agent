package com.newrelic.agent.database;

import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;
import org.junit.Test;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreparedStatementExplainPlanExecutorTest {
    @Test
    public void createStatement_returnsStatementInstance() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        Connection mockConn = mock(Connection.class);
        Object [] params = {new Object(), new Object()};

        when(mockConn.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(mockExplain, "SELECT * FROM DUAL", params, RecordSql.raw, null);
        Statement statement = executor.createStatement(mockConn, "sql");
        assertNotNull(statement);
    }

    @Test
    public void executeStatement_returnsResultSet() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Connection mockConnection = mock(Connection.class);
        Object [] params = {new Object(), new Object()};

        when(mockStatement.getConnection()).thenReturn(mockConnection);
        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(mockExplain, "SELECT * FROM DUAL", params, RecordSql.raw, null);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        verify(mockStatement, times(2)).setObject(anyInt(), any());
        assertNotNull(resultSet);

        //Cover branch where params is null
        executor = new PreparedStatementExplainPlanExecutor(mockExplain, "SELECT * FROM DUAL", null, RecordSql.raw, null);
        executor.executeStatement(mockStatement, "sql_not_used");
    }

    @Test
    public void executeStatement_handlesIntegerArrayParameters() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Connection mockConnection = mock(Connection.class);
        Array mockSqlArray = mock(Array.class);

        Integer[] integerArray = {1, 2, 3};
        Object[] params = {integerArray};

        when(mockStatement.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createArrayOf(eq("integer"), any(Object[].class))).thenReturn(mockSqlArray);
        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));

        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT * FROM table WHERE id = ANY(?)", params, RecordSql.raw, null);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        verify(mockConnection).createArrayOf(eq("integer"), any(Object[].class));
        verify(mockStatement).setArray(eq(1), eq(mockSqlArray));
        assertNotNull(resultSet);
    }

    @Test
    public void executeStatement_handlesPrimitiveIntArrayParameters() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Connection mockConnection = mock(Connection.class);
        Array mockSqlArray = mock(Array.class);

        int[] primitiveArray = {1, 2, 3};
        Object[] params = {primitiveArray};

        when(mockStatement.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createArrayOf(eq("integer"), any(Object[].class))).thenReturn(mockSqlArray);
        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));

        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT * FROM table WHERE id = ANY(?)", params, RecordSql.raw, null);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        verify(mockConnection).createArrayOf(eq("integer"), any(Object[].class));
        verify(mockStatement).setArray(eq(1), eq(mockSqlArray));
        assertNotNull(resultSet);
    }

    @Test
    public void executeStatement_handlesStringArrayParameters() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Connection mockConnection = mock(Connection.class);
        Array mockSqlArray = mock(Array.class);

        String[] stringArray = {"a", "b", "c"};
        Object[] params = {stringArray};

        when(mockStatement.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createArrayOf(eq("varchar"), any(Object[].class))).thenReturn(mockSqlArray);
        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));

        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT * FROM table WHERE name = ANY(?)", params, RecordSql.raw, null);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        verify(mockConnection).createArrayOf(eq("varchar"), any(Object[].class));
        verify(mockStatement).setArray(eq(1), eq(mockSqlArray));
        assertNotNull(resultSet);
    }

    @Test
    public void executeStatement_handlesMixedParameters() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Connection mockConnection = mock(Connection.class);
        Array mockSqlArray = mock(Array.class);

        Integer[] integerArray = {1, 2, 3};
        String regularParam = "test";
        Object[] params = {regularParam, integerArray};

        when(mockStatement.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createArrayOf(eq("integer"), any(Object[].class))).thenReturn(mockSqlArray);
        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));

        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT * FROM table WHERE name = ? AND id = ANY(?)", params, RecordSql.raw, null);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        verify(mockStatement).setObject(eq(1), eq(regularParam));
        verify(mockConnection).createArrayOf(eq("integer"), any(Object[].class));
        verify(mockStatement).setArray(eq(2), eq(mockSqlArray));
        assertNotNull(resultSet);
    }

    @Test
    public void executeStatement_handlesLongArrayParameters() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Connection mockConnection = mock(Connection.class);
        Array mockSqlArray = mock(Array.class);

        Long[] longArray = {1L, 2L, 3L};
        Object[] params = {longArray};

        when(mockStatement.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createArrayOf(eq("bigint"), any(Object[].class))).thenReturn(mockSqlArray);
        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));

        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT * FROM table WHERE id = ANY(?)", params, RecordSql.raw, null);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        verify(mockConnection).createArrayOf(eq("bigint"), any(Object[].class));
        verify(mockStatement).setArray(eq(1), eq(mockSqlArray));
        assertNotNull(resultSet);
    }

    @Test
    public void executeStatement_fallbacksToSetObjectWhenArrayConversionFails() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Connection mockConnection = mock(Connection.class);

        Integer[] integerArray = {1, 2, 3};
        Object[] params = {integerArray};

        when(mockStatement.getConnection()).thenReturn(mockConnection);
        // Simulate createArrayOf throwing an exception
        when(mockConnection.createArrayOf(anyString(), any(Object[].class))).thenThrow(new SQLException("Not supported"));
        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));

        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT * FROM table WHERE id = ANY(?)", params, RecordSql.raw, null);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        // Should fall back to setObject when createArrayOf fails
        verify(mockStatement).setObject(eq(1), eq(integerArray));
        assertNotNull(resultSet);
    }

    // Tests for convertToObjectArray method for all primitive types
    @Test
    public void convertToObjectArray_handlesIntArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        int[] primitiveArray = {1, 2, 3, 4, 5};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(5, result.length);
        org.junit.Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    public void convertToObjectArray_handlesLongArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        long[] primitiveArray = {100L, 200L, 300L};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Long[]{100L, 200L, 300L}, result);
    }

    @Test
    public void convertToObjectArray_handlesDoubleArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        double[] primitiveArray = {1.1, 2.2, 3.3};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Double[]{1.1, 2.2, 3.3}, result);
    }

    @Test
    public void convertToObjectArray_handlesFloatArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        float[] primitiveArray = {1.5f, 2.5f, 3.5f};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Float[]{1.5f, 2.5f, 3.5f}, result);
    }

    @Test
    public void convertToObjectArray_handlesBooleanArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        boolean[] primitiveArray = {true, false, true};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Boolean[]{true, false, true}, result);
    }

    @Test
    public void convertToObjectArray_handlesShortArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        short[] primitiveArray = {10, 20, 30};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Short[]{10, 20, 30}, result);
    }

    @Test
    public void convertToObjectArray_handlesByteArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        byte[] primitiveArray = {1, 2, 3};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Byte[]{1, 2, 3}, result);
    }

    @Test
    public void convertToObjectArray_handlesCharArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        char[] primitiveArray = {'a', 'b', 'c'};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Character[]{'a', 'b', 'c'}, result);
    }

    @Test
    public void convertToObjectArray_handlesEmptyArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        int[] primitiveArray = {};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(0, result.length);
    }

    @Test
    public void convertToObjectArray_passesThoughObjectArray() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        Integer[] objectArray = {1, 2, 3};
        Object[] result = executor.convertToObjectArray(objectArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(objectArray, result);
    }

    @Test
    public void convertToObjectArray_handlesMinMaxIntValues() {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(
                mockExplain, "SELECT 1", new Object[]{}, RecordSql.raw, null);

        int[] primitiveArray = {Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
        Object[] result = executor.convertToObjectArray(primitiveArray);

        assertNotNull(result);
        org.junit.Assert.assertEquals(3, result.length);
        org.junit.Assert.assertArrayEquals(new Integer[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}, result);
    }
}
