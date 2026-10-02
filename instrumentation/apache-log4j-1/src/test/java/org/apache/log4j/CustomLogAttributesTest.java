/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.log4j;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.test.marker.Java25IncompatibleTest;
import com.newrelic.test.marker.Java26IncompatibleTest;
import org.junit.experimental.categories.Category;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@Category({ Java25IncompatibleTest.class, Java26IncompatibleTest.class })
@InstrumentationTestConfig(includePrefixes = {"org.apache.log4j"}, configName = "application_logging_custom_attributes_enabled.yml")
public class CustomLogAttributesTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void reset() {
        introspector.clearLogEvents();
    }

    @Test
    public void testConfiguredCustomAttributesAreAddedToLogEvent() {
        final Logger logger = LogManager.getLogger(CustomLogAttributesTest.class);
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
        final Logger logger = LogManager.getLogger(CustomLogAttributesTest.class);
        logger.error("message");

        Collection<LogEvent> logEvents = introspector.getLogEvents();
        assertEquals(1, logEvents.size());

        LogEvent logEvent = logEvents.iterator().next();
        Map<String, Object> attributes = logEvent.getUserAttributesCopy();
        // "instrumentation" is configured as a custom attribute in the yml, but it collides with a
        // standard agent attribute name, so the standard value should win.
        assertEquals("apache-log4j-1", attributes.get("instrumentation"));
    }
}
