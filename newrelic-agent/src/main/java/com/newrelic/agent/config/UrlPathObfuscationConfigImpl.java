/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class UrlPathObfuscationConfigImpl extends BaseConfig implements UrlPathObfuscationConfig {
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + AgentConfigImpl.URL_OBFUSCATION + ".";
    public static final String REGEX = "regex";

    public static final boolean ENABLED_DEFAULT = Boolean.FALSE;

    public static final String ENABLED = "enabled";

    private final boolean isEnabled;
    private final UrlPathObfuscationRegExConfigImpl regExConfig;

    public UrlPathObfuscationConfigImpl(Map<String, Object> configSettings) {
        super(configSettings, SYSTEM_PROPERTY_ROOT);
        regExConfig = new UrlPathObfuscationRegExConfigImpl(nestedProps(REGEX));
        isEnabled = getRegExPattern() != null && getProperty(ENABLED, ENABLED_DEFAULT);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Pattern getRegExPattern() {
        return regExConfig.getRegExPattern();
    }

    @Override
    public String getRegExReplacement() {
        return regExConfig.getRegExReplacement();
    }

    /**
     * Small inner class so we can mirror the config key layout of node ("regex" is its own stanza)
     */
    private static final class UrlPathObfuscationRegExConfigImpl extends BaseConfig {
        public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + AgentConfigImpl.URL_OBFUSCATION +
                "." + REGEX + ".";

        public static final Boolean IGNORE_CASE_DEFAULT = Boolean.FALSE;
        public static final String REGEX_REPLACEMENT_DEFAULT = "";

        public static final String REGEX_PATTERN = "pattern";
        public static final String REGEX_REPLACEMENT = "replacement";
        public static final String IGNORE_CASE = "ignore_case";

        private final Pattern regexPattern;
        private final String regexReplacement;

        UrlPathObfuscationRegExConfigImpl(Map<String, Object> configSettings) {
            super(configSettings, SYSTEM_PROPERTY_ROOT);
            regexReplacement = getProperty(REGEX_REPLACEMENT, REGEX_REPLACEMENT_DEFAULT);

            // If the reg ex pattern is invalid, set it to null and force the enabled flag to false
            Pattern tmpRegExPattern = null;
            boolean ignoreCase = getProperty(IGNORE_CASE, IGNORE_CASE_DEFAULT);
            String pattern = null;
            try {
                pattern = getProperty(REGEX_PATTERN);
                if (pattern != null &&  !pattern.trim().isEmpty()) {
                    if (ignoreCase) {
                        tmpRegExPattern = Pattern.compile(pattern,  Pattern.CASE_INSENSITIVE);
                    } else {
                        tmpRegExPattern = Pattern.compile(pattern);
                    }
                }
            } catch (PatternSyntaxException e) {
                Agent.LOG.log(Level.WARNING, "Failed to compile URL obfuscation regex pattern {0}; " +
                        "feature is disabled. Reason: {1}", pattern, e);
            }
            regexPattern = tmpRegExPattern;
        }

        Pattern getRegExPattern() {
            return regexPattern;
        }

        String getRegExReplacement() {
            return regexReplacement;
        }
    }
}
