package com.newrelic.agent.database;

import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(mockExplain, "SELECT * FROM DUAL", params, RecordSql.raw);
        Statement statement = executor.createStatement(mockConn, "sql");
        assertNotNull(statement);
    }

    @Test
    public void executeStatement_returnsResultSet() throws SQLException {
        SqlTracerExplainInfo mockExplain = mock(SqlTracerExplainInfo.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        Object [] params = {new Object(), new Object()};

        when(mockStatement.executeQuery()).thenReturn(mock(ResultSet.class));
        PreparedStatementExplainPlanExecutor executor = new PreparedStatementExplainPlanExecutor(mockExplain, "SELECT * FROM DUAL", params, RecordSql.raw);
        ResultSet resultSet = executor.executeStatement(mockStatement, "sql_not_used");

        verify(mockStatement, times(2)).setObject(anyInt(), any());
        assertNotNull(resultSet);

        //Cover branch where params is null
        executor = new PreparedStatementExplainPlanExecutor(mockExplain, "SELECT * FROM DUAL", null, RecordSql.raw);
        executor.executeStatement(mockStatement, "sql_not_used");
    }
}
