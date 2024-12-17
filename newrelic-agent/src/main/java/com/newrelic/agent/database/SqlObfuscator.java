/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.google.common.base.Joiner;
import com.newrelic.api.agent.QueryConverter;
import jregex.Pattern;

import java.util.HashMap;
import java.util.Map;

/**
 * The agent can be configured to report raw sql in transaction traces, report no sql at all, or report sql with string
 * and numeric literals replaced with the ? character.
 */
public abstract class SqlObfuscator {
    private static final String SINGLE_QUOTE = "'(?:[^']|'')*?(?:\\\\'.*|'(?!'))";
    private static final String DOUBLE_QUOTE = "\"(?:[^\"]|\"\")*?(?:\\\\\".*|\"(?!\"))";
    private static final String DOLLAR_QUOTE = "(\\$(?!\\d)[^$]*?\\$).*?(?:\\1|$)";
    private static final String ORACLE_QUOTE = "q'\\[.*?(?:\\]'|$)|q'\\{.*?(?:\\}'|$)|q'<.*?(?:>'|$)|q'\\(.*?(?:\\)'|$)";
    private static final String COMMENT = "(?:#|--).*?(?=\\r|\\n|$)";
    private static final String MULTILINE_COMMENT = "/\\*(?:[^/]|/[^*])*?(?:\\*/|/\\*.*)";
    private static final String UUID = "\\{?(?:[0-9a-f]\\-*){32}\\}?";
    private static final String HEX = "0x[0-9a-f]+";
    private static final String BOOLEAN = "\\b(?:true|false|null)\\b";
    private static final String NUMBER = "-?\\b(?:[0-9_]+\\.)?[0-9_]+([eE][+-]?[0-9_]+)?";

    public static final String OBFUSCATED_SETTING = "obfuscated";
    public static final String RAW_SETTING = "raw";
    public static final String OFF_SETTING = "off";
    private final QueryConverter<String> queryConverter = new QueryConverter<String>() {
        @Override
        public String toRawQueryString(String rawQuery) {
            return rawQuery;
        }

        @Override
        public String toObfuscatedQueryString(String rawQuery) {
            return obfuscateSql(rawQuery);
        }
    };

    private SqlObfuscator() {
    }

    /**
     * Obfuscates a sql statement with an unknown dialect. The obfuscator will attempt to obfuscate using a combination
     * of all known dialects. If that fails to obfuscate then "?" will be returned.
     * 
     * If the dialect is known, {@link #obfuscateSql(String, String)} should be used instead.
     * 
     * @param sql the sql string to obfuscate
     * @return an obfuscated version of the sql passed in
     */
    public abstract String obfuscateSql(String sql);

    /**
     * Obfuscated a sql statement with the given dialect. If the dialect is supported we will return a fully obfuscated
     * version of the sql. If the dialect is not supported the obfuscator will attempt to obfuscate using a combination
     * of all known dialects. If that still fails to obfuscate then "?" will be returned.
     * @param sql the sql string to obfuscate
     * @param dialect the dialect to obfuscate
     * @return an obfuscated version of the sql passed in
     */
    public abstract String obfuscateSql(String sql, String dialect);

    public boolean isObfuscating() {
        return false;
    }

    public <String> QueryConverter<java.lang.String> getQueryConverter() {
        return queryConverter;
    }

    static class DefaultSqlObfuscator extends SqlObfuscator {
        private static final Pattern ALL_DIALECTS_PATTERN;
        private static final Pattern ALL_UNMATCHED_PATTERN;
        private static final Pattern MYSQL_DIALECT_PATTERN;
        private static final Pattern MYSQL_UNMATCHED_PATTERN;
        private static final Pattern POSTGRES_DIALECT_PATTERN;
        private static final Pattern POSTGRES_UNMATCHED_PATTERN;
        private static final Pattern ORACLE_DIALECT_PATTERN;
        private static final Pattern ORACLE_UNMATCHED_PATTERN;

