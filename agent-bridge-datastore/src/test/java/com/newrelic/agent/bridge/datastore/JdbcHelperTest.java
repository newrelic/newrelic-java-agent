/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JdbcHelperTest {

    @Test
    public void testParseInstanceIdentifier() {
        // derby
        assertEquals("myDB", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:myDB"));
        assertEquals("d1b", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:memory:d1b"));
        assertEquals("C:\\myderby\\db", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:memory:C:\\myderby\\db"));
        assertEquals("/home/myname/db", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:memory:/home/myname/db"));
        assertEquals("/home/myname/../myname/db", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:memory:/home/myname/../myname/db"));
        assertEquals("~/sample", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:file:~/sample?username=user;password=passwd"));
        assertEquals("/var/db/sample", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:directory:/var/db/sample"));
        assertEquals("../otherDirectory/myDB", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:../otherDirectory/myDB"));
        assertEquals("C:/sample", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:file:C:/sample;password=passwd"));

        // h2
        assertEquals("mydbname", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:mem:mydbname?username=MyUsername&password=ThisisSecret"));
        assertEquals("C:/data", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:C:/data"));
        assertEquals("~/TEST1234", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:file:~/TEST1234?password=s3kr3t"));
        assertEquals("~/test", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:~/test;USERNAME=user,PASSWORD=s3kr3t"));
        assertEquals("/data/sample", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:file:/data/sample"));
        assertEquals("C:/data/sample", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:file:C:/data/sample"));
        assertEquals("<databaseName>", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:mem:<databaseName>"));
        assertEquals("mydbname", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:mem:mydbname"));
        assertEquals("play", JdbcHelper.parseInMemoryIdentifier("jdbc:h2:mem:play;MODE=MYSQL"));

        // hsqldb
        assertEquals("/opt/db/testdb", JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb:file:/opt/db/testdb, "));
        assertEquals("mymemdb", JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb:mem:mymemdb, SA, "));
        assertEquals("org.my.path.resdb", JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb:res:org.my.path.resdb"));
        assertEquals("mymemdb", JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb:mymemdb"));
        assertEquals("/opt/db/testdb", JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb:file:/opt/db/testdb;ifexists=true, SA"));
        assertEquals("sample", JdbcHelper.parseInMemoryIdentifier("jdbc:derby:sample"));
        assertEquals("/opt/db/testdb", JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb:/opt/db/testdb;shutdown=true"));

        // sqlite
        assertEquals("C:/work/mydatabase.db", JdbcHelper.parseInMemoryIdentifier("jdbc:sqlite:C:/work/mydatabase.db"));
        assertEquals("/home/rms/work/my.db", JdbcHelper.parseInMemoryIdentifier("jdbc:sqlite:/home/rms/work/my.db"));
        assertEquals("sample.db", JdbcHelper.parseInMemoryIdentifier("jdbc:sqlite:sample.db"));
        assertEquals("memory", JdbcHelper.parseInMemoryIdentifier("jdbc:sqlite::memory:"));
    }

    @Test
    public void testParseFailsJdbcTCPConnections() {
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:sqlserver://win-doze-database.xdp.nerd.us:1433;authenticationScheme=nativeAuthentication;xopenStates=false;sendTimeAsDatetime=true;trustServerCertificate=false;sendStringParametersAsUnicode=true;selectMethod=direct;responseBuffering=adaptive;packetSize=8000;mu ltiSubnetFailover=false;loginTimeout=15;lockTimeout=-1;lastUpdateCount=true;encrypt=false;disableStatementPooling=true;databaseName=javaagent;applicationName=Microsoft JDBC Driver for SQL Server;applicationIntent=readwrite;"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:sqlserver://win-doze-database.xdp.nerd.us"));

        // MySQL
        // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-configuration-properties.html
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mysql:/sakila"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mysql://mycoolhost.net:3306/select?wat=true"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mysql://mycoolhost.net/select"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mysql:loadbalance://localhost:3306,localhost:3310/sakila"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mysql:replication://master,slave1,slave2,slave3/test"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mysql://address=(protocol=tcp)(host=localhost)(port=3306)/db"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mysql://address=(protocol=pipe)(path=\\\\.\\pipe\\MySQL57)/test"));

        // Postgres
        // https://jdbc.postgresql.org/documentation/80/connect.html
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:postgresql:agent_t?loadBalanceHosts=true\""));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:postgresql:database"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:postgresql://host/database"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:postgresql://host:1234/database"));

        // informix
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:informix-\n"
                + "sqli://123.45.67.89:1533/testDB:INFORMIXSERVER=myserver;user=rdtest;password-\n"
                + "test"));

        // MariaDB
        // https://mariadb.com/kb/en/mariadb/about-mariadb-connector-j/
        //
        // jdbc:(mysql|mariadb):[replication:|failover:|sequential:|aurora:]//<hostDescription>[,<hostDescription>...]/[database][?<key1>=<value1>[&<key2>=<value2>]]
        // host description
        // <host>[:<portnumber>]  or address=(host=<host>)[(port=<portnumber>)][(type=(master|slave))]
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mariadb://myhost/db"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mariadb://myhost:1234/db"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mariadb:replication://myhost:1234/db"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:mariadb//address=(host=127.0.0.1)"));

        // Oracle
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:oracle:thin:@199.26.328.251:49161/xe"));

        // DB2
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:weblogic:db2://server1:50000;DatabaseName=jdbc;Database=acct;"));

        // Hsqldb
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb:hsql://localhost/xdb, SA"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:hsqldb://localhost/xdb, SA"));

        // Derby
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:derby://localhost:1527/DerbyDB"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:derby://myhost:1527/memory:myDB;create=true"));

        // h2
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:h2://localhost:1527/H2DB"));
        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.parseInMemoryIdentifier("jdbc:h2://1928.168.1.1:1527/H2DB;password=s3kr3t"));
    }

    @Test
    public void testCacheConnectionURL() throws SQLException {
        final Connection connection = Mockito.mock(Connection.class);
        final DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);

        // First call succeeds, subsequent calls throw an exception
        Mockito.when(metaData.getURL())
               .thenReturn("connectionString")
               .thenThrow(new RuntimeException("bang! I should only be called once"));

        Assert.assertEquals("connectionString", JdbcHelper.getConnectionURL(connection));
        Assert.assertEquals("connectionString", JdbcHelper.getConnectionURL(connection));
        Assert.assertEquals("connectionString", JdbcHelper.getConnectionURL(connection));
    }

    @Test
    public void putAndGetConnectionFactory_withValidFactory_isSuccessful() throws SQLException {
        final Connection connection = Mockito.mock(Connection.class);
        final DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getURL()).thenReturn("connUrl1");
        final ConnectionFactory factory = Mockito.mock(ConnectionFactory.class);

        assertNull(JdbcHelper.getConnectionFactory(connection));
        assertFalse(JdbcHelper.connectionFactoryExists(connection));
        JdbcHelper.putConnectionFactory("connUrl1", factory);
        assertEquals(factory, JdbcHelper.getConnectionFactory(connection));
        assertTrue(JdbcHelper.connectionFactoryExists(connection));
    }

    @Test
    public void putAndGetDatabaseName_withValidName_isSuccessful() throws SQLException {
        final Connection connection = Mockito.mock(Connection.class);
        final DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getURL()).thenReturn("connUrl3");
        final ConnectionFactory factory = Mockito.mock(ConnectionFactory.class);

        assertNull(JdbcHelper.getCachedDatabaseName(connection));
        assertFalse(JdbcHelper.databaseNameExists(connection));
        JdbcHelper.putDatabaseName("connUrl3", "dbName1");
        assertEquals("dbName1", JdbcHelper.getCachedDatabaseName(connection));
        assertTrue(JdbcHelper.databaseNameExists(connection));
    }

    @Test
    public void putAndGetVendor_withValidName_isSucccessful() {
        DatabaseVendor vendor = Mockito.mock(JdbcDatabaseVendor.class);
        Mockito.when(vendor.getType()).thenReturn("test");
        Assert.assertEquals(UnknownDatabaseVendor.INSTANCE, JdbcHelper.getVendor(Driver.class, "jdbc:test"));
        JdbcHelper.putVendor(Driver.class, vendor);
        assertEquals(vendor, JdbcHelper.getVendor(Driver.class, "jdbc:test"));

        // also test to make sure we can get it by the url without a valid class
        assertEquals(vendor, JdbcHelper.getVendor(JdbcHelperTest.class, "jdbc:test"));
    }

    @Test
    public void getDatabaseName_allCases() throws SQLException {
        final Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getCatalog()).thenReturn("myCatalog");

        assertEquals(JdbcHelper.UNKNOWN, JdbcHelper.getDatabaseName(null));
        assertEquals("myCatalog", JdbcHelper.getDatabaseName(connection));
    }

    @Test
    public void testAddSqlMetadataCommentIfNeeded_withVariousConfigs() throws Exception {
        // These tests aren't 100% comprehensive because of the difficulty of
        // mocking the service manager and transaction objects in the bridge
        // project. These test what is possible based on these restrictions.

        // Null SQL - should return null
        assertNull(JdbcHelper.addSqlMetadataCommentIfNeeded(null));

        // Empty SQL - empty String
        assertEquals("", JdbcHelper.addSqlMetadataCommentIfNeeded(""));
    }
}