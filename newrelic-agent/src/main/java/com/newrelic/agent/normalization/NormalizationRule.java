/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.newrelic.agent.MetricNames;

/**
 * A class representing a renaming rule.
 * 
 * This class is thread-safe.
 */
public class NormalizationRule {

    private static final Pattern SEGMENT_SEPARATOR_PATTERN = Pattern.compile(MetricNames.SEGMENT_DELIMITER_STRING);
    private static final Pattern BACKREFERENCE_PATTERN = Pattern.compile("\\\\(\\d)"); // search for \1, \2 etc.
    private static final String BACKREFERENCE_REPLACEMENT = "\\$$1"; // replace \1 with $1, \2 with $2 etc.

    private final Pattern pattern;
    private final boolean ignore;
    private final boolean terminateChain;
    private final int order;
    private final boolean eachSegment;
    private final boolean replaceAll;
    private final String replaceRegex;
    private final ReplacementFormatter formatter;

    public NormalizationRule(String matchExp, String replacement, boolean ignore, int order, boolean terminateChain,
            boolean eachSegment, boolean replaceAll) throws PatternSyntaxException {
        this.ignore = ignore;
        this.order = order;
        this.terminateChain = terminateChain;
        this.eachSegment = eachSegment;
        this.replaceAll = replaceAll;
        this.pattern = Pattern.compile(matchExp, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        // replace back references (\1 etc) in the replacement pattern with Java-style
        // back references ($1 etc).
        if (replacement == null || replacement.length() == 0) {
            replaceRegex = null;
        } else {
            Matcher backReferenceMatcher = BACKREFERENCE_PATTERN.matcher(replacement);
            replaceRegex = backReferenceMatcher.replaceAll(BACKREFERENCE_REPLACEMENT);
        }

        if (ignore) {
            formatter = new IgnoreReplacementFormatter();
        } else {
            formatter = new FancyReplacementFormatter();
        }
    }

    public boolean isTerminateChain() {
        return terminateChain;
    }

    public RuleResult normalize(String name) {
        return formatter.getRuleResult(name);
    }

    public boolean isIgnore() {
        return ignore;
    }

    public boolean isReplaceAll() {
        return replaceAll;
    }

    public boolean isEachSegment() {
        return eachSegment;
    }

    public String getReplacement() {
        return replaceRegex;
    }

    public int getOrder() {
        return order;
    }

    public String getMatchExpression() {
        return pattern.pattern();
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "match_expression: {0} replacement: {1} eval_order: {2} each_segment: {3} ignore: {4} terminate_chain: {5} replace_all: {6}",
                pattern.pattern(), replaceRegex, order, eachSegment, ignore, terminateChain, replaceAll);
    }

    private interface ReplacementFormatter {
        RuleResult getRuleResult(String url);
    }

    private class IgnoreReplacementFormatter implements ReplacementFormatter {

        @Override
        public RuleResult getRuleResult(String url) {
            if (eachSegment) {
                return forEachSegment(url);
            } else {
                return forEntireUrl(url);
            }
        }

        private RuleResult forEachSegment(String url) {
            String[] segments = SEGMENT_SEPARATOR_PATTERN.split(url);
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i];
                if (segment == null || segment.length() == 0) {
                    continue;
                }
                Matcher matcher = pattern.matcher(segment);
                if (matcher.find()) {
                    return RuleResult.getIgnoreMatch();
                }
            }
            return RuleResult.getNoMatch();

        }

        private RuleResult forEntireUrl(String url) {
            Matcher matcher = pattern.matcher(url);
            return matcher.find() ? RuleResult.getIgnoreMatch() : RuleResult.getNoMatch();
        }
    }

    private class FancyReplacementFormatter implements ReplacementFormatter {
        @Override
        public RuleResult getRuleResult(String url) {
            if (eachSegment) {
                return forEachSegment(url);
            } else {
                return forEntireUrl(url);
            }
        }

        private RuleResult forEachSegment(String url) {
            boolean isMatch = false;
            String[] segments = SEGMENT_SEPARATOR_PATTERN.split(url);
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i];
                if (segment == null || segment.length() == 0) {
                    continue;
                }
                RuleResult result = forEntireUrl(segment);
                if (result.isMatch()) {
                    isMatch = true;
                    segments[i] = result.getReplacement();
                }
            }
            if (!isMatch) {
                return RuleResult.getNoMatch();
            }
            StringBuilder path = new StringBuilder();
            for (int i = 1; i < segments.length; i++) {
                String segment = segments[i];
                if (segment == null || segment.length() == 0) {
                    continue;
                }
                path.append('/').append(segment);
            }
            return RuleResult.getMatch(path.toString());
        }

        private RuleResult forEntireUrl(String url) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String replacement;
                if (replaceRegex == null || replaceRegex.length() == 0) {
                    replacement = null;
                } else if (replaceAll) {
                    replacement = matcher.replaceAll(replaceRegex);
                } else {
                    replacement = matcher.replaceFirst(replaceRegex);
                }
                return RuleResult.getMatch(replacement);
            }
            return RuleResult.getNoMatch();
        }
    }
}
