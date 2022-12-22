/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.jboss;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.model.LogEvent;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.Logger_InstrumentationTest;
import org.jboss.logmanager.MDC;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.jboss.logmanager" }, configName = "application_logging_context_data_enabled.yml")
public class ContextDataTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    static {
        // must set before the Logger
        // loads logging.properties from the classpath
        String path = ContextDataTest.class
                .getClassLoader().getResource("logging.properties").getFile();
        System.setProperty("java.util.logging.config.file", path);
    }

//    @BeforeClass
//    public static void init() {
//        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
//    }

    @Before
    public void reset() {
//        Configurator.reconfigure();
        introspector.clearLogEvents();
    }

    @Test
    public void testAttributes() {
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        String attrKey1 = "key";
        String attrKey2 = "anotherKey";
        String val1 = "42";
        String val2 = "panic";

        MDC.put(attrKey1, val1);
        MDC.put(attrKey2, val2);
        logger.log(Level.ERROR, "message");

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