        static {
            String allDialectsPattern = Joiner.on("|").join(SINGLE_QUOTE, DOUBLE_QUOTE, DOLLAR_QUOTE, ORACLE_QUOTE,
                    COMMENT, MULTILINE_COMMENT, UUID, HEX, BOOLEAN, NUMBER);
            
            String mysqlDialectPattern = Joiner.on("|").join(SINGLE_QUOTE, DOUBLE_QUOTE, COMMENT, MULTILINE_COMMENT,
                    HEX, BOOLEAN, NUMBER);
            String postgresDialectPattern = Joiner.on("|").join(SINGLE_QUOTE, DOLLAR_QUOTE, COMMENT, MULTILINE_COMMENT,
                    UUID, BOOLEAN, NUMBER);
            String oracleDialectPattern = Joiner.on("|").join(SINGLE_QUOTE, ORACLE_QUOTE, COMMENT, MULTILINE_COMMENT,
                    NUMBER);

            ALL_DIALECTS_PATTERN = new Pattern(allDialectsPattern, Pattern.DOTALL | Pattern.IGNORE_CASE);
            ALL_UNMATCHED_PATTERN = new Pattern("'|\"|/\\*|\\*/|\\$", Pattern.DOTALL | Pattern.IGNORE_CASE);
            MYSQL_DIALECT_PATTERN = new Pattern(mysqlDialectPattern, Pattern.DOTALL | Pattern.IGNORE_CASE);
            MYSQL_UNMATCHED_PATTERN = new Pattern("'|\"|/\\*|\\*/", Pattern.DOTALL | Pattern.IGNORE_CASE);
            POSTGRES_DIALECT_PATTERN = new Pattern(postgresDialectPattern, Pattern.DOTALL | Pattern.IGNORE_CASE);
            POSTGRES_UNMATCHED_PATTERN = new Pattern("'|/\\*|\\*/|\\$(?!\\?)", Pattern.DOTALL | Pattern.IGNORE_CASE);
            ORACLE_DIALECT_PATTERN = new Pattern(oracleDialectPattern, Pattern.DOTALL | Pattern.IGNORE_CASE);
            ORACLE_UNMATCHED_PATTERN = new Pattern("'|/\\*|\\*/", Pattern.DOTALL | Pattern.IGNORE_CASE);

        }

        @Override
        public String obfuscateSql(String sql) {
            if (sql == null || sql.length() == 0) {
                return sql;
            }
            String obfuscatedSql = ALL_DIALECTS_PATTERN.replacer("?").replace(sql);
            return checkForUnmatchedPairs(ALL_UNMATCHED_PATTERN, obfuscatedSql);
        }

        @Override
        public String obfuscateSql(String sql, String dialect) {
            if (sql == null || sql.length() == 0) {
                return sql;
            }
            if (dialect.equals("mysql")) {
                String obfuscatedSql = MYSQL_DIALECT_PATTERN.replacer("?").replace(sql);
                return checkForUnmatchedPairs(MYSQL_UNMATCHED_PATTERN, obfuscatedSql);
            } else if (dialect.equals("postgresql") || dialect.equals("postgres")) {
                String obfuscatedSql = POSTGRES_DIALECT_PATTERN.replacer("?").replace(sql);
                return checkForUnmatchedPairs(POSTGRES_UNMATCHED_PATTERN, obfuscatedSql);
            } else if (dialect.equals("oracle")) {
                String obfuscatedSql = ORACLE_DIALECT_PATTERN.replacer("?").replace(sql);
                return checkForUnmatchedPairs(ORACLE_UNMATCHED_PATTERN, obfuscatedSql);
            }
            return obfuscateSql(sql);
        }

        @Override
        public boolean isObfuscating() {
            return true;
        }

        /**
         * This method will check to see if there are any open single quotes, double quotes or comment open/closes still
         * left in the obfuscated string. If so, it means something didn't obfuscate properly so we will return "?"
         * instead to prevent any data from leaking.
         */
        private String checkForUnmatchedPairs(Pattern pattern, String obfuscatedSql) {
            return pattern.matcher(obfuscatedSql).find() ? "?" : obfuscatedSql;
        }
    }

    public static SqlObfuscator getDefaultSqlObfuscator() {
        return new DefaultSqlObfuscator();
    }

    static SqlObfuscator getNoObfuscationSqlObfuscator() {
        return new SqlObfuscator() {

            @Override
            public String obfuscateSql(String sql) {
                return sql;
            }

            @Override
            public String obfuscateSql(String sql, String dialect) {
                return sql;
            }
        };
    }

    static SqlObfuscator getNoSqlObfuscator() {
        return new SqlObfuscator() {

            @Override
            public String obfuscateSql(String sql) {
                return null;
            }

            @Override
            public String obfuscateSql(String sql, String dialect) {
                return null;
            }
        };
    }

    public static SqlObfuscator getCachingSqlObfuscator(SqlObfuscator sqlObfuscator) {
        if (sqlObfuscator.isObfuscating()) {
            return new CachingSqlObfuscator(sqlObfuscator);
        } else {
            return sqlObfuscator;
        }
    }

    static class CachingSqlObfuscator extends SqlObfuscator {
        private final Map<String, String> cache = new HashMap<>();
        private final SqlObfuscator sqlObfuscator;

        public CachingSqlObfuscator(SqlObfuscator sqlObfuscator) {
            this.sqlObfuscator = sqlObfuscator;
        }

        @Override
        public String obfuscateSql(String sql) {
            String obfuscatedSql = cache.get(sql);
            if (obfuscatedSql == null) {
                obfuscatedSql = sqlObfuscator.obfuscateSql(sql);
                cache.put(sql, obfuscatedSql);
            }
            return obfuscatedSql;
        }

        @Override
        public String obfuscateSql(String sql, String dialect) {
            String obfuscatedSql = cache.get(sql);
            if (obfuscatedSql == null) {
                obfuscatedSql = sqlObfuscator.obfuscateSql(sql, dialect);
                cache.put(sql, obfuscatedSql);
            }
            return obfuscatedSql;
        }

        @Override
        public boolean isObfuscating() {
            return sqlObfuscator.isObfuscating();
        }

    }
}
