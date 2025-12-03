/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.sql;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SqlStatementNormalizerTest {
    @Test
    public void sqlWithComments_parseCorrectly() {
        String sql = "   /* foo */   SELECT * FROM /* bar */ users /**/ WHERE name = 'Claptrap''s Account' -- comment\nAND /* block */ age > 25     ";
        String result = SqlStatementNormalizer.normalizeSql(sql);
        assertEquals("SELECT * FROM USERS WHERE NAME = 'CLAPTRAP''S ACCOUNT' AND AGE > 25", result);
    }

    @Test
    public void sqlWithCommentStringsInStringLiterals_parseCorrectly() {
        String sql = "SELECT * FROM comments\nWHERE content = '/* This is not a comment */'\nAND author = 'Claptrap''s'\n" +
                "AND note = '-- This looks like a comment but is not'\nAND /* actual comment */ status = 'active'\n" +
                "-- real single line comment\nORDER BY created_date";
        String result = SqlStatementNormalizer.normalizeSql(sql);
        assertEquals("SELECT * FROM COMMENTS WHERE CONTENT = '/* THIS IS NOT A COMMENT */' AND AUTHOR = 'CLAPTRAP''S' " +
                "AND NOTE = '-- THIS LOOKS LIKE A COMMENT BUT IS NOT' AND STATUS = 'ACTIVE' ORDER BY CREATED_DATE", result);
    }

    @Test
    public void sqlWithUnterminatedComment_doesNotBomb() {
        String sql = "select * from foo /* unfinished comment here";
        String result = SqlStatementNormalizer.normalizeSql(sql);
        assertEquals("SELECT * FROM FOO", result);
    }

    @Test
    public void nominalSqlStatement_parsesCorrectly() {
        String sql = "select * from foo";
        String result = SqlStatementNormalizer.normalizeSql(sql);
        assertEquals("SELECT * FROM FOO", result);
    }

    @Test
    public void sqlStatementWithMultipleNewlines_parsesCorrectly() {
        String sql = "select \n\n\n\n\n\n*\n\n\n\n\nfrom foo\n\n\n\n\n";
        String result = SqlStatementNormalizer.normalizeSql(sql);
        assertEquals("SELECT * FROM FOO", result);
    }

    @Test
    public void nullOrEmptyStatement_returnsEmptyString() {
        assertEquals("", SqlStatementNormalizer.normalizeSql(null));
        assertEquals("", SqlStatementNormalizer.normalizeSql(""));
    }

    @Test
    public void normalizingCallableStatements() {
        String [] testCallables = {
                "CALL get_user(123, 'active') -- comment",
                "EXEC update_user @id = 123 /* comment */",
                "{ CALL my_proc(?, ?) }",
                "BEGIN\n    my_proc('test'); -- comment\n    END;",
                "CALL my_proc() # MySQL comment"
        };

        String [] results = {
                "CALL GET_USER(123, 'ACTIVE')",
                "EXEC UPDATE_USER @ID = 123",
                "{ CALL MY_PROC(?, ?) }",
                "BEGIN MY_PROC('TEST'); END;",
                "CALL MY_PROC()"
        };

        for (int idx = 0; idx < testCallables.length; idx++) {
            assertEquals(results[idx], SqlStatementNormalizer.normalizeSql(testCallables[idx]));
        }
    }

    @Test
    public void timingTest() {
        // Just to get some timing numbers for normalizing a large number of statements
        String sql1 = "   /* foo */   SELECT * FROM /* bar */ users /**/ WHERE name = 'Claptrap''s Account' -- comment\nAND /* block */ age > 25     ";
        String sql2 = "SELECT * FROM comments\nWHERE content = '/* This is not a comment */'\nAND author = 'Claptrap''s'\n" +
                "AND note = '-- This looks like a comment but is not'\nAND /* actual comment */ status = 'active'\n" +
                "-- real single line comment\nORDER BY created_date";

        long start = System.currentTimeMillis();
        for (int idx=0; idx<100000; idx++) {
            SqlStatementNormalizer.normalizeSql(sql1);
            SqlStatementNormalizer.normalizeSql(sql2);
        }
        System.out.println("Elasped time: " + (System.currentTimeMillis() - start) + "ms");
    }
}
