package com.newrelic.agent.database;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.util.Caffeine2CollectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CachingDatabaseStatementParserTest {

    @Before
    public void setUp() {
        // Set up a real collection factory that supports weak keys and max size
        AgentBridge.collectionFactory = new Caffeine2CollectionFactory();
    }

    @After
    public void tearDown() {
        // Reset to avoid affecting other tests
        AgentBridge.collectionFactory = new com.newrelic.agent.bridge.DefaultCollectionFactory();
    }

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

    @Test
    public void multipleDifferentSqlStatements_cacheSeparately() {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);

        ParsedDatabaseStatement stmt1 = new ParsedDatabaseStatement("table1", "select", true);
        ParsedDatabaseStatement stmt2 = new ParsedDatabaseStatement("table2", "insert", false);
        ParsedDatabaseStatement stmt3 = new ParsedDatabaseStatement("table3", "update", true);

        when(mockParser.getParsedDatabaseStatement(any(), eq("SELECT * FROM table1"), any())).thenReturn(stmt1);
        when(mockParser.getParsedDatabaseStatement(any(), eq("INSERT INTO table2"), any())).thenReturn(stmt2);
        when(mockParser.getParsedDatabaseStatement(any(), eq("UPDATE table3"), any())).thenReturn(stmt3);

        ParsedDatabaseStatement result1 = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "SELECT * FROM table1", null);
        ParsedDatabaseStatement result2 = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "INSERT INTO table2", null);
        ParsedDatabaseStatement result3 = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "UPDATE table3", null);

        // Verify each was parsed once
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("SELECT * FROM table1"), any());
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("INSERT INTO table2"), any());
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("UPDATE table3"), any());

        // Verify correct results
        assertEquals(stmt1, result1);
        assertEquals(stmt2, result2);
        assertEquals(stmt3, result3);

        // Call again to verify caching works for all three
        assertEquals(stmt1, cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "SELECT * FROM table1", null));
        assertEquals(stmt2, cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "INSERT INTO table2", null));
        assertEquals(stmt3, cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "UPDATE table3", null));
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("SELECT * FROM table1"), any());
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("INSERT INTO table2"), any());
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("UPDATE table3"), any());
    }

    @Test
    public void cacheConsistency_sameObjectReturnedForSameSql() {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);

        ParsedDatabaseStatement stmt = new ParsedDatabaseStatement("users", "select", true);
        when(mockParser.getParsedDatabaseStatement(any(), eq("SELECT * FROM users"), any())).thenReturn(stmt);

        // Get the statement multiple times
        ParsedDatabaseStatement result1 = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "SELECT * FROM users", null);
        ParsedDatabaseStatement result2 = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "SELECT * FROM users", null);
        ParsedDatabaseStatement result3 = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "SELECT * FROM users", null);

        // All should return the exact same cached object
        assertSame("Should return same cached object", result1, result2);
        assertSame("Should return same cached object", result2, result3);

        // Parser should only be called once
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("SELECT * FROM users"), any());
    }

    @Test
    public void lazyInitialization_cacheCreatedOnFirstUse() {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);

        // Create parser - cache should not be initialized yet
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);

        ParsedDatabaseStatement stmt = new ParsedDatabaseStatement("products", "select", true);
        when(mockParser.getParsedDatabaseStatement(any(), eq("SELECT * FROM products"), any())).thenReturn(stmt);

        // First use should trigger cache creation and work correctly
        ParsedDatabaseStatement result = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "SELECT * FROM products", null);
        assertNotNull("Should successfully create cache and return result", result);
        assertEquals(stmt, result);

        // Subsequent use should use the initialized cache
        ParsedDatabaseStatement result2 = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "SELECT * FROM products", null);
        assertEquals(stmt, result2);

        // Parser should only be called once
        verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq("SELECT * FROM products"), any());
    }

    @Test
    public void concurrentAccess_isThreadSafe() throws InterruptedException {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);
        DatabaseVendor sharedVendor = mock(DatabaseVendor.class); // Shared mock to avoid thread-safety issues

        // Set up mock to return different statements for different SQL
        for (int i = 0; i < 50; i++) {
            String sql = "SELECT * FROM table" + i;
            ParsedDatabaseStatement stmt = new ParsedDatabaseStatement("table" + i, "select", true);
            when(mockParser.getParsedDatabaseStatement(any(), eq(sql), any())).thenReturn(stmt);
        }

        int threadCount = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int i = 0; i < operationsPerThread; i++) {
                        String sql = "SELECT * FROM table" + i;
                        ParsedDatabaseStatement result = cache.getParsedDatabaseStatement(sharedVendor, sql, null);

                        if (result != null && result.getOperation().equals("select")) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);

        executor.shutdown();
        assertEquals(true, completed);
        assertEquals("All operations should succeed", threadCount * operationsPerThread, successCount.get());
    }

    @Test
    public void exceptionDuringParsing_returnsUnparseableStatement() {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);

        // Mock parser throws RuntimeException
        when(mockParser.getParsedDatabaseStatement(any(), eq("INVALID SQL"), any()))
                .thenThrow(new RuntimeException("Parse error"));

        ParsedDatabaseStatement result = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), "INVALID SQL", null);

        assertEquals("Should return UNPARSEABLE_STATEMENT on exception",
                     DatabaseStatementParser.UNPARSEABLE_STATEMENT, result);
    }

    @Test
    public void cacheGrowthBeyondInitialCapacity() {
        DatabaseStatementParser mockParser = mock(DatabaseStatementParser.class);
        CachingDatabaseStatementParser cache = new CachingDatabaseStatementParser(mockParser);

        List<String> sqlStatements = new ArrayList<>();

        // Add 100 different SQL statements (more than typical initial capacity)
        for (int i = 0; i < 100; i++) {
            String sql = "SELECT * FROM table_" + i + " WHERE id = " + i;
            sqlStatements.add(sql);
            ParsedDatabaseStatement stmt = new ParsedDatabaseStatement("table_" + i, "select", true);
            when(mockParser.getParsedDatabaseStatement(any(), eq(sql), any())).thenReturn(stmt);
        }

        // Parse all statements
        for (String sql : sqlStatements) {
            ParsedDatabaseStatement result = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), sql, null);
            assertNotNull("Should successfully cache statement " + sql, result);
        }

        // Verify each was parsed exactly once
        for (String sql : sqlStatements) {
            verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq(sql), any());
        }

        // Access all statements again to verify they're still cached
        for (String sql : sqlStatements) {
            ParsedDatabaseStatement result = cache.getParsedDatabaseStatement(mock(DatabaseVendor.class), sql, null);
            assertNotNull("Should retrieve cached statement " + sql, result);
        }

        // Verify still only parsed once each (no additional calls)
        for (String sql : sqlStatements) {
            verify(mockParser, times(1)).getParsedDatabaseStatement(any(), eq(sql), any());
        }
    }
}