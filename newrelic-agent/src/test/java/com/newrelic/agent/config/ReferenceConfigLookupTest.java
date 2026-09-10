/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ReferenceConfigLookupTest {

    @Test
    public void mapIsPopulated() {
        Map<String, String> map = ReferenceConfigLookup.getEnvVarKeyToConfigKeyMap();
        assertNotNull(map);
        assertFalse("lookup map should not be empty", map.isEmpty());
    }

    @Test
    public void nestedDotKeyMappedFromUnderscore() {
        Map<String, String> map = ReferenceConfigLookup.getEnvVarKeyToConfigKeyMap();
        assertEquals("jfr.enabled", map.get("jfr_enabled"));
    }

    @Test
    public void multiLevelNestedKeyMappedCorrectly() {
        Map<String, String> map = ReferenceConfigLookup.getEnvVarKeyToConfigKeyMap();
        assertEquals("transaction_tracer.record_sql", map.get("transaction_tracer_record_sql"));
    }

    @Test
    public void keyWithLiteralUnderscoreMapsToItself() {
        Map<String, String> map = ReferenceConfigLookup.getEnvVarKeyToConfigKeyMap();
        assertEquals("log_file_name", map.get("log_file_name"));
    }

    @Test
    public void unknownKeyReturnsNull() {
        Map<String, String> map = ReferenceConfigLookup.getEnvVarKeyToConfigKeyMap();
        assertNull(map.get("totally_bogus_nonexistent_key"));
    }
}
