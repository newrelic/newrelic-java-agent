package com.newrelic.agent.bridge.datastore;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class R2dbcObfuscatorTest {
    @Test
    public void testQueryConverter(){
        String testQuotes = "Found one word: 'one''word' and two words: 'two' 'words'. Another word: \"another_word\" and even an escaped word: \'escaped word'";
        assertEquals("Found one word: ? and two words: ? ?. Another word: ? and even an escaped word: ?", R2dbcObfuscator.QUERY_CONVERTER.toObfuscatedQueryString(testQuotes));

        String testDollarQuotes = "Found a string literal: $tag$string_literal$tag$";
        assertEquals("Found a string literal: ?", R2dbcObfuscator.QUERY_CONVERTER.toObfuscatedQueryString(testDollarQuotes));

        String testBooleans = "Found true and TRUE and FalSe and false and NULL but not FakeBoolean";
        assertEquals("Found ? and ? and ? and ? and ? but not FakeBoolean", R2dbcObfuscator.QUERY_CONVERTER.toObfuscatedQueryString(testBooleans));

        String testSingleLineComment = "Found a line with a comment #here\n Second line with a comment --here\r and a terminating comment #here";
        assertEquals("Found a line with a comment ?\n Second line with a comment ?\r and a terminating comment ?", R2dbcObfuscator.QUERY_CONVERTER.toObfuscatedQueryString(testSingleLineComment));

        String testHexAndUUID = "Found a hex string: 0x98C72 and a UUID string: {1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed}";
        assertEquals("Found a hex string: ? and a UUID string: ?", R2dbcObfuscator.QUERY_CONVERTER.toObfuscatedQueryString(testHexAndUUID));
    }

    @Test
    public void testPostgresConverter(){
        String testFindUUIDNotHex = "Found {1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed} but not 0x98C72"; //There will be numeric fall-through
        assertEquals("Found ? but not ?x98C72", R2dbcObfuscator.POSTGRES_QUERY_CONVERTER.toObfuscatedQueryString(testFindUUIDNotHex));

        String testFindDollarQuotesNotDoubleQuotes = "Found $message$tagged_string$message$ but not \"double_quoted_string\"";
        assertEquals("Found ? but not \"double_quoted_string\"", R2dbcObfuscator.POSTGRES_QUERY_CONVERTER.toObfuscatedQueryString(testFindDollarQuotesNotDoubleQuotes));
    }

    @Test
    public void testMysqlConverter() {
        String testFindHexNotUUID = "Found 0x98C72 but not {1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed}"; //There will be numeric fall-through
        assertEquals("Found ? but not {?b9d6bcd-bbfd?b2d?b5d-ab8dfbbd4bed}", R2dbcObfuscator.MYSQL_QUERY_CONVERTER.toObfuscatedQueryString((testFindHexNotUUID)));

        String testFindDoubleQuotesNotDollarQuotes = "Found \"double_quoted_string\" but not $message$tagged_string$message$";
        assertEquals("Found ? but not $message$tagged_string$message$", R2dbcObfuscator.MYSQL_QUERY_CONVERTER.toObfuscatedQueryString(testFindDoubleQuotesNotDollarQuotes));
    }
}