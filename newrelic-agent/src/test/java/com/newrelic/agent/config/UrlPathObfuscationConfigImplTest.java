/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UrlPathObfuscationConfigImplTest {

    private Map<String, Object> localProps;

    @Rule
    public SaveSystemPropertyProviderRule saveSystemPropertyProviderRule = new SaveSystemPropertyProviderRule();

    @Before
    public void setup() {
        localProps = new HashMap<>();
    }

    @Test
    public void testDefaults_noConfigPresent() {
        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertFalse(config.isEnabled());
        assertNull(config.getRegExPattern());
        assertEquals("", config.getRegExReplacement());
    }

    @Test
    public void testDefaults_nullConfigSettings() {
        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(null);
        assertFalse(config.isEnabled());
        assertNull(config.getRegExPattern());
        assertEquals("", config.getRegExReplacement());
    }

    @Test
    public void testEnabled_validPattern() {
        localProps.put(UrlPathObfuscationConfigImpl.ENABLED, true);
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("/account/[0-9]+", null, null));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertTrue(config.isEnabled());
        assertNotNull(config.getRegExPattern());
        assertEquals("/account/[0-9]+", config.getRegExPattern().pattern());
    }

    @Test
    public void testDisabled_enabledFlagIsFalse() {
        localProps.put(UrlPathObfuscationConfigImpl.ENABLED, false);
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("/account/[0-9]+", null, null));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertFalse(config.isEnabled());
        // the pattern itself still compiles even though the disabled is false
        assertNotNull(config.getRegExPattern());
    }

    @Test
    public void testDisabled_enabledTrueButRegexStanzaMissing() {
        localProps.put(UrlPathObfuscationConfigImpl.ENABLED, true);

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertFalse(config.isEnabled());
        assertNull(config.getRegExPattern());
    }

    @Test
    public void testDisabled_patternIsBlank() {
        localProps.put(UrlPathObfuscationConfigImpl.ENABLED, true);
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("   ", null, null));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertFalse(config.isEnabled());
        assertNull(config.getRegExPattern());
    }

    @Test
    public void testInvalidPattern_setsEnabledFlagFalse() {
        localProps.put(UrlPathObfuscationConfigImpl.ENABLED, true);
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("[invalid(", null, null));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertFalse(config.isEnabled());
        assertNull(config.getRegExPattern());
    }

    @Test
    public void testDefaultReplacement_isEmptyString() {
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("/account/[0-9]+", null, null));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertEquals("", config.getRegExReplacement());
    }

    @Test
    public void testCustomReplacementString() {
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("/account/[0-9]+", "REDACTED", null));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertEquals("REDACTED", config.getRegExReplacement());
    }

    @Test
    public void testCaseSensitive_disabledByDefault() {
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("ABC", null, null));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertFalse(config.getRegExPattern().matcher("abc").find());
    }

    @Test
    public void testIgnoreCaseWhenConfigured() {
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("ABC", null, true));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertTrue(config.getRegExPattern().matcher("abc").find());
    }

    @Test
    public void configureEnabled_viaSystemProperty() {
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("/account/[0-9]+", null, null));

        Properties properties = new Properties();
        properties.put("newrelic.config.url_obfuscation.enabled", "true");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertTrue(config.isEnabled());
    }

    @Test
    public void configureRegexPattern_viaSystemProperty() {
        Properties properties = new Properties();
        properties.put("newrelic.config.url_obfuscation.regex.pattern", "/account/[0-9]+");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(Collections.emptyMap());
        assertNotNull(config.getRegExPattern());
        assertEquals("/account/[0-9]+", config.getRegExPattern().pattern());
    }

    @Test
    public void configureRegexReplacement_viaSystemProperty() {
        Properties properties = new Properties();
        properties.put("newrelic.config.url_obfuscation.regex.replacement", "REDACTED");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(Collections.emptyMap());
        assertEquals("REDACTED", config.getRegExReplacement());
    }

    @Test
    public void configureIgnoreCase_viaSystemProperty() {
        localProps.put(UrlPathObfuscationConfigImpl.REGEX, regexSettings("ABC", null, null));

        Properties properties = new Properties();
        properties.put("newrelic.config.url_obfuscation.regex.ignore_case", "true");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(properties),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        UrlPathObfuscationConfigImpl config = new UrlPathObfuscationConfigImpl(localProps);
        assertTrue(config.getRegExPattern().matcher("abc").find());
    }

    private static Map<String, Object> regexSettings(String pattern, String replacement, Boolean ignoreCase) {
        Map<String, Object> regex = new HashMap<>();
        if (pattern != null) {
            regex.put("pattern", pattern);
        }
        if (replacement != null) {
            regex.put("replacement", replacement);
        }
        if (ignoreCase != null) {
            regex.put("ignore_case", ignoreCase);
        }
        return regex;
    }
}
