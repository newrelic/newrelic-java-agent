package com.newrelic.agent.sql;

import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SqlStatementHasherTest {
    @Test
    public void nullOrEmptyStatement_returnsEmptyString() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        assertEquals("", SqlStatementHasher.hashSqlStatement("", md));
        assertEquals("", SqlStatementHasher.hashSqlStatement(null, md));
    }

    @Test
    public void regularString_returnsCorrectMD5Hash() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        assertEquals("a380157ccd4d4715187c72cbe34a2123", SqlStatementHasher.hashSqlStatement("SELECT * FROM FOO", md));
    }
}
