/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.sql;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlStatementNormalizerTest {
    @Test
    public void sqlWithComments_parseCorrectly() {
        String sql = "   /* foo */   SELECT * FROM /* bar */ users /**/ WHERE name = 'Claptrap''s Account' -- comment\nAND /* block */ age > 25     ";
        assertEquals("SELECT * FROM USERS WHERE NAME = ? AND AGE > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void sqlWithCommentStringsInStringLiterals_parseCorrectly() {
        String sql = "SELECT * FROM comments\nWHERE content = '/* This is not a comment */'\nAND author = 'Claptrap''s'\n" +
                "AND note = '-- This looks like a comment but is not'\nAND /* actual comment */ status = 'active'\n" +
                "-- real single line comment\nORDER BY created_date";
        // All string literals replaced with ?
        assertEquals("SELECT * FROM COMMENTS WHERE CONTENT = ? AND AUTHOR = ? AND NOTE = ? AND STATUS = ? ORDER BY CREATED_DATE",
                SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void sqlWithUnterminatedComment_doesNotBomb() {
        String sql = "select * from foo /* unfinished comment here";
        assertEquals("SELECT * FROM FOO", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void nominalSqlStatement_parsesCorrectly() {
        String sql = "select * from foo";
        assertEquals("SELECT * FROM FOO", SqlStatementNormalizer.normalizeSql(sql));

        sql = "select v1_0.pet_id,v1_0.id,v1_0.visit_date,v1_0.description from visits v1_0 where v1_0.pet_id=? order by v1_0.visit_date";
        assertEquals("SELECT V1_0.PET_ID,V1_0.ID,V1_0.VISIT_DATE,V1_0.DESCRIPTION FROM VISITS V1_0 WHERE V1_0.PET_ID=? ORDER BY V1_0.VISIT_DATE",
                SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void sqlStatementWithMultipleNewlines_parsesCorrectly() {
        String sql = "select \n\n\n\n\n\n*\n\n\n\n\nfrom foo\n\n\n\n\n";
        assertEquals("SELECT * FROM FOO", SqlStatementNormalizer.normalizeSql(sql));
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
                "CALL GET_USER(?, ?)",
                "EXEC UPDATE_USER ? = ?",  // @id becomes ?
                "{ CALL MY_PROC(?, ?) }",
                "BEGIN MY_PROC(?); END;",
                "CALL MY_PROC()"
        };

        for (int idx = 0; idx < testCallables.length; idx++) {
            assertEquals(results[idx], SqlStatementNormalizer.normalizeSql(testCallables[idx]));
        }
    }

    @Test
    public void jdbcStylePlaceholders_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
        assertEquals("SELECT * FROM USERS WHERE ID = ? AND NAME = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void postgresStylePlaceholders_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE id = $1 AND name = $2 AND age > $3";
        assertEquals("SELECT * FROM USERS WHERE ID = ? AND NAME = ? AND AGE > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void oracleStyleNamedPlaceholders_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE id = :id AND name = :username AND age > :age";
        assertEquals("SELECT * FROM USERS WHERE ID = ? AND NAME = ? AND AGE > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void sqlServerStylePlaceholders_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE id = @id AND name = @username AND age > @p1";
        assertEquals("SELECT * FROM USERS WHERE ID = ? AND NAME = ? AND AGE > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void pythonStyleNamedPlaceholders_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE id = %(id)s AND name = %(username)s";
        assertEquals("SELECT * FROM USERS WHERE ID = ? AND NAME = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void mixedPlaceholderStyles_normalizeToQuestionMark() {
        // This shouldn't happen in practice, but test it anyway
        String sql = "SELECT * FROM users WHERE a = ? AND b = $1 AND c = :name AND d = @id";
        assertEquals("SELECT * FROM USERS WHERE A = ? AND B = ? AND C = ? AND D = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void numericLiterals_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE age > 25 AND balance < 1000.50 AND score = -42";
        assertEquals("SELECT * FROM USERS WHERE AGE > ? AND BALANCE < ? AND SCORE = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void stringLiterals_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE name = 'John' AND status = 'active'";
        assertEquals("SELECT * FROM USERS WHERE NAME = ? AND STATUS = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void stringLiteralsWithEscapedQuotes_normalizeToQuestionMark() {
        String sql = "SELECT * FROM users WHERE name = 'O''Brien' AND note = 'It''s fine'";
        assertEquals("SELECT * FROM USERS WHERE NAME = ? AND NOTE = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void scientificNotation_normalizeToQuestionMark() {
        String sql = "SELECT * FROM data WHERE value > 1.5e10 AND other < 2E-5";
        assertEquals("SELECT * FROM DATA WHERE VALUE > ? AND OTHER < ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void decimalNumbers_normalizeToQuestionMark() {
        String sql = "SELECT * FROM products WHERE price = 19.99 AND discount = 0.15 AND tax = .08";
        assertEquals("SELECT * FROM PRODUCTS WHERE PRICE = ? AND DISCOUNT = ? AND TAX = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void inClauseWithMultipleNumbers_normalizesToSinglePlaceholder() {
        String sql = "SELECT * FROM users WHERE id IN (1, 2, 3, 4, 5)";
        assertEquals("SELECT * FROM USERS WHERE ID IN (?)", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void inClauseWithMultipleStrings_normalizesToSinglePlaceholder() {
        String sql = "SELECT * FROM users WHERE status IN ('active', 'pending', 'approved')";
        assertEquals("SELECT * FROM USERS WHERE STATUS IN (?)", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void inClauseWithMultiplePlaceholders_normalizesToSinglePlaceholder() {
        String sql = "SELECT * FROM users WHERE id IN (?, ?, ?, ?)";
        assertEquals("SELECT * FROM USERS WHERE ID IN (?)", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void inClauseWithPostgresPlaceholders_normalizesToSinglePlaceholder() {
        String sql = "SELECT * FROM users WHERE id IN ($1, $2, $3)";
        assertEquals("SELECT * FROM USERS WHERE ID IN (?)", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void inClauseWithSingleValue_doesNotNormalize() {
        String sql = "SELECT * FROM users WHERE id IN (123)";
        // Single value stays as (?)
        assertEquals("SELECT * FROM USERS WHERE ID IN (?)", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void inClauseWithSubquery_doesNotNormalize() {
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        // Subquery is preserved, not normalized to (?)
        assertEquals("SELECT * FROM USERS WHERE ID IN (SELECT USER_ID FROM ORDERS)", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void multipleInClauses_bothNormalize() {
        String sql = "SELECT * FROM orders WHERE user_id IN (1, 2, 3) AND status IN ('pending', 'shipped')";
        assertEquals("SELECT * FROM ORDERS WHERE USER_ID IN (?) AND STATUS IN (?)", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void sameQueryDifferentLanguages_producesSameNormalizedForm() {
        // Java JDBC
        String jdbc = "SELECT * FROM users WHERE id = ? AND name = ? AND age > ?";

        // Python with client-side substitution (already executed)
        String pythonExecuted = "SELECT * FROM users WHERE id = 123 AND name = 'John' AND age > 25";

        // PostgreSQL with positional parameters
        String postgres = "SELECT * FROM users WHERE id = $1 AND name = $2 AND age > $3";

        // Oracle with named parameters
        String oracle = "SELECT * FROM users WHERE id = :id AND name = :name AND age > :age";

        // SQL Server with named parameters
        String sqlServer = "SELECT * FROM users WHERE id = @id AND name = @name AND age > @age";

        String expected = "SELECT * FROM USERS WHERE ID = ? AND NAME = ? AND AGE > ?";

        assertEquals(expected, SqlStatementNormalizer.normalizeSql(jdbc));
        assertEquals(expected, SqlStatementNormalizer.normalizeSql(pythonExecuted));
        assertEquals(expected, SqlStatementNormalizer.normalizeSql(postgres));
        assertEquals(expected, SqlStatementNormalizer.normalizeSql(oracle));
        assertEquals(expected, SqlStatementNormalizer.normalizeSql(sqlServer));
    }

    @Test
    public void insertWithDifferentLanguages_producesSameNormalizedForm() {
        // JDBC
        String jdbc = "INSERT INTO users (name, age) VALUES (?, ?)";

        // Already executed with literals
        String executed = "INSERT INTO users (name, age) VALUES ('Alice', 30)";

        // PostgreSQL
        String postgres = "INSERT INTO users (name, age) VALUES ($1, $2)";

        String expected = "INSERT INTO USERS (NAME, AGE) VALUES (?, ?)";

        assertEquals(expected, SqlStatementNormalizer.normalizeSql(jdbc));
        assertEquals(expected, SqlStatementNormalizer.normalizeSql(executed));
        assertEquals(expected, SqlStatementNormalizer.normalizeSql(postgres));
    }

    @Test
    public void updateWithInClauseDifferentLanguages_producesSameNormalizedForm() {
        // With literals
        String withLiterals = "UPDATE users SET status = 'inactive' WHERE id IN (1, 2, 3, 4, 5)";

        // With placeholders
        String withPlaceholders = "UPDATE users SET status = ? WHERE id IN (?, ?, ?, ?, ?)";

        String expected = "UPDATE USERS SET STATUS = ? WHERE ID IN (?)";

        assertEquals(expected, SqlStatementNormalizer.normalizeSql(withLiterals));
        assertEquals(expected, SqlStatementNormalizer.normalizeSql(withPlaceholders));
    }

    @Test
    public void numbersInTableNames_notNormalized() {
        String sql = "SELECT * FROM table1 JOIN table2 ON table1.id = table2.id";
        assertEquals("SELECT * FROM TABLE1 JOIN TABLE2 ON TABLE1.ID = TABLE2.ID", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void numbersInColumnNames_notNormalized() {
        String sql = "SELECT col1, col2, field_3 FROM users WHERE col1 = 123";
        assertEquals("SELECT COL1, COL2, FIELD_3 FROM USERS WHERE COL1 = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void stringLiteralsContainingPlaceholderLikeText_notTreatedAsPlaceholders() {
        String sql = "SELECT * FROM logs WHERE message = 'Error: $1 failed' AND code = 'ERR-001'";
        // The $1 inside the string should not be treated as a placeholder
        assertEquals("SELECT * FROM LOGS WHERE MESSAGE = ? AND CODE = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void colonInTimeValues_notTreatedAsPlaceholder() {
        // The logic for this might need to be updated based on real world
        // examples
        String sql = "SELECT * FROM events WHERE time > '10:30:00'";
        assertEquals("SELECT * FROM EVENTS WHERE TIME > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void negativeNumbers_normalizeCorrectly() {
        String sql = "SELECT * FROM data WHERE temp < -10 AND altitude > -500.5";
        assertEquals("SELECT * FROM DATA WHERE TEMP < ? AND ALTITUDE > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void positiveNumbers_normalizeCorrectly() {
        String sql = "SELECT * FROM data WHERE score = +100 AND bonus = +25.5";
        assertEquals("SELECT * FROM DATA WHERE SCORE = ? AND BONUS = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void signWithoutDigit_notNormalized() {
        String sql = "SELECT x + y FROM data WHERE a - b > 0";
        assertEquals("SELECT X + Y FROM DATA WHERE A - B > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void decimalNumbers_handledCorrectly() {
        String sql1 = "SELECT * FROM data WHERE value = 1.5";
        assertEquals("SELECT * FROM DATA WHERE VALUE = ?", SqlStatementNormalizer.normalizeSql(sql1));

        String sql2 = "SELECT * FROM data WHERE value = .5";
        assertEquals("SELECT * FROM DATA WHERE VALUE = ?", SqlStatementNormalizer.normalizeSql(sql2));

        String sql3 = "SELECT users.name FROM users WHERE users.id = 123";
        assertEquals("SELECT USERS.NAME FROM USERS WHERE USERS.ID = ?", SqlStatementNormalizer.normalizeSql(sql3));
    }

    @Test
    public void emptyInClause_handledGracefully() {
        String sql = "SELECT * FROM users WHERE id IN ()";
        assertEquals("SELECT * FROM USERS WHERE ID IN ()", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void nestedParentheses_handledCorrectly() {
        String sql = "SELECT * FROM users WHERE (age > 25 AND (status = 'active' OR status = 'pending'))";
        assertEquals("SELECT * FROM USERS WHERE (AGE > ? AND (STATUS = ? OR STATUS = ?))", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void functionCallsWithParameters_parametersNormalized() {
        String sql = "SELECT SUBSTRING(name, 1, 5) FROM users WHERE LENGTH(email) > 20";
        assertEquals("SELECT SUBSTRING(NAME, ?, ?) FROM USERS WHERE LENGTH(EMAIL) > ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void caseSensitiveIdentifiersWithBackticks_preserved() {
        String sql = "SELECT * FROM `user-table` WHERE `user-id` = 123";
        assertEquals("SELECT * FROM `USER-TABLE` WHERE `USER-ID` = ?", SqlStatementNormalizer.normalizeSql(sql));
    }

    @Test
    public void postgresAnyArraySyntax_normalizesCorrectly() {
        // PostgreSQL = ANY syntax with ARRAY constructor
        // Each literal should be normalized individually
        String sql1 = "SELECT * FROM users WHERE id = ANY(ARRAY[1, 2, 3, 4, 5])";
        assertEquals("SELECT * FROM USERS WHERE ID = ANY(ARRAY[?, ?, ?, ?, ?])", SqlStatementNormalizer.normalizeSql(sql1));

        // PostgreSQL = ANY with string literal array (single string, not individual elements)
        String sql2 = "SELECT * FROM users WHERE status = ANY('{active,pending,approved}')";
        assertEquals("SELECT * FROM USERS WHERE STATUS = ANY(?)", SqlStatementNormalizer.normalizeSql(sql2));

        // PostgreSQL = ANY with parameter placeholder
        String sql3 = "SELECT * FROM users WHERE id = ANY($1)";
        assertEquals("SELECT * FROM USERS WHERE ID = ANY(?)", SqlStatementNormalizer.normalizeSql(sql3));

        // PostgreSQL = ANY with named parameter
        String sql4 = "SELECT * FROM users WHERE tag = ANY(:tags)";
        assertEquals("SELECT * FROM USERS WHERE TAG = ANY(?)", SqlStatementNormalizer.normalizeSql(sql4));
    }

    @Test
    public void massiveComplexQuery_normalizesCorrectly() {
        long start = System.currentTimeMillis();
        String result = SqlStatementNormalizer.normalizeSql(MASSIVE_COMPLEX_SQL);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(MASSIVE_COMPLEX_SQL_EXPECTED, result);

        assertFalse("no literal numbers", result.matches(".*\\d+\\.\\d+.*"));
        assertFalse("no string literals", result.contains("'"));
        assertTrue("no comments", !result.contains("/*") && !result.contains("--") && !result.contains("#"));
        assertTrue("all placeholders should be ?", result.contains("?"));
        assertTrue("collapse IN clauses", result.contains("IN (?)"));

        System.out.println("Complex query normalization time: " + elapsed + "ms");
    }

    @Test
    public void timingTest() {
        // Just to get some timing numbers for normalizing a large number of statements
        String sql1 = "   /* foo */   SELECT * FROM /* bar */ users /**/ WHERE name = 'Claptrap''s Account' -- comment\nAND /* block */ age > 25     ";
        String sql2 = "SELECT * FROM comments\nWHERE content = '/* This is not a comment */'\nAND author = 'Claptrap''s'\n" +
                "AND note = '-- This looks like a comment but is not'\nAND /* actual comment */ status = 'active'\n" +
                "-- real single line comment\nORDER BY created_date";
        String sql3 = "SELECT * FROM orders WHERE user_id IN (1, 2, 3, 4, 5) AND status IN ('pending', 'shipped')";
        String sql4 = "SELECT * FROM users WHERE id = $1 AND name = $2 AND age > $3";

        long start = System.currentTimeMillis();
        for (int idx=0; idx<100000; idx++) {
            SqlStatementNormalizer.normalizeSql(sql1);
            SqlStatementNormalizer.normalizeSql(sql2);
            SqlStatementNormalizer.normalizeSql(sql3);
            SqlStatementNormalizer.normalizeSql(sql4);
        }
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * A large SQL statement that contains multiple comment types,
     * placeholder styles, number formats, string literals, IN clauses
     * and function calls.
     */
    private static final String MASSIVE_COMPLEX_SQL = "/* Beginning of massive query */ \n" +
            "SELECT \n" +
            "    u.id AS user_id, -- User identifier\n" +
            "    u.username, \n" +
            "    u.email, /* Email address */\n" +
            "    u.created_at,\n" +
            "    o.order_id,\n" +
            "    o.total_amount, # Order total\n" +
            "    o.tax_amount,\n" +
            "    p.product_name,\n" +
            "    p.price AS unit_price,\n" +
            "    COUNT(*) as order_count,\n" +
            "    SUM(o.total_amount) as total_revenue,\n" +
            "    AVG(p.price) as avg_price,\n" +
            "    MAX(o.created_at) as last_order_date\n" +
            "FROM users u\n" +
            "/* Join with orders table */\n" +
            "INNER JOIN orders o ON u.id = o.user_id\n" +
            "LEFT JOIN order_items oi ON o.order_id = oi.order_id -- Get order items\n" +
            "INNER JOIN products p ON oi.product_id = p.id\n" +
            "WHERE \n" +
            "    -- Filter by various conditions\n" +
            "    u.status = 'active' \n" +
            "    AND u.email LIKE '%@example.com' /* Company emails only */\n" +
            "    AND u.age > 18\n" +
            "    AND u.age < 65\n" +
            "    AND u.balance >= 100.50 # Minimum balance\n" +
            "    AND u.credit_score > 700\n" +
            "    AND u.last_login > '2024-01-01 10:30:00'\n" +
            "    /* PostgreSQL style parameters */\n" +
            "    AND u.region_id = $1\n" +
            "    AND u.country_code = $2\n" +
            "    AND u.tier IN ($3, $4, $5)\n" +
            "    -- Oracle style parameters\n" +
            "    AND o.status = :order_status\n" +
            "    AND o.payment_method = :payment_type\n" +
            "    AND o.shipping_country = :ship_country\n" +
            "    /* SQL Server style parameters */\n" +
            "    AND p.category_id = @category\n" +
            "    AND p.brand_id = @brand\n" +
            "    AND p.supplier_id = @supplier_id\n" +
            "    -- Python dict-style parameters\n" +
            "    AND u.referral_code = %(referral)s\n" +
            "    AND u.promo_code = %(promo)s\n" +
            "    /* IN clauses with literals */\n" +
            "    AND o.status IN ('pending', 'processing', 'shipped', 'delivered')\n" +
            "    AND p.category IN (1, 2, 3, 5, 8, 13, 21) -- Fibonacci categories\n" +
            "    AND u.country_code IN ('US', 'CA', 'UK', 'AU') # English-speaking\n" +
            "    -- IN clauses with placeholders\n" +
            "    AND o.warehouse_id IN (?, ?, ?)\n" +
            "    AND p.tag_id IN ($6, $7, $8, $9)\n" +
            "    /* Numeric comparisons with various formats */\n" +
            "    AND o.discount_percent < 0.25 -- Max 25% discount\n" +
            "    AND o.tax_rate >= .08 # Minimum tax\n" +
            "    AND p.weight_kg < 100.0\n" +
            "    AND p.rating >= 4.5\n" +
            "    AND o.item_count BETWEEN 1 AND 50\n" +
            "    AND p.stock_quantity > -10 -- Allow negative for backorders\n" +
            "    AND p.views_count > 1000000 /* Million views */\n" +
            "    AND p.conversion_rate > 1.5e-2 -- Scientific notation: 1.5%\n" +
            "    AND u.login_count < 1E6 # One million logins\n" +
            "    /* String literals with special characters */\n" +
            "    AND u.notes NOT LIKE '%O''Brien%' -- Escaped quote\n" +
            "    AND u.description != 'It''s a test' -- Another escaped quote\n" +
            "    AND o.comment <> '/* Not a comment */' -- Comment-like string\n" +
            "    AND o.note != '-- Not a SQL comment' /* String containing -- */\n" +
            "    AND p.name != 'Product$1' # Dollar sign in name\n" +
            "    AND p.code != ':special:' -- Colon in string\n" +
            "    AND p.identifier != '@handle' -- At sign in string\n" +
            "    /* Complex conditions with nested parentheses */\n" +
            "    AND (\n" +
            "        (u.subscription_tier = 'premium' AND u.months_active >= 12)\n" +
            "        OR (u.subscription_tier = 'basic' AND u.months_active >= 24)\n" +
            "        OR (u.total_spent > 5000.00 AND u.order_count > 100)\n" +
            "    )\n" +
            "    /* Function calls with various parameters */\n" +
            "    AND SUBSTRING(u.email, 1, 10) = 'john.smith'\n" +
            "    AND COALESCE(u.phone, 'N/A') != 'N/A'\n" +
            "    AND ROUND(o.total_amount, 2) = 99.99\n" +
            "    AND DATEDIFF('day', o.created_at, o.shipped_at) <= 3\n" +
            "    -- More mixed placeholder styles\n" +
            "    AND YEAR(u.birthday) = ?\n" +
            "    AND MONTH(u.anniversary) IN ($10, $11)\n" +
            "    AND DAY(o.delivery_date) = :delivery_day\n" +
            "    AND LENGTH(p.description) > @min_length\n" +
            "    AND u.loyalty_points >= %(min_points)s\n" +
            "GROUP BY \n" +
            "    u.id, u.username, u.email, u.created_at,\n" +
            "    o.order_id, o.total_amount, o.tax_amount,\n" +
            "    p.product_name, p.price\n" +
            "/* Having clause with aggregates */\n" +
            "HAVING \n" +
            "    COUNT(*) > 5 -- At least 5 orders\n" +
            "    AND SUM(o.total_amount) > 1000.00\n" +
            "    AND AVG(p.price) BETWEEN 10.0 AND 500.0\n" +
            "ORDER BY \n" +
            "    total_revenue DESC, -- Highest revenue first\n" +
            "    order_count DESC,\n" +
            "    u.username ASC\n" +
            "LIMIT 100 # Top 100 results\n" +
            "OFFSET 0 -- Start from beginning\n" +
            "/* End of massive query */";

    /**
     * Expected normalized output for MASSIVE_COMPLEX_SQL:
     * - All placeholders normalized to ?
     * - All literals replaced with ?
     * - All IN clauses collapsed to IN (?)
     * - All comments removed
     * - Whitespace normalized
     * - Converted to uppercase
     */
    private static final String MASSIVE_COMPLEX_SQL_EXPECTED =
            "SELECT U.ID AS USER_ID, U.USERNAME, U.EMAIL, U.CREATED_AT, O.ORDER_ID, " +
            "O.TOTAL_AMOUNT, O.TAX_AMOUNT, P.PRODUCT_NAME, P.PRICE AS UNIT_PRICE, COUNT(*) AS ORDER_COUNT, " +
            "SUM(O.TOTAL_AMOUNT) AS TOTAL_REVENUE, AVG(P.PRICE) AS AVG_PRICE, MAX(O.CREATED_AT) AS LAST_ORDER_DATE " +
            "FROM USERS U INNER JOIN ORDERS O ON U.ID = O.USER_ID " +
            "LEFT JOIN ORDER_ITEMS OI ON O.ORDER_ID = OI.ORDER_ID " +
            "INNER JOIN PRODUCTS P ON OI.PRODUCT_ID = P.ID " +
            "WHERE U.STATUS = ? AND U.EMAIL LIKE ? AND U.AGE > ? AND U.AGE < ? " +
            "AND U.BALANCE >= ? AND U.CREDIT_SCORE > ? AND U.LAST_LOGIN > ? " +
            "AND U.REGION_ID = ? AND U.COUNTRY_CODE = ? AND U.TIER IN (?) " +
            "AND O.STATUS = ? AND O.PAYMENT_METHOD = ? AND O.SHIPPING_COUNTRY = ? " +
            "AND P.CATEGORY_ID = ? AND P.BRAND_ID = ? AND P.SUPPLIER_ID = ? " +
            "AND U.REFERRAL_CODE = ? AND U.PROMO_CODE = ? " +
            "AND O.STATUS IN (?) AND P.CATEGORY IN (?) AND U.COUNTRY_CODE IN (?) " +
            "AND O.WAREHOUSE_ID IN (?) AND P.TAG_ID IN (?) " +
            "AND O.DISCOUNT_PERCENT < ? AND O.TAX_RATE >= ? AND P.WEIGHT_KG < ? " +
            "AND P.RATING >= ? AND O.ITEM_COUNT BETWEEN ? AND ? " +
            "AND P.STOCK_QUANTITY > ? AND P.VIEWS_COUNT > ? " +
            "AND P.CONVERSION_RATE > ? AND U.LOGIN_COUNT < ? " +
            "AND U.NOTES NOT LIKE ? AND U.DESCRIPTION != ? AND O.COMMENT <> ? " +
            "AND O.NOTE != ? AND P.NAME != ? AND P.CODE != ? AND P.IDENTIFIER != ? " +
            "AND ( (U.SUBSCRIPTION_TIER = ? AND U.MONTHS_ACTIVE >= ?) " +
            "OR (U.SUBSCRIPTION_TIER = ? AND U.MONTHS_ACTIVE >= ?) " +
            "OR (U.TOTAL_SPENT > ? AND U.ORDER_COUNT > ?) ) " +
            "AND SUBSTRING(U.EMAIL, ?, ?) = ? AND COALESCE(U.PHONE, ?) != ? " +
            "AND ROUND(O.TOTAL_AMOUNT, ?) = ? AND DATEDIFF(?, O.CREATED_AT, O.SHIPPED_AT) <= ? " +
            "AND YEAR(U.BIRTHDAY) = ? AND MONTH(U.ANNIVERSARY) IN (?) " +
            "AND DAY(O.DELIVERY_DATE) = ? AND LENGTH(P.DESCRIPTION) > ? " +
            "AND U.LOYALTY_POINTS >= ? " +
            "GROUP BY U.ID, U.USERNAME, U.EMAIL, U.CREATED_AT, O.ORDER_ID, O.TOTAL_AMOUNT, " +
            "O.TAX_AMOUNT, P.PRODUCT_NAME, P.PRICE " +
            "HAVING COUNT(*) > ? AND SUM(O.TOTAL_AMOUNT) > ? AND AVG(P.PRICE) BETWEEN ? AND ? " +
            "ORDER BY TOTAL_REVENUE DESC, ORDER_COUNT DESC, U.USERNAME ASC LIMIT ? OFFSET ?";
}
