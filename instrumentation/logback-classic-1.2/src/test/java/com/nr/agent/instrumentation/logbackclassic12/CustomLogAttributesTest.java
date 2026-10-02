/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.logbackclassic12;

import ch.qos.logback.classic.Logger;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.model.LogEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "ch.qos.logback" }, configName = "application_logging_custom_attributes_enabled.yml")
public class CustomLogAttributesTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void reset() {
        introspector.clearLogEvents();
    }

    @Test
    public void testConfiguredCustomAttributesAreAddedToLogEvent() {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger_InstrumentationTest.class);
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
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger_InstrumentationTest.class);
        logger.error("message");

        Collection<LogEvent> logEvents = introspector.getLogEvents();
        assertEquals(1, logEvents.size());

        LogEvent logEvent = logEvents.iterator().next();
        Map<String, Object> attributes = logEvent.getUserAttributesCopy();
        // "instrumentation" is configured as a custom attribute in the yml, but it collides with a
        // standard agent attribute name, so the standard value should win.
        assertEquals("logback-classic-1.2", attributes.get("instrumentation"));
    }
}
