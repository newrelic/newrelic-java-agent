package com.nr.instrumentation.graphql;

import graphql.com.google.common.base.Joiner;
import java.util.regex.Pattern;

public class GraphQLObfuscateUtil {
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

    private static final Pattern ALL_DIALECTS_PATTERN;
    private static final Pattern ALL_UNMATCHED_PATTERN;

    static {
        String allDialectsPattern = Joiner.on("|").join(SINGLE_QUOTE, DOUBLE_QUOTE, DOLLAR_QUOTE, ORACLE_QUOTE,
                COMMENT, MULTILINE_COMMENT, UUID, HEX, BOOLEAN, NUMBER);

        ALL_DIALECTS_PATTERN = Pattern.compile(allDialectsPattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        ALL_UNMATCHED_PATTERN = Pattern.compile("'|\"|/\\*|\\*/|\\$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    }

    public static String obfuscateQuery(String query){
        if (query == null || query.length() == 0) {
            return query;
        }

        //fixme Too eager, replaces __ in graph query with *** . Should not replace
        String obfuscatedQuery = ALL_DIALECTS_PATTERN.matcher(query).replaceAll("***");
        return checkForUnmatchedPairs(ALL_UNMATCHED_PATTERN, obfuscatedQuery);
    }

    private static String checkForUnmatchedPairs(Pattern pattern, String obfuscatedQuery) {
        return pattern.matcher(obfuscatedQuery).find() ? "***" : obfuscatedQuery;
    }
}


