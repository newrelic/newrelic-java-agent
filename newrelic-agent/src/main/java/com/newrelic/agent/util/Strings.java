/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * String helper methods.
 */
public class Strings {
    public static final String NEWRELIC_DEPENDENCY_INTERNAL_PACKAGE_PREFIX = "com/newrelic/agent/deps/";

    private Strings() {
    }

    public static Collection<String> trim(Collection<String> strings) {
        Collection<String> trimmedList = new ArrayList<>(strings.size());
        for (String string : strings) {
            trimmedList.add(string.trim());
        }
        return trimmedList;
    }

    public static String unquoteDatabaseName(String s) {
        int index = s.indexOf('.');
        if (index > 0) {
            return unquote(s.substring(0, index))
                    + '.'
                    + unquote(s.substring(index + 1));
        }

        return unquote(s);
    }

    public static String removeBrackets(String s) {
        return s.replace("[", "")
                .replace("]", "");
    }

    /**
     * Join a list of strings with a delimiter. If the first string is empty, its delimiter is included. No delimiters
     * are included for subsequent empty strings.
     */
    public static String join(char delimiter, String... strings) {
        if (strings.length == 0) {
            return null;
        }
        if (strings.length == 1) {
            return strings[0];
        }
        int length = strings.length - 1;
        for (String s : strings) {
            length += s.length();
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            if (!strings[i].isEmpty()) {
                sb.append(delimiter).append(strings[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Remove leading and trailing single-, double-, or back-quotes, if any.
     */
    public static String unquote(String string) {
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

    /**
     * We use shadow jar to repackage third party libraries. The side effect is that it make it hard to reference these
     * libraries in our instrumentation because the class names get rewritten - org/apache/commons/ClassName becomes
     * com/newrelic/agent/deps/org/apache/commons/ClassName.
     *
     * This method rewrites these repackaged class names to their original package.
     */
    public static String fixInternalClassName(String className) {
        className = className.replace('.', '/');
        if (className.startsWith(NEWRELIC_DEPENDENCY_INTERNAL_PACKAGE_PREFIX)) {
            return className.substring(NEWRELIC_DEPENDENCY_INTERNAL_PACKAGE_PREFIX.length());
        }
        return className;
    }


    /**
     * Replaces occurrences of {@code '.'} and {@code '-'} within the string with the underscore character ({@code '_'}).
     *
     * This optimizes the fast path of config reading.
     *
     * @param string The string to replace
     * @return String value with dots and hyphens replaced with underscore character.
     */
    public static String replaceDotHyphenWithUnderscore(String string) {
        // optimize the fast case: no dot or hyphen
        int matchingChar = findDotOrHyphen(string, 0);
        if (matchingChar == -1) {
            return string;
        }

        final StringBuilder updated = new StringBuilder(string.length());
        int start = 0;
        while (matchingChar != -1) {
            updated.append(string, start, matchingChar).append('_');
            start = matchingChar + 1;
            matchingChar = findDotOrHyphen(string, start);
        }
        // append tail
        updated.append(string, start, string.length());
        return updated.toString();
    }

    /**
     * Obfuscate the supplied String. If the String is null or less than 4 characters, return the
     * original String. Otherwise, return the first character + "***" + the last character.
     *
     * @param target The String to obfuscate
     * @return the obfuscated String or the original String if it's null or less than 4 characters
     */
    public static String obfuscate(String target) {
        int MIN_LENGTH = 3;
        if (target != null) {
            if (target.length() <= MIN_LENGTH) {
                return target;
            }

            return target.charAt(0) + "***" + target.charAt(target.length() - 1);
        }

        return null;
    }

    private static int findDotOrHyphen(String string, int start) {
        if (string == null) {
            return -1;
        }
        for (int i = start; i < string.length(); i++) {
            final char ch = string.charAt(i);
            if (ch == '.' || ch == '-') {
                return i;
            }
        }
        return -1;
    }

}
