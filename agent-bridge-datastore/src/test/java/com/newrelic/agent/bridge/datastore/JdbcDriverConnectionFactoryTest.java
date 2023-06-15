package com.newrelic.agent.bridge.datastore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcDriverConnectionFactoryTest {
    DatabaseVendor mockVendor;
    Driver mockDriver;
    Connection mockConnection;

    @Before
    public void setup() {
        mockVendor = Mockito.mock(DatabaseVendor.class);
        mockDriver = Mockito.mock(Driver.class);
        mockConnection = Mockito.mock(Connection.class);
    }

    @Test(expected = SQLException.class)
    public void getConnection_withNullDriver_throwsSqlException() throws SQLException {
        JdbcDriverConnectionFactory factory = new JdbcDriverConnectionFactory(mockVendor, null, "url", new Properties());
        factory.getConnection();
    }

    @Test
    public void getConnection_withDriver_attemptsConnection() throws SQLException {
        Properties emptyProps = new Properties();
        Mockito.when(mockDriver.connect("url", emptyProps)).thenReturn(mockConnection);

        JdbcDriverConnectionFactory factory = new JdbcDriverConnectionFactory(mockVendor, mockDriver, "url", emptyProps);
        Connection conn = factory.getConnection();

        Assert.assertEquals(mockConnection, conn);
        Mockito.verify(mockDriver).connect("url", emptyProps);
    }

    @Test
    public void getDatabaseVendor_withNullVendor_returnsUnknownInstance() {
        JdbcDriverConnectionFactory factory = new JdbcDriverConnectionFactory(null, mockDriver, "url", new Properties());
        Assert.assertEquals(UnknownDatabaseVendor.INSTANCE, factory.getDatabaseVendor());
    }

    @Test
    public void getDatabaseVendor_withVendor_returnsUnknownInstance() {
        JdbcDriverConnectionFactory factory = new JdbcDriverConnectionFactory(mockVendor, mockDriver, "url", new Properties());
        Assert.assertEquals(mockVendor, factory.getDatabaseVendor());
    }
}
