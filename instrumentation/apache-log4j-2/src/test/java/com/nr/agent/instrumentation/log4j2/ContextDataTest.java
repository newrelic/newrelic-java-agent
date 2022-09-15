/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.logging.log4j.core" }, configName = "application_logging_context_data_enabled.yml")
public class ContextDataTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void reset() {
        Configurator.reconfigure();
        introspector.clearLogEvents();
    }

    @Test
    public void testAttributes() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        String attrKey1 = "key";
        String attrKey2 = "anotherKey";
        String val1 = "42";
        String val2 = "panic";

        ThreadContext.put(attrKey1, val1);
        ThreadContext.put(attrKey2, val2);
        logger.error("message");

        Collection<LogEvent> logEvents = introspector.getLogEvents();
        assertEquals(1, logEvents.size());

        LogEvent logEvent = logEvents.iterator().next();
        Map<String, Object> attributes = logEvent.getUserAttributesCopy();
        long contextAttrCount = attributes.keySet().stream()
                .filter(key -> key.startsWith("context."))
                .count();
        // MDC data is filtered later in the process
        assertEquals(2L, contextAttrCount);
        assertEquals(val1, attributes.get("context." + attrKey1));
        assertEquals(val2, attributes.get("context." + attrKey2));
    }

}
