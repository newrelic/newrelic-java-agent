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

    public static final String ENABLED = "enabled";
    public static final String REGEX_PATTERN = "regex_pattern";
    public static final String REGEX_REPLACEMENT = "regex_replacement";
    public static final String CASE_INSENSITIVE = "case_insensitive";

    public static final Boolean ENABLED_DEFAULT = Boolean.FALSE;
    public static final Boolean CASE_INSENSITIVE_DEFAULT = Boolean.TRUE;
    public static final String REGEX_REPLACEMENT_DEFAULT = "";

    private final boolean isEnabled;
    private final Pattern regexPattern;
    private final String regexReplacement;

    public UrlPathObfuscationConfigImpl(Map<String, Object> configSettings) {
        super(configSettings, SYSTEM_PROPERTY_ROOT);
        regexReplacement = getProperty(REGEX_REPLACEMENT, REGEX_REPLACEMENT_DEFAULT);

        // If the reg ex pattern is invalid, set it to null and force the enabled flag to false
        Pattern tmpRegExPattern = null;
        boolean isCaseInsensitive = getProperty(CASE_INSENSITIVE, CASE_INSENSITIVE_DEFAULT);
        String pattern = getProperty(REGEX_PATTERN);
        try {
            pattern = getProperty(REGEX_PATTERN);
            if (pattern != null &&  !pattern.trim().isEmpty()) {
                if (isCaseInsensitive) {
                    tmpRegExPattern = Pattern.compile(pattern,  Pattern.CASE_INSENSITIVE);
                } else {
                    tmpRegExPattern = Pattern.compile(pattern);
                }
            }
        } catch (PatternSyntaxException e) {
            Agent.LOG.log(Level.WARNING, "Failed to compile URL obfuscation regex pattern {0}; " +
                    "feature is disabled. Reason: {1}", pattern, e);
        }
        isEnabled = (tmpRegExPattern != null) && getProperty(ENABLED, ENABLED_DEFAULT);
        regexPattern = tmpRegExPattern;
    }


    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Pattern getRegExPattern() {
        return regexPattern;
    }

    @Override
    public String getRegExReplacement() {
        return regexReplacement;
    }
}
