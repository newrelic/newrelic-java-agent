/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import java.util.regex.Pattern;

public interface UrlPathObfuscationConfig {

    /**
     * If <code>true</code> URL obfuscation is enabled
     */
    boolean isEnabled();

    /**
     * Specifies the regex pattern to use for url obfuscation. If this is not set (even if enabled is true),
     * no url obfuscation will be performed. If the supplied pattern  invalid, a message will be logged
     * at WARNING level and the feature will be disabled. If the value is blank, the feature will also
     * be disabled.
     *
     * @return the compiled regular expression instance or null if not set or invalid
     */
    Pattern getRegExPattern();

    /**
     * Specifies the replacement string to use for URLs matched by the <code>getRegExPattern</code>
     * method. If this value is blank, matched URLs will be replaced by an empty String.
     *
     * @return the configured replacement String
     */
    String getRegExReplacement();
}
