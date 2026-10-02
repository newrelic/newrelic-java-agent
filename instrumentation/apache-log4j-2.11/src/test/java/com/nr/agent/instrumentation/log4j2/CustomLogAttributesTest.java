/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.log4j2;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.model.LogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.logging.log4j.core" }, configName = "application_logging_custom_attributes_enabled.yml")
public class CustomLogAttributesTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void reset() {
        Configurator.reconfigure();
        introspector.clearLogEvents();
    }

    @Test
    public void testConfiguredCustomAttributesAreAddedToLogEvent() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        logger.error("message");

        Collection<LogEvent> logEvents = introspector.getLogEvents();
        assertEquals(1, logEvents.size());

        LogEvent logEvent = logEvents.iterator().next();
        Map<String, Object> attributes = logEvent.getUserAttributesCopy();
        assertEquals("prod", attributes.get("env"));
        assertEquals("us-east-1", attributes.get("region"));
    }

    @Test
    public void testConfiguredCustomAttributesDoNotOverrideStandardAttributes() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        logger.error("message");

        Collection<LogEvent> logEvents = introspector.getLogEvents();
        assertEquals(1, logEvents.size());

        LogEvent logEvent = logEvents.iterator().next();
        Map<String, Object> attributes = logEvent.getUserAttributesCopy();
        // "instrumentation" is configured as a custom attribute in the yml, but it collides with a
        // standard agent attribute name, so the standard value should win.
        assertEquals("apache-log4j-2.11", attributes.get("instrumentation"));
    }
}
