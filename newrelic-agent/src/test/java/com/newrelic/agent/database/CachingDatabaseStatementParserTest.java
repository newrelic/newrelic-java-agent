package com.newrelic.agent.database;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import org.junit.Test;

import java.sql.ResultSetMetaData;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CachingDatabaseStatementParserTest {
    @Test
    public void getParsedDatabaseStatement_cachesSqlStatement() {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);

        when(mockParser.getParsedDatabaseStatement(any(), eq("sql"), any())).thenReturn(new ParsedDatabaseStatement("tablename", "select", true));

        // Initial call will cache the result
        ParsedDatabaseStatement statement = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "sql", mock(ResultSetMetaData.class));
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("sql"), any());

        // Subsequent calls will retrieve the value from the cache and not call the databaseStatementParser again
        assertEquals(statement, cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "sql", mock(ResultSetMetaData.class)));
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("sql"), any());
    }

    @Test
    public void getParsedDatabaseStatement_withNullStatement_returnsUnparseableStatementInstance() {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);
        assertEquals(DatabaseStatementParser.UNPARSEABLE_STATEMENT, cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), null, null));
    }
}