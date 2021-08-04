/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.threads;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;

/**
 * Attempts to take thread names and normalize them to prevent MGIs
 */
public class ThreadNameNormalizer {

    private static final char REPLACEMENT_CHAR = '#';
    private static final String REPLACEMENT_STRING = new String(new char[] { REPLACEMENT_CHAR });

    /*
     * Pattern matches hex numbers at least 8 characters long surrounded by
     * word breaks, or a number of any length.  We use this to normalize thread names into groups.
     */
    private static final String DEFAULT_PATTERN = "((?<=[\\W_]|^)([0-9a-fA-F]){4,}(?=[\\W_]|$))|\\d+";
    
    private static final List<ReplacementRule> REPLACEMENT_RULES =
            ImmutableList.of(
                getConstantRegexReplacementRule(".* (GET|PUT|POST|DELETE|HEAD) .*", "WebRequest" + REPLACEMENT_CHAR),
                getConstantRegexReplacementRule("pool-.*thread.*", "pool" + REPLACEMENT_CHAR + "thread" + REPLACEMENT_CHAR),
                getPrefixReplacementRule(
                        "ActiveMQ",
                        "BoneCP-keep-alive-scheduler",
                        "CookieBrokerUpdates",
                        "C3P0PooledConnectionPoolManager",
                        "default-akka.actor.default",
                        "DelayedAutomationRunner",
                        "elasticsearch",
                        "hystrix",
                        "org.eclipse.jetty.server.session.HashSessionManager",
                        "jbm-client-session",
                        "JobHandler",
                        "QuartzScheduler",
                        "Sending mailitem",
                        "SOAPProcessorThread",
                        "TransientResourceLock"),
                getReplaceAfterMatchRule("http:", "https:", "uri:", "@"),
                /* many thread names place variables in braces */
                getEnclosingCharactersReplacementRule('{', '}'),
                getEnclosingCharactersReplacementRule('(', ')'),
                getEnclosingCharactersReplacementRule('[', ']'));
    private final Pattern replacementPattern;
    private final ThreadNames threadNames;

    public ThreadNameNormalizer(AgentConfig config, ThreadNames threadNames) {
        this(config.getValue(ThreadService.NAME_PATTERN_CFG_KEY, DEFAULT_PATTERN), threadNames);
    }

    /**
     * For testing.
     */
    public ThreadNameNormalizer(ThreadNames threadNames) {
        this(DEFAULT_PATTERN, threadNames);
    }

    private ThreadNameNormalizer(String pattern, ThreadNames threadNames) {
        replacementPattern = Pattern.compile(pattern);
        this.threadNames = threadNames;
    }

    public String getNormalizedThreadName(BasicThreadInfo threadInfo) {
        return getNormalizedThreadName(threadNames.getThreadName(threadInfo));
    }

    protected String getNormalizedThreadName(String name) {
        for (ReplacementRule rule : REPLACEMENT_RULES) {
            ReplacementResult result = rule.getResult(name);
            if (null != result) {
                name = result.replacement;
                if (result.stop) {
                    return name;
                }
            }
        }
        
        // Don't replace with a '*' because that messes up the dashboard metric selector
        String renamed = replacementPattern.matcher(name).replaceAll(REPLACEMENT_STRING);
        // Remove the slash characters for proper metric interpretation
        return renamed.replace('/', '-');
    }
    
    private static class ReplacementResult {
        final boolean stop;
        final String replacement;
        public ReplacementResult(boolean stop, String replacement) {
            super();
            this.stop = stop;
            this.replacement = replacement;
        }
    }
    
    private interface ReplacementRule {
        ReplacementResult getResult(String name);
    }
    
    private abstract static class RegexReplacementRule implements ReplacementRule {
        private final Pattern pattern;
        
        public RegexReplacementRule(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        @Override
        public ReplacementResult getResult(String name) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                return getResult(name, matcher);
            }
            return null;
        }

        protected abstract ReplacementResult getResult(String name, Matcher matcher);
    }
    
    /**
     * Returns a rule that replaces strings wrapped with the given enclosing characters.
     * I wasn't able to write this as a regex because of nested enclosing characters, ie
     *   [[thing] [more stuff]]
     */
    private static ReplacementRule getEnclosingCharactersReplacementRule(final char startChar, final char endChar) {
        return new ReplacementRule() {

            @Override
            public ReplacementResult getResult(String name) {
                int index = 0;
                boolean replace = false;
                while (index < name.length() && (index = name.indexOf(startChar, index)) >= 0) {
                    replace = true;
                    int found = 1;
                    int i = index + 1;
                    for (; i < name.length() && found > 0; i++) {
                        if (name.charAt(i) == startChar) {
                            found++;
                        } else if (name.charAt(i) == endChar) {
                            found--;
                        }
                    }
                    
                    String replacement = name.substring(0, index) + startChar + REPLACEMENT_CHAR + endChar;
                    index = replacement.length();
                    if (i == name.length()) {
                        name = replacement;
                    } else {
                        name = replacement + name.substring(i, name.length());
                    }
                }
                return replace ? new ReplacementResult(false, name) : null;
            }
            
        };
    }
    
    private static ReplacementRule getConstantRegexReplacementRule(final String regex, final String replacement) {
        final ReplacementResult replacementResult = new ReplacementResult(true, replacement);
        return new RegexReplacementRule(regex) {
            @Override
            protected ReplacementResult getResult(String name, Matcher matcher) {
                return replacementResult;
            }
            
        };
    }
    
    /**
     * Returns a rule that matches any one of the given terms as a prefix.
     */
    private static ReplacementRule getPrefixReplacementRule(String... terms) {
        return getGroupRegexReplacementRule('(' + Joiner.on('|').join(terms) + ").*", 1, true);
    }
    
    private static ReplacementRule getReplaceAfterMatchRule(String... patterns) {
        final Set<String> patternsList = Sets.newHashSet(patterns);
        for (String p : patterns) {
            patternsList.add(p.toUpperCase());
        }
        return new ReplacementRule() {

            @Override
            public ReplacementResult getResult(String name) {
                for (String p : patternsList) {
                    int index = name.indexOf(p);
                    if (index >= 0) {
                        return new ReplacementResult(false, name.substring(0, index) + p + REPLACEMENT_CHAR);
                    }
                }
                return null;
            }
        };
    }
    
    private static ReplacementRule getGroupRegexReplacementRule(final String regex, final int groupId, final boolean stop) {
        return new RegexReplacementRule(regex) {
            @Override
            protected ReplacementResult getResult(String name, Matcher matcher) {
                return new ReplacementResult(stop, matcher.group(groupId) + REPLACEMENT_CHAR);
            }
            
        };
    }
}
