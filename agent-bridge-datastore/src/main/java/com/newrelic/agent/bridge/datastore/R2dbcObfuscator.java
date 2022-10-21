/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.api.agent.QueryConverter;

import java.util.regex.Pattern;

public class R2dbcObfuscator {
    private static final int PATTERN_SWITCHES = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
    private static final String SINGLE_QUOTE = "'(?:[^']|'')*?(?:\\\\'.*|'(?!'))";
    private static final String DOUBLE_QUOTE = "\"(?:[^\"]|\"\")*?(?:\\\\\".*|\"(?!\"))";
    private static final String DOLLAR_QUOTE = "(\\$(?!\\d)[^$]*?\\$).*?(?:\\1|$)";
    private static final String COMMENT = "(?:#|--).*?(?=\\r|\\n|$)";
    private static final String MULTILINE_COMMENT = "/\\*(?:[^/]|/[^*])*?(?:\\*/|/\\*.*)";
    private static final String UUID = "\\{?(?:[0-9a-f]\\-*){32}\\}?";
    private static final String HEX = "0x[0-9a-f]+";
    private static final String BOOLEAN = "\\b(?:true|false|null)\\b";
    private static final String NUMBER = "-?\\b(?:[0-9_]+\\.)?[0-9_]+([eE][+-]?[0-9_]+)?";
    private static final Pattern ALL_DIALECTS_PATTERN;
    private static final Pattern ALL_UNMATCHED_PATTERN;
    private static final Pattern MYSQL_DIALECT_PATTERN;
    private static final Pattern MYSQL_UNMATCHED_PATTERN;
    private static final Pattern POSTGRES_DIALECT_PATTERN;
    private static final Pattern POSTGRES_UNMATCHED_PATTERN;

    public static final QueryConverter<String> QUERY_CONVERTER;
    public static final QueryConverter<String> MYSQL_QUERY_CONVERTER;
    public static final QueryConverter<String> POSTGRES_QUERY_CONVERTER;

    static {
        ALL_DIALECTS_PATTERN = Pattern.compile(String.join("|", SINGLE_QUOTE, DOUBLE_QUOTE, DOLLAR_QUOTE, COMMENT, MULTILINE_COMMENT, UUID, HEX, BOOLEAN, NUMBER), PATTERN_SWITCHES);
        ALL_UNMATCHED_PATTERN = Pattern.compile("'|\"|/\\*|\\*/|\\$", PATTERN_SWITCHES);
        MYSQL_DIALECT_PATTERN = Pattern.compile(String.join("|", SINGLE_QUOTE, DOUBLE_QUOTE, COMMENT, MULTILINE_COMMENT, HEX, BOOLEAN, NUMBER), PATTERN_SWITCHES);
        MYSQL_UNMATCHED_PATTERN = Pattern.compile("'|\"|/\\*|\\*/", PATTERN_SWITCHES);
        POSTGRES_DIALECT_PATTERN = Pattern.compile(String.join("|", SINGLE_QUOTE, DOLLAR_QUOTE, COMMENT, MULTILINE_COMMENT, UUID, BOOLEAN, NUMBER), PATTERN_SWITCHES);
        POSTGRES_UNMATCHED_PATTERN = Pattern.compile("'|/\\*|\\*/|\\$(?!\\?)", PATTERN_SWITCHES);


        QUERY_CONVERTER = new QueryConverter<String>() {
            @Override
            public String toRawQueryString(String statement) {
                return statement;
            }

            @Override
            public String toObfuscatedQueryString(String statement) {
                return obfuscateSql(statement, ALL_DIALECTS_PATTERN, ALL_UNMATCHED_PATTERN);
            }
        };

        MYSQL_QUERY_CONVERTER = new QueryConverter<String>() {
            @Override
            public String toRawQueryString(String statement) {
                return statement;
            }

            @Override
            public String toObfuscatedQueryString(String statement) {
                return obfuscateSql(statement, MYSQL_DIALECT_PATTERN, MYSQL_UNMATCHED_PATTERN);
            }
        };

        POSTGRES_QUERY_CONVERTER = new QueryConverter<String>() {
            @Override
            public String toRawQueryString(String statement) {
                return statement;
            }

            @Override
            public String toObfuscatedQueryString(String statement) {
                return obfuscateSql(statement, POSTGRES_DIALECT_PATTERN, POSTGRES_UNMATCHED_PATTERN);
            }
        };
    }

    private static String obfuscateSql(String sql, Pattern dialect, Pattern unmatched) {
        if (sql == null || sql.length() == 0) {
            return sql;
        }
        String obfuscatedSql = dialect.matcher(sql).replaceAll("?");
        return unmatched.matcher(obfuscatedSql).find() ? "?" : obfuscatedSql;
    }
}
