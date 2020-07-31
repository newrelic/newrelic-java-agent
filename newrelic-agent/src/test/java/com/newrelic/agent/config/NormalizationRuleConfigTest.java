/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class NormalizationRuleConfigTest {
    @Test
    public void extractsServerPropCorrectly() {
        Map<String, Object> properties = Collections.<String, Object>singletonMap(
                "url_rules",
                ServerProp.createPropObject(Collections.singletonList(
                        Collections.singletonMap("map key", "map value")
                ))
        );

        NormalizationRuleConfig target = new NormalizationRuleConfig(properties);
        assertEquals("map value", target.getUrlRules().get(0).get("map key"));
    }

    @Test
    public void handlesWithoutServerProp() {
        Map<String, Object> properties = Collections.<String, Object>singletonMap(
                "url_rules",
                Collections.singletonList(
                        Collections.singletonMap("map key", "map value")
                )
        );

        NormalizationRuleConfig target = new NormalizationRuleConfig(properties);
        assertEquals("map value", target.getUrlRules().get(0).get("map key"));
    }


    @Test
    public void gracefullyHandlesNullAsAString() {
        Map<String, Object> properties = Collections.<String, Object>singletonMap(
                "url_rules",
                "null"
        );

        NormalizationRuleConfig target = new NormalizationRuleConfig(properties);
        assertTrue(target.getUrlRules().isEmpty());
    }

    @Test
    public void gracefullyHandlesNull() {
        Map<String, Object> properties = Collections.singletonMap(
                "url_rules",
                null
        );

        NormalizationRuleConfig target = new NormalizationRuleConfig(properties);
        assertTrue(target.getUrlRules().isEmpty());
    }

    @Test
    public void gracefullyHandlesNoKey() {
        Map<String, Object> properties = Collections.emptyMap();

        NormalizationRuleConfig target = new NormalizationRuleConfig(properties);
        assertTrue(target.getUrlRules().isEmpty());
    }


    @Test
    public void gracefullyHandlesSomeOtherInvalidValue() {
        Map<String, Object> properties = Collections.<String, Object>singletonMap(
                "url_rules",
                8675309L
        );

        NormalizationRuleConfig target = new NormalizationRuleConfig(properties);
        assertTrue(target.getUrlRules().isEmpty());
    }
}