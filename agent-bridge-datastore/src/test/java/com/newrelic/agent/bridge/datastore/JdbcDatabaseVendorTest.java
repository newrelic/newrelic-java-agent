package com.newrelic.agent.bridge.datastore;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class JdbcDatabaseVendorTest {
    @Test
    public void getName_returnsName() {
        Assert.assertEquals("name", new TestJdbcDatabaseVendor("name", "type", true).getName());
    }

    @Test
    public void getType_returnsType() {
        Assert.assertEquals("type", new TestJdbcDatabaseVendor("name", "type", true).getType());
    }

    @Test
    public void getExplainSupported_returnsExplainsSupported() {
        Assert.assertTrue("type", new TestJdbcDatabaseVendor("name", "type", true).isExplainPlanSupported());
    }

    @Test
    public void getExplainSql_withExplainSupport_returnsExplainSql() throws SQLException {
        TestJdbcDatabaseVendor testJdbcDatabaseVendor = new TestJdbcDatabaseVendor("name", "type", true);
        Assert.assertEquals("EXPLAIN my explain sql", testJdbcDatabaseVendor.getExplainPlanSql("my explain sql"));
    }

    @Test(expected = SQLException.class)
    public void getExplainSql_withoutExplainSupport_throwsException() throws SQLException {
        TestJdbcDatabaseVendor testJdbcDatabaseVendor = new TestJdbcDatabaseVendor("name", "type", false);
        testJdbcDatabaseVendor.getExplainPlanSql("my explain sql");
    }

    @Test
    public void parseExplainPlanResultSet_withPopulatedResultSet_returnsExplainPlanText() throws SQLException {
        TestJdbcDatabaseVendor testJdbcDatabaseVendor = new TestJdbcDatabaseVendor("name", "type", true);

        // Mock a result set with 3 rows and 2 columns per row
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        Mockito.when(mockResultSet.getObject(1)).thenReturn("col1");
        Mockito.when(mockResultSet.getObject(2)).thenReturn("col2");

        Collection<Collection<Object>> response = testJdbcDatabaseVendor.parseExplainPlanResultSet(2, mockResultSet, RecordSql.off);
        Assert.assertEquals(3, response.size());

        Iterator<Collection<Object>> iterator = response.iterator();
        iterator.forEachRemaining((columnCollection) -> {
            LinkedList<Object> asLinkedList = (LinkedList<Object>) columnCollection;
            Assert.assertEquals(2, asLinkedList.size());
            Assert.assertEquals("col1", asLinkedList.get(0).toString());
            Assert.assertEquals("col2", asLinkedList.get(1).toString());
        });
    }

    @Test
    public void getExplainPlanFormat_returnsText() {
        TestJdbcDatabaseVendor testJdbcDatabaseVendor = new TestJdbcDatabaseVendor("name", "type", true);
        Assert.assertEquals("text", testJdbcDatabaseVendor.getExplainPlanFormat());
    }

    private static class TestJdbcDatabaseVendor extends JdbcDatabaseVendor {

        public TestJdbcDatabaseVendor(String name, String type, boolean explainSupported) {
            super(name, type, explainSupported);
        }

        public DatastoreVendor getDatastoreVendor() {
            return DatastoreVendor.Derby;
        }
    }
}
