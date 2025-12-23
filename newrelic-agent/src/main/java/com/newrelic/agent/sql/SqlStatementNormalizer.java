/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.sql;

/**
 * Normalizes a SQL statement to a consistent form for cross-language comparison.
 * </br> </br>
 * Converts to uppercase </br>
 * Normalizes all parameter placeholders (?, $1, :name, @name, %(name)s) to '?' </br>
 * Replaces string and numeric literals with '?' </br>
 * Removes comments </br>
 * Normalizes whitespace </br>
 */
public class SqlStatementNormalizer {
    /**
     * Normalize a SQL statement based on the rules in the class javadoc.
     * This is a bit of an ugly state machine, but it was significantly
     * faster than running multiple regular expressions against the statement.
     *
     * @param sql the SQL statement to normalize
     *
     * @return the normalized SQL statement
     */
    public static String normalizeSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }

        sql = sql.toUpperCase();

        // Handle SQL params and literals
        sql = normalizeParametersAndLiterals(sql);

        // Remove comments/whitespace
        return removeCommentsAndNormalizeWhitespace(sql);
    }

    /**
     * Normalizes all parameter placeholders and literals to a standard form.
     * Handles multiple placeholder styles: ?, $1, :name, @name, %(name)s
     * Replaces string and numeric literals with '?'
     *
     * @param sql the SQL statement to normalize
     *
     * @return SQL with normalized parameters
     */
    private static String normalizeParametersAndLiterals(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(sql.length());
        SqlNormalizerState state = new SqlNormalizerState(sql);

        while (state.hasMore()) {
            char current = state.current();

            if (current == '\'') {
                // Replace string literals with ?
                skipStringLiteral(state);
                result.append('?');
            } else if (current == '(') {
                // Check for IN clause with multiple values/placeholders
                if (isPrecededByIn(result)) {
                    String inClause = tryNormalizeInClause(state);
                    result.append(inClause);
                } else {
                    result.append('(');
                    state.advance();
                }
            } else if (isNumericLiteral(state)) {
                // Numeric literals
                skipNumericLiteral(state);
                result.append('?');
            } else if (isPlaceholder(state)) {
                // Any placeholder type --> ?
                skipPlaceholder(state);
                result.append('?');
            } else {
                // Just append anything else
                result.append(current);
                state.advance();
            }
        }

        return result.toString();
    }

    /**
     * Check if the result StringBuilder is preceded by "IN".
     * Handles whitespace between "IN" and the current position.
     */
    private static boolean isPrecededByIn(StringBuilder result) {
        if (result.length() < 2) {
            return false;
        }

        // Scan backwards, skipping whitespace
        int idx = result.length() - 1;
        while (idx >= 0 && Character.isWhitespace(result.charAt(idx))) {
            idx--;
        }

        // Check if we have at least "IN" (2 characters)
        if (idx < 1) {
            return false;
        }

        // Check for "IN" - scanning backwards we see 'N' first, then 'I'
        if (result.charAt(idx) == 'N' && result.charAt(idx - 1) == 'I') {
            // Make sure "IN" is a complete token, not part of a larger word like "WITHIN"
            return idx < 2 || !isIdentifierChar(result.charAt(idx - 2));
        }

        return false;
    }

    /**
     * Checks if a character is valid in an identifier (letter, digit, or underscore).
     */
    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Checks if current position is a parameter placeholder.
     * Supports: ?, $1, :name, @name, %(name)s
     */
    private static boolean isPlaceholder(SqlNormalizerState state) {
        char c = state.current();

        // JDBC/MySQL style: ?
        if (c == '?') {
            return true;
        }

        // PostgreSQL style: $1, $2...
        if (c == '$' && state.hasNext() && Character.isDigit(state.peek())) {
            return true;
        }

        // Oracle/Python style: :name or :1
        if (c == ':' && state.hasNext() && isIdentifierChar(state.peek())) {
            return true;
        }

        // SQL Server style: @name or @p1
        if (c == '@' && state.hasNext() && isIdentifierChar(state.peek())) {
            return true;
        }

        // Python style: %(name)s
        if (c == '%' && state.hasNext() && state.peek() == '(') {
            return true;
        }

        return false;
    }

    /**
     * Skips over any type of prepared statement placeholder
     */
    private static void skipPlaceholder(SqlNormalizerState state) {
        char c = state.current();

        if (c == '?') {
            state.advance();
        } else if (c == '$') {
            // PostgreSQL: $1, $2...
            state.advance(); // Skip $
            while (state.hasMore() && Character.isDigit(state.current())) {
                state.advance();
            }
        } else if (c == ':' || c == '@') {
            // Oracle/Python/SQL Server: :NAME, @NAME
            state.advance(); // Skip : or @
            while (state.hasMore() && isIdentifierChar(state.current())) {
                state.advance();
            }
        } else if (c == '%' && state.hasNext() && state.peek() == '(') {
            // Python: %(NAME)S
            state.advance(2); // Skip %(
            while (state.hasMore() && state.current() != ')') {
                state.advance();
            }
            if (state.hasMore()) {
                state.advance(); // Skip )
            }
            if (state.hasMore() && state.current() == 'S') {
                state.advance(); // Skip S (uppercased)
            }
        }
    }

    /**
     * Checks if current position is a numeric literal.
     * Avoids numbers that are part of other tokens (like "column1").
     */
    private static boolean isNumericLiteral(SqlNormalizerState state) {
        char c = state.current();

        // Check for digit, minus, plus, or decimal point
        if (!Character.isDigit(c) && c != '-' && c != '+' && c != '.') {
            return false;
        }

        // Make sure it's not part of an identifier
        if (state.idx > 0) {
            char prev = state.sql.charAt(state.idx - 1);
            // If preceded by letter, digit, underscore, or backtick, it's part of identifier
            if (Character.isLetter(prev) || prev == '_' || prev == '`') {
                return false;
            }
        }

        // Look ahead to confirm it's a complete number
        int savedIdx = state.idx;

        // Handle optional sign
        if (c == '-' || c == '+') {
            state.advance();
            if (!state.hasMore()) {
                state.idx = savedIdx;
                return false;
            }
            c = state.current();
        }

        // Numbers starting with decimal point
        if (c == '.') {
            state.advance();
            if (!state.hasMore() || !Character.isDigit(state.current())) {
                state.idx = savedIdx;
                return false;
            }
            // Looks like an actual decimal number
            state.idx = savedIdx;
            return true;
        }

        // Must have at least one digit before optional decimal point
        if (!Character.isDigit(c)) {
            state.idx = savedIdx;
            return false;
        }

        state.idx = savedIdx;
        return true;
    }

    /**
     * Skips over a numeric literal.
     */
    private static void skipNumericLiteral(SqlNormalizerState state) {
        // + or - sign
        char c = state.current();
        if (c == '-' || c == '+') {
            state.advance();
        }

        // Skip any digits
        while (state.hasMore() && Character.isDigit(state.current())) {
            state.advance();
        }

        // Decimal points
        if (state.hasMore() && state.current() == '.') {
            state.advance();
            while (state.hasMore() && Character.isDigit(state.current())) {
                state.advance();
            }
        }

        // Scientific notation (1e10, 1E-5)
        if (state.hasMore() && state.current() == 'E') {
            state.advance();
            if (state.hasMore() && (state.current() == '+' || state.current() == '-')) {
                state.advance();
            }
            while (state.hasMore() && Character.isDigit(state.current())) {
                state.advance();
            }
        }
    }

    /**
     * Skips over a string literal, handling escaped quotes.
     */
    private static void skipStringLiteral(SqlNormalizerState state) {
        state.advance(); // Skips the opening quote

        while (state.hasMore()) {
            char c = state.current();

            if (c == '\'') {
                // Check for escaped quote ''
                if (state.hasNext() && state.peek() == '\'') {
                    state.advance(2); // Skip both quotes
                } else {
                    state.advance(); // Skip closing quote
                    break;
                }
            } else if (c == '\\') {
                // Handle backslash escaping (MySQL, PostgreSQL)
                state.advance();
                if (state.hasMore()) {
                    state.advance();
                }
            } else {
                state.advance();
            }
        }
    }

    /**
     * Tries to normalize an IN clause like IN (1,2,3) or IN (?,?,?) to IN (?).
     * If it's not a simple IN clause, returns the opening paren as-is.
     */
    private static String tryNormalizeInClause(SqlNormalizerState state) {
        // Save position in case we need to backtrack
        int saveIdx = state.idx;

        state.advance(); // Opening (

        int itemCount = 0;
        boolean allParametersOrLiterals = true;
        boolean foundNonWhitespace = false;

        // Scan the contents of the parentheses
        while (state.hasMore() && state.current() != ')') {
            char c = state.current();

            if (Character.isWhitespace(c)) {
                state.advance();
            } else if (c == ',') {
                state.advance();
            } else if (isPlaceholder(state)) {
                foundNonWhitespace = true;
                itemCount++;
                skipPlaceholder(state);
            } else if (isNumericLiteral(state)) {
                foundNonWhitespace = true;
                itemCount++;
                skipNumericLiteral(state);
            } else if (c == '\'') {
                foundNonWhitespace = true;
                itemCount++;
                skipStringLiteral(state);
            } else {
                // Not a list, bail
                allParametersOrLiterals = false;
                break;
            }
        }

        // Check if we found a closing paren and have multiple items
        if (allParametersOrLiterals && foundNonWhitespace && itemCount > 1 &&
            state.hasMore() && state.current() == ')') {
            state.advance(); // Skip closing )
            return "(?)";
        }

        // Not a normalizable IN clause, restore position
        state.idx = saveIdx;
        state.advance();
        return "(";
    }

    /**
     * Strip comments and normalize white space from the supplied SQL statement. It handles multiline comments
     * and single line comments.
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
