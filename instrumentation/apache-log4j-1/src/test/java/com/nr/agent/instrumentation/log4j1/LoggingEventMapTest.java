/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.log4j1;

import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
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

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_CLASS;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.ERROR_STACK;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LEVEL;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LOGGER_FQCN;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.LOGGER_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.MESSAGE;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_ID;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.THREAD_NAME;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.TIMESTAMP;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


public class LoggingEventMapTest {

    private static Stream<Arguments> providerParamsForLoggingEventMapTest() {
        return Stream.of(
                argumentsOf("Minimal case", null, null, null, null, null, false, emptyMap(), 3),
                argumentsOf("Base case with all data MDC disabled", "com.newrelic.SomeClass", "SomeClasLogger",
                        Priority.ERROR, "Hello", new RuntimeException("SIMULATED"), false, singletonMap("tagKey", "tagValue"), 11),
                argumentsOf("Minimal case with MDC enabled", null, null, null, null, null, true, emptyMap(), 4),
                argumentsOf("Base case with all data and MDC enabled", "com.newrelic.OtherClass", "OtherClasLogger",
                        Priority.ERROR, "Hello", new RuntimeException("SIMULATED"), true, singletonMap("tagKey", "tagValue"), 12)
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
            Map<String, String> tags,
            int expectedEntryCount

    ) {
        return Arguments.of(testDetails, fqnOfLogger, loggerName, level, message, t, appLoggingContextDataEnabled, tags,
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
            Map<String, String> tags,
            int expectedEntryCount
    ) {
        // setup Category with test factory
        Category category = TestCategoryFactory.create(loggerName);

        // given a valid logging event
        LoggingEvent event = new LoggingEvent(fqnOfLogger, category, level, message, t);

        // and some application context data is available
        MDC.put("some", "value");

        // when logging event map created
        Map<LogAttributeKey, Object> loggingEventMap = LoggingEventMap.from(event, appLoggingContextDataEnabled, tags);

        // then it is not null or empty, and it has expected size
        assertNotNull(loggingEventMap);
        assertFalse(loggingEventMap.isEmpty());
        assertEquals(expectedEntryCount, loggingEventMap.size());

        // and logger information is set correctly
        assertEquals(loggerName, loggingEventMap.get(LOGGER_NAME));
        assertEquals(fqnOfLogger, loggingEventMap.get(LOGGER_FQCN));

        // and message and timestamp are set correctly
        assertEquals(message, loggingEventMap.get(MESSAGE));
        assertNotNull(loggingEventMap.get(TIMESTAMP));

        // and level is set correctly
        if (level != null) {
            assertEquals(level.toString(), loggingEventMap.get(LEVEL));
        } else {
            assertNull(loggingEventMap.get(LEVEL));
        }

        // and thread fields are not null
        assertEquals(event.getThreadName(), loggingEventMap.get(THREAD_NAME));
        assertNotNull(loggingEventMap.get(THREAD_ID));

        // and error information is set correctly
        if (t != null) {
            assertEquals(t.getMessage(), loggingEventMap.get(ERROR_MESSAGE));
            assertNotNull(loggingEventMap.get(ERROR_STACK));
            assertEquals(t.getClass().getCanonicalName(), loggingEventMap.get(ERROR_CLASS));
        } else {
            assertNull(loggingEventMap.get(ERROR_MESSAGE));
            assertNull(loggingEventMap.get(ERROR_STACK));
            assertNull(loggingEventMap.get(ERROR_CLASS));
        }

        // and MDC property should be added if appropriate
        if (appLoggingContextDataEnabled) {
            assertEquals("value", loggingEventMap.get(new LogAttributeKey("some", LogAttributeType.CONTEXT)));
        } else {
            assertNull(loggingEventMap.get(new LogAttributeKey("some", LogAttributeType.CONTEXT)));
        }

        // and tags should be added if present
        if (!tags.isEmpty()) {
            assertEquals("tagValue", loggingEventMap.get(new LogAttributeKey("tagKey", LogAttributeType.TAG)));
        } else {
            assertNull(loggingEventMap.get(new LogAttributeKey("tagKey", LogAttributeType.TAG)));
        }
    }
}
