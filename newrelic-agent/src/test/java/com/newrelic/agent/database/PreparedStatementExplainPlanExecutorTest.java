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
}
