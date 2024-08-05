/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.log4j;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.test.marker.Java23IncompatibleTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@org.junit.experimental.categories.Category({ Java23IncompatibleTest.class })
@InstrumentationTestConfig(includePrefixes = {"org.apache.log4j"}, configName = "application_logging_context_data_enabled.yml")
public class ContextDataTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void reset() {
        introspector.clearLogEvents();
    }

    @Test
    public void testAttributes() {
        final Logger logger = LogManager.getLogger(ContextDataTest.class);
        String attrKey1 = "key";
        String attrKey2 = "anotherKey";
        String val1 = "42";
        String val2 = "panic";

        MDC.put(attrKey1, val1);
        MDC.put(attrKey2, val2);
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
