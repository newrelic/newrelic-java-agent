/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class R2dbcOperation {

    static final Pattern VALID_METRIC_NAME_MATCHER = Pattern.compile("[a-zA-Z0-9.$_@]+");
    static final int PATTERN_SWITCHES = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
    static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    static final Map<String, Pattern[]> OPERATION_PATTERNS = new HashMap<>();

    static {
        OPERATION_PATTERNS.put("SELECT", new Pattern[]{Pattern.compile("^\\s*select.*?\\sfrom[\\s\\[]+([^]\\s,)(;]*).*", PATTERN_SWITCHES)});
        OPERATION_PATTERNS.put("INSERT", new Pattern[]{Pattern.compile("^\\s*insert(?:\\s+ignore)?(?:\\s+into)?\\s+([^\\s(,;]*).*", PATTERN_SWITCHES)});
        OPERATION_PATTERNS.put("UPDATE", new Pattern[]{Pattern.compile("^\\s*update\\s+([^\\s,;]*).*", PATTERN_SWITCHES)});
        OPERATION_PATTERNS.put("DELETE", new Pattern[]{Pattern.compile("^\\s*delete\\s*?.*?\\s+from\\s+([^\\s,(;]*).*", PATTERN_SWITCHES)});
        OPERATION_PATTERNS.put("WITH", new Pattern[]{Pattern.compile("^\\s*with\\s+(?:recursive\\s+)?([^\\s,(;]*)", PATTERN_SWITCHES)});
        OPERATION_PATTERNS.put("CALL", new Pattern[]{Pattern.compile(".*call\\s+([^\\s(,]*).*", PATTERN_SWITCHES)});
        OPERATION_PATTERNS.put("EXEC", new Pattern[]{Pattern.compile(".*(?:exec|execute)\\s+(?!as\\s+)([^\\s(,=;]*+);?\\s*+(?:[^=]|$).*", PATTERN_SWITCHES), Pattern.compile(".*(?:exec|execute)\\s+[^\\s(,]*.*?=(?:\\s|)([^\\s]*)", PATTERN_SWITCHES)});
    }

    public static OperationAndTableName extractFrom(String sql) {
        String strippedSql = COMMENT_PATTERN.matcher(sql).replaceAll("");
        String upperCaseSql = strippedSql.toUpperCase(); //NR-262136, upper case for case-insensitive non-regex-checks
        try {
            for (Map.Entry<String, Pattern[]> operation : OPERATION_PATTERNS.entrySet()) {
                String opName = operation.getKey();
                if (upperCaseSql.contains(opName)) { //NR-262136, non-regex check before pattern matching
                    for (Pattern pattern : operation.getValue()) {
                        Matcher matcher = pattern.matcher(strippedSql);
                        if (matcher.find()) {
                            String model = matcher.groupCount() > 0 ? removeBrackets(unquoteDatabaseName(matcher.group(1).trim())) : "unknown";
                            return new OperationAndTableName(operation.getKey(), VALID_METRIC_NAME_MATCHER.matcher(model).matches() ? model : "ParseError");
                        }
                    }
                }
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static String unquoteDatabaseName(String s) {
        int index = s.indexOf('.');
        if (index > 0) {
            return unquote(s.substring(0, index))
                    + '.'
                    + unquote(s.substring(index + 1));
        }

        return unquote(s);
    }

    private static String unquote(String string) {
        if (string == null || string.length() < 2) {
            return string;
        }

        char first = string.charAt(0);
        char last = string.charAt(string.length() - 1);
        if (first == last && (first == '"' || first == '\'' || first == '`')) {
            return string.substring(1, string.length() - 1);
        }

        return string;
    }

    private static String removeBrackets(String s) {
        return s.replace("[", "")
                .replace("]", "");
    }
}
