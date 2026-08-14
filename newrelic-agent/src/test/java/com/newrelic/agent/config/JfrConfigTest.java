/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestEnvironmentFacade;
import static com.newrelic.agent.SaveSystemPropertyProviderRule.TestSystemProps;
import static com.newrelic.agent.config.JfrConfigImpl.*;
import static org.junit.Assert.*;

public class JfrConfigTest {

    @Before
    public void setup() {
        //clear the SystemPropertyProvider after each test.
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                new TestEnvironmentFacade()));
    }

    @Test
    public void jfrLabelsDisabledByDefault() {
        Map<String, Object> jfrSettings = new HashMap<>();
        JfrConfig config = new JfrConfigImpl(jfrSettings);
        assertFalse(config.labelsEnabled());
    }

    @Test
    public void jfrLabelsSettingsObeyLocalConfig() {
        /*
         * jfr:
         *   labels:
         *     enabled: true
         */
        Map<String, Object> jfrSettings = new HashMap<>();
        Map<String, Object> labelsSettings = new HashMap<>();
        labelsSettings.put(ENABLED, true);
        jfrSettings.put(LABELS, labelsSettings);
        JfrConfig config = new JfrConfigImpl(jfrSettings);
        assertTrue(config.labelsEnabled());

        /*
         * jfr:
         *   labels:
         *     enabled: false
         */
        labelsSettings.put(ENABLED, false);
        config = new JfrConfigImpl(jfrSettings);
        assertFalse(config.labelsEnabled());
    }

    @Test
    public void jfrLabelsSettingsObeySystemProperty() {
        //Enabled
        Properties props = new Properties();
        props.setProperty("newrelic.config.jfr.labels.enabled", "true");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(props),
                new TestEnvironmentFacade()
        ));
        Map<String, Object> jfrSettings = new HashMap<>();
        JfrConfig config = new JfrConfigImpl(jfrSettings);
        assertTrue(config.labelsEnabled());

        //Disabled
        props.setProperty("newrelic.config.jfr.labels.enabled", "false");
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(props),
                new TestEnvironmentFacade()
        ));
        config = new JfrConfigImpl(jfrSettings);
        assertFalse(config.labelsEnabled());
    }

    @Test
    public void jfrLabelsSettingsObeyEnvironmentVariable() {
        //Enabled
        TestEnvironmentFacade environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_JFR_LABELS_ENABLED", "true"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                environmentFacade
        ));
        Map<String, Object> jfrSettings = new HashMap<>();
        JfrConfig config = new JfrConfigImpl(jfrSettings);
        assertTrue(config.labelsEnabled());

        //Disabled
        environmentFacade = new TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_JFR_LABELS_ENABLED", "false"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(),
                environmentFacade
        ));
        config = new JfrConfigImpl(jfrSettings);
        assertFalse(config.labelsEnabled());
    }
}
