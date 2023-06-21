package com.newrelic.agent.bridge.datastore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcDataSourceConnectionFactoryTest {
    DatabaseVendor mockVendor;
    DataSource mockDataSource;
    Connection mockConnection;

    @Before
    public void setup() {
        mockVendor = Mockito.mock(DatabaseVendor.class);
        mockDataSource = Mockito.mock(DataSource.class);
        mockConnection = Mockito.mock(Connection.class);
    }

    @Test(expected = SQLException.class)
    public void getConnection_withNullDriver_throwsSqlException() throws SQLException {
        JdbcDataSourceConnectionFactory factory = new JdbcDataSourceConnectionFactory(mockVendor, null, "user", "pw");
        factory.getConnection();
    }

    @Test
    public void getConnection_withDriverWithUsernameAndPassword_returnsConnection() throws SQLException {
        Mockito.when(mockDataSource.getConnection("url", "pw")).thenReturn(mockConnection);

        JdbcDataSourceConnectionFactory factory = new JdbcDataSourceConnectionFactory(mockVendor, mockDataSource, "url", "pw");
        Connection conn = factory.getConnection();

        Assert.assertEquals(mockConnection, conn);
        Mockito.verify(mockDataSource).getConnection("url", "pw");
    }

    @Test
    public void getConnection_DriverWithoutUsernameAndPassword_returnsConnection() throws SQLException {
        Mockito.when(mockDataSource.getConnection()).thenReturn(mockConnection);

        JdbcDataSourceConnectionFactory factory = new JdbcDataSourceConnectionFactory(mockVendor, mockDataSource);
        Connection conn = factory.getConnection();

        Assert.assertEquals(mockConnection, conn);
        Mockito.verify(mockDataSource).getConnection();
    }

    @Test(expected = SQLException.class)
    public void getConnection_rethrowsSqlException() throws SQLException {
        Mockito.when(mockDataSource.getConnection()).thenThrow(new SQLException("test"));

        JdbcDataSourceConnectionFactory factory = new JdbcDataSourceConnectionFactory(mockVendor, mockDataSource);
        Connection conn = factory.getConnection();

        Mockito.verify(mockDataSource).getConnection();
    }

    @Test
    public void getDatabaseVendor_withVendor_returnsUnknownInstance() {
        JdbcDataSourceConnectionFactory factory = new JdbcDataSourceConnectionFactory(mockVendor, mockDataSource, "user", "pw");
        Assert.assertEquals(mockVendor, factory.getDatabaseVendor());
    }
}
