/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class SqlObfuscatorTest {

    @Test
    public void obfuscateHTML() {
        String parameter="";
        String path = "src/test/resources/com/newrelic/agent/document-content.json";
        try (Stream<String> lines = java.nio.file.Files.lines(Paths.get(path), StandardCharsets.UTF_8)) {
            parameter = lines.collect(Collectors.joining(System.lineSeparator()));

        } catch (IOException e) {
            e.printStackTrace();
        }
        parameter = "'" + parameter + "'";
        String sql = "insert employees values (4, "+parameter+"  )";
        assertEquals("insert employees values (?, ?)", SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(sql));
    }

    @Test
    public void nullSql() {
        assertEquals(null, SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(null));
    }

    @Test
    public void columnsWithNumerics() {
        String sql = "Select id0 from metrics0";
        assertEquals(sql, SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(sql));
    }

    @Test
    public void quotedColumns() {
        String sql = "select \"id\", 'name' from employees";
        assertEquals("select ?, ? from employees", SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(sql));
    }

    @Test
    public void insert() {
        String sql = "insert employees values (4, 'dude')";
        assertEquals("insert employees values (?, ?)", SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(sql));
    }

    @Test
    public void like() {
        String sql = "select * from employees where name like 'dude'";
        assertEquals("select * from employees where name like ?", SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(sql));
    }

    @Test
    public void lineBreak() {
        String sql = SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(
                "select * from accounts where accounts.name != 'dude \n newline' order by accounts.name");
        assertEquals("select * from accounts where accounts.name != ? order by accounts.name", sql);
    }

    @Test
    public void singleQuote() {
        String sql = SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql("select * from employees where name = 'dude'");
        assertEquals("select * from employees where name = ?", sql);
    }

    @Test
    public void doubleQuote() {
        String sql = SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(
                "select * from employees where name = \"dude\" and comment = 'whoah dude'");
        assertEquals("select * from employees where name = ? and comment = ?", sql);
    }

    @Test
    public void testDollarQuotes() {
        SqlObfuscator obfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String result = obfuscator.obfuscateSql("select * from a where x = $$I'm a thing$$ and b = $FOO$;");
        assertEquals("select * from a where x = ? and b = ?", result);
    }
    
    @Test
    public void testOracleQuotes() throws Exception {
        SqlObfuscator obfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String result = obfuscator.obfuscateSql("select * from a where x = q'[a]' " +
                "and y = q'[ some thing ]' and z = q'{thing}' " +
                "and aa = q'(meh)' and bb = q'<okey>'");

        String expected = "select * from a where x = ? and y = ? and z = ? and aa = ? and bb = ?";
        assertEquals(expected, result);
    }
    
    @Test
    public void intReplacement() {
        String sql = SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql(
                "select * from employees where id = 737366255");
        assertEquals("select * from employees where id = ?", sql);

        sql = SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql("select * from employees where id=737366255");
        assertEquals("select * from employees where id=?", sql);

        sql = SqlObfuscator.getDefaultSqlObfuscator().obfuscateSql("select * from employees where id=     737366255");
        assertEquals("select * from employees where id=     ?", sql);
    }

    @Test
    public void digits() {
        SqlObfuscator sqlObfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String rawSql = "SELECT c1.communityID, c2.lft as parent FROM jiveCommunity c1, jiveCommunity c2 WHERE c1.lft < c2.lft AND c1.rgt > c2.rgt AND c1.communityID = ? ORDER BY parent DESC";
        String expectedSql = "SELECT c1.communityID, c2.lft as parent FROM jiveCommunity c1, jiveCommunity c2 WHERE c1.lft < c2.lft AND c1.rgt > c2.rgt AND c1.communityID = ? ORDER BY parent DESC";
        String actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "SELECT c11.communityID, c22.lft FROM jiveCommunity c11, jiveCommunity c22";
        expectedSql = "SELECT c11.communityID, c22.lft FROM jiveCommunity c11, jiveCommunity c22";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = 77853";
        expectedSql = "SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = ?";
        expectedSql = "SELECT t0.ID, t0.TYPE FROM SVF003P.NIETVORDERBAARHEID t0 WHERE t0.FK_SCHULDENAAR_ID = ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where credit-card = 123-45-6789";
        expectedSql = "select * from customers where credit-card = ???";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where credit-card = 123_45_6789";
        expectedSql = "select * from customers where credit-card = ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where credit-card = '123-45-6789'";
        expectedSql = "select * from customers where credit-card = ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where credit-card = 123456789";
        expectedSql = "select * from customers where credit-card = ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where credit-card =     123456789";
        expectedSql = "select * from customers where credit-card =     ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where credit-card =123456789";
        expectedSql = "select * from customers where credit-card =?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from employees where id in (123,   456,7890)";
        expectedSql = "select * from employees where id in (?,   ?,?)";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id = c123d456";
        expectedSql = "select * from customers where id = c123d456";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id = 123.50";
        expectedSql = "select * from customers where id = ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id >123";
        expectedSql = "select * from customers where id >?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id <123";
        expectedSql = "select * from customers where id <?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id = haran21 ";
        expectedSql = "select * from customers where id = haran21 ";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id = +123";
        expectedSql = "select * from customers where id = +?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id =+123";
        expectedSql = "select * from customers where id =+?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id = -123";
        expectedSql = "select * from customers where id = ?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id = 123*1024";
        expectedSql = "select * from customers where id = ?*?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);

        rawSql = "select * from customers where id = 123/1024";
        expectedSql = "select * from customers where id = ?/?";
        actualSql = sqlObfuscator.obfuscateSql(rawSql);
        assertEquals(expectedSql, actualSql);
    }

    @Test
    public void noObfuscation() {
        String sql = "select * from employees where id = 737366255";
        assertEquals(sql, SqlObfuscator.getNoObfuscationSqlObfuscator().obfuscateSql(sql));
    }

    @Test
    public void testSimpleComment() throws Exception {
        SqlObfuscator obfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String result = obfuscator.obfuscateSql("select * from foo.bar -- this is really really helpful");
        String expected = "select * from foo.bar ?";
        assertEquals(expected, result);
    }

    @Test
    public void testMultilineComment() throws Exception {
        SqlObfuscator obfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        String sql = "/**" +
                " * note: we really love jimbo!" +
                "*/ select * from foo.bar where name = 'jimbo'";
        String result = obfuscator.obfuscateSql(sql);
        String expected = "? select * from foo.bar where name = ?";
        assertEquals(expected, result);
    }

    @Test
    public void getCachingSqlObfuscator() {
        SqlObfuscator sqlObfuscator = SqlObfuscator.getNoObfuscationSqlObfuscator();
        assertSame(sqlObfuscator, SqlObfuscator.getCachingSqlObfuscator(sqlObfuscator));

        sqlObfuscator = SqlObfuscator.getNoSqlObfuscator();
        assertSame(sqlObfuscator, SqlObfuscator.getCachingSqlObfuscator(sqlObfuscator));

        sqlObfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        assertNotSame(sqlObfuscator, SqlObfuscator.getCachingSqlObfuscator(sqlObfuscator));
    }

    @Test
    public void testCachingSqlObfuscator() {
        SqlObfuscator sqlObfuscator = SqlObfuscator.getDefaultSqlObfuscator();
        SqlObfuscator cachingSqlObfuscator = SqlObfuscator.getCachingSqlObfuscator(sqlObfuscator);

        String sql = "select * employees where name = \"dude\"";
        assertNotSame(sqlObfuscator.obfuscateSql(sql), (sqlObfuscator.obfuscateSql(sql)));
        assertSame(cachingSqlObfuscator.obfuscateSql(sql), (cachingSqlObfuscator.obfuscateSql(sql)));
    }

    @Test
    public void noSql() {
        Assert.assertNull(SqlObfuscator.getNoSqlObfuscator().obfuscateSql("select test from dude where id = 3"));
    }

    @Test
    public void testLargeHtmlTextSqlStackOverflow() throws Exception {
        SqlObfuscator sqlObfuscator = SqlObfuscator.getDefaultSqlObfuscator();

        URL htmlFileUrl = this.getClass().getResource("/html_ipsum.html");
        File htmlFile = new File(htmlFileUrl.getFile());
        CharSource largeHtmlText = Files.asCharSource(htmlFile, Charsets.UTF_8);
        String rawSql = "Replace into table1 (col1, col2, col3, col4, col5) values ('string', 'string', '" + largeHtmlText +  "', CURRENT_TIMESTAMP, int)";

        String actualSql = sqlObfuscator.obfuscateSql(rawSql);
        Assert.assertNotNull(actualSql);
    }
}
