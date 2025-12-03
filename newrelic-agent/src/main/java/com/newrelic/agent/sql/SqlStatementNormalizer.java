/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.sql;

public class SqlStatementNormalizer {
    public static String normalizeSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }

        return removeCommentsAndNormalizeWhitespace(sql).toUpperCase();
    }

    /**
     * Strip comments and normalize white space from the supplied SQL statement. It handles multiline comments
     * and single line comments.
     * </br>
     * This is a bit ugly, but this is faster than doing this via a couple of complex regular expressions.
     *
     * @param sql the SQL to remove comments from
     *
     * @return the modified SQL statement
     */
    private static  String removeCommentsAndNormalizeWhitespace(String sql) {
        StringBuilder result = new StringBuilder();
        SqlNormalizerState state = new SqlNormalizerState(sql);

        while (state.hasMore()) {
            char current = state.current();

            if (current == '\'') {
                processStringLiteral(result, state);
            } else if (isMultilineCommentStart(state)) {
                processMultilineComment(state);
            } else if (isSingleLineCommentStart(state)) {
                processSingleLineComment(state);
            } else if (current == '#') {
                processHashComment(state);
            } else if (Character.isWhitespace(current)) {
                processWhitespace(result, state);
            } else {
                processRegularCharacter(result, state);
            }
        }

        return result.toString().trim();
    }

    private static  void processStringLiteral(StringBuilder result, SqlNormalizerState state) {
        result.append(state.current());
        state.setLastWasWhitespace(false);
        state.advance();

        while (state.hasMore()) {
            char c = state.current();
            result.append(c);

            if (c == '\'') {
                // Escaped quote '' check
                if (state.hasNext() && state.peek() == '\'') {
                    result.append('\'');
                    state.advance(2);
                } else {
                    state.advance();
                    break;
                }
            } else {
                state.advance();
            }
        }
        state.setLastWasWhitespace(false);
    }

    private static  boolean isMultilineCommentStart(SqlNormalizerState state) {
        return state.current() == '/' && state.hasNext() && state.peek() == '*';
    }

    private static  void processMultilineComment(SqlNormalizerState state) {
        state.advance(2); // Skip /*

        while (state.idx < state.length - 1) {
            if (state.current() == '*' && state.peek() == '/') {
                state.advance(2);
                break;
            }
            state.advance();
        }

        // Handle unclosed comment
        if (state.idx == state.length - 1) {
            state.idx = state.length;
        }
    }

    private static  boolean isSingleLineCommentStart(SqlNormalizerState state) {
        return state.current() == '-' && state.hasNext() && state.peek() == '-';
    }

    private static  void processSingleLineComment(SqlNormalizerState state) {
        state.advance(2); // Skip --
        skipToEndOfLine(state);
    }

    private static  void processHashComment(SqlNormalizerState state) {
        state.advance(); // Skip #
        skipToEndOfLine(state);
    }

    private static  void skipToEndOfLine(SqlNormalizerState state) {
        // Skip until newline
        while (state.hasMore() && state.current() != '\n' && state.current() != '\r') {
            state.advance();
        }
        // Skip the newline character(s)
        while (state.hasMore() && (state.current() == '\n' || state.current() == '\r')) {
            state.advance();
        }
    }

    private static  void processWhitespace(StringBuilder result, SqlNormalizerState state) {
        if (!state.isLastWhitespace() && result.length() > 0) {
            result.append(' ');
            state.setLastWasWhitespace(true);
        }
        state.advance();
    }

    private static  void processRegularCharacter(StringBuilder result, SqlNormalizerState state) {
        result.append(state.current());
        state.setLastWasWhitespace(false);
        state.advance();
    }

    private static class SqlNormalizerState {
        private final String sql;
        private final int length;
        private int idx;
        private boolean lastWasWhitespace;

        SqlNormalizerState(String sql) {
            this.sql = sql;
            this.length = sql.length();
            this.idx = 0;
            this.lastWasWhitespace = true; // Start as true to trim leading whitespace
        }

        boolean hasMore() {
            return idx < length;
        }

        boolean hasNext() {
            return idx + 1 < length;
        }

        char current() {
            return sql.charAt(idx);
        }

        char peek() {
            return sql.charAt(idx + 1);
        }

        void advance() {
            idx++;
        }

        void advance(int count) {
            idx += count;
        }

        void setLastWasWhitespace(boolean newValue) {
            lastWasWhitespace = newValue;
        }

        boolean isLastWhitespace() {
            return lastWasWhitespace;
        }
    }
}
