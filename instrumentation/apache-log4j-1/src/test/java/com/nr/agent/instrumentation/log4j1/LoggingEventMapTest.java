/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.log4j1;

import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import com.newrelic.test.marker.Java21IncompatibleTest;
import com.newrelic.test.marker.Java22IncompatibleTest;
import org.apache.log4j.Category;
import org.apache.log4j.MDC;
import org.apache.log4j.Priority;
import org.apache.log4j.TestCategoryFactory;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// Log4J1 has a quirk with Java 21(.0.0), maybe this will work when 21.0.1 is released
@org.junit.experimental.categories.Category({ Java21IncompatibleTest.class, Java22IncompatibleTest.class })
public class LoggingEventMapTest {

    private static Stream<Arguments> providerParamsForLoggingEventMapTest() {
        return Stream.of(
                argumentsOf("Minimal case", null, null, null, null, null, false, 4),
                argumentsOf("Base case with all data MDC disabled", "com.newrelic.SomeClass", "SomeClasLogger",
                        Priority.ERROR, "Hello", new RuntimeException("SIMULATED"), false, 11),
                argumentsOf("Minimal case with MDC enabled", null, null, null, null, null, true, 5),
                argumentsOf("Base case with all data and MDC enabled", "com.newrelic.OtherClass", "OtherClasLogger",
                        Priority.ERROR, "Hello", new RuntimeException("SIMULATED"), true, 12)
        );
    }

    private static Arguments argumentsOf(
            String testDetails,
            String fqnOfLogger,
            String loggerName,
            Priority level,
            String message,
            Throwable t,
            boolean appLoggingContextDataEnabled,
            int expectedEntryCount

    ) {
        return Arguments.of(testDetails, fqnOfLogger, loggerName, level, message, t, appLoggingContextDataEnabled,
                expectedEntryCount);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providerParamsForLoggingEventMapTest")
    void testLoggingEventCreation(
            String testDetails,
            String fqnOfLogger,
            String loggerName,
            Priority level,
            String message,
            Throwable t,
            boolean appLoggingContextDataEnabled,
            int expectedEntryCount
    ) {
        // setup Category with test factory
        Category category = TestCategoryFactory.create(loggerName);

        // given a valid logging event
        LoggingEvent event = new LoggingEvent(fqnOfLogger, category, level, message, t);

        // and some application context data is available
        MDC.put("some", "value");

        // when logging event map created
        Map<LogAttributeKey, Object> loggingEventMap = LoggingEventMap.from(event, appLoggingContextDataEnabled);

        // then it is not null or empty, and it has expected size
        assertNotNull(loggingEventMap);
        assertFalse(loggingEventMap.isEmpty());
        assertEquals(expectedEntryCount, loggingEventMap.size());

        // and logger information is set correctly
        assertEquals(loggerName, loggingEventMap.get(AppLoggingUtils.LOGGER_NAME));
        assertEquals(fqnOfLogger, loggingEventMap.get(AppLoggingUtils.LOGGER_FQCN));

        // and message and timestamp are set correctly
        assertEquals(message, loggingEventMap.get(AppLoggingUtils.MESSAGE));
        assertNotNull(loggingEventMap.get(AppLoggingUtils.TIMESTAMP));

        // and level is set correctly
        if (level != null) {
            assertEquals(level.toString(), loggingEventMap.get(AppLoggingUtils.LEVEL));
        } else {
            assertNull(loggingEventMap.get(AppLoggingUtils.LEVEL));
        }

        // and thread fields are not null
        assertEquals(event.getThreadName(), loggingEventMap.get(AppLoggingUtils.THREAD_NAME));
        assertNotNull(loggingEventMap.get(AppLoggingUtils.THREAD_ID));

        // and error information is set correctly
        if (t != null) {
            assertEquals(t.getMessage(), loggingEventMap.get(AppLoggingUtils.ERROR_MESSAGE));
            assertNotNull(loggingEventMap.get(AppLoggingUtils.ERROR_STACK));
            assertEquals(t.getClass().getCanonicalName(), loggingEventMap.get(AppLoggingUtils.ERROR_CLASS));
        } else {
            assertNull(loggingEventMap.get(AppLoggingUtils.ERROR_MESSAGE));
            assertNull(loggingEventMap.get(AppLoggingUtils.ERROR_STACK));
            assertNull(loggingEventMap.get(AppLoggingUtils.ERROR_CLASS));
        }

        // and MDC property should be added if appropriate
        if (appLoggingContextDataEnabled) {
            assertEquals("value", loggingEventMap.get(new LogAttributeKey("some", LogAttributeType.CONTEXT)));
        } else {
            assertNull(loggingEventMap.get(new LogAttributeKey("some", LogAttributeType.CONTEXT)));
        }
    }
}
