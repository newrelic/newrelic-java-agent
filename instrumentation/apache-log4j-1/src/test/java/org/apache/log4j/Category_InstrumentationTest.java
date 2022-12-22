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
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.model.LogEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"org.apache.log4j"}, configName = "application_logging_enabled.yml")
public class Category_InstrumentationTest {

    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    private static List<LogEvent> filterLoggingEventsAtLevel(Collection<LogEvent> logEvents, Level level) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(level.toString()))
                .collect(Collectors.toList());
    }

    @Before
    public void reset() {
        introspector.clearLogEvents();
    }

    @Test
    public void testLogEventsAllLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Log at ALL level
        logger.setLevel(Level.ALL);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.fatal(CAPTURED);

        int expectedTotalEventsCapturedAtError = 2;
        logger.error(CAPTURED);
        logger.error(CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 3;
        logger.warn(CAPTURED);
        logger.warn(CAPTURED);
        logger.warn(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 4;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 5;
        logger.debug(CAPTURED);
        logger.debug(CAPTURED);
        logger.debug(CAPTURED);
        logger.debug(CAPTURED);
        logger.debug(CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 6;
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtFatal +
                expectedTotalEventsCapturedAtError + expectedTotalEventsCapturedAtWarn +
                expectedTotalEventsCapturedAtInfo + expectedTotalEventsCapturedAtDebug +
                expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsOffLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Logging is OFF at all levels
        logger.setLevel(Level.OFF);

        int expectedTotalEventsCapturedAtFatal = 0;
        logger.fatal(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtError = 0;
        logger.error(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 0;
        logger.warn(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.debug(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.trace(NOT_CAPTURED);

        int expectedTotalEventsCaptured = 0;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsFatalLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Log at FATAL level
        logger.setLevel(Level.FATAL);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.fatal(CAPTURED);

        int expectedTotalEventsCapturedAtError = 0;
        logger.error(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 0;
        logger.warn(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.debug(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.trace(NOT_CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtFatal +
                expectedTotalEventsCapturedAtError + expectedTotalEventsCapturedAtWarn +
                expectedTotalEventsCapturedAtInfo + expectedTotalEventsCapturedAtDebug +
                expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsErrorLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Logging is ERROR at all levels
        logger.setLevel(Level.ERROR);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.fatal(CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.error(CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 0;
        logger.warn(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.debug(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.trace(NOT_CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtFatal +
                expectedTotalEventsCapturedAtError + expectedTotalEventsCapturedAtWarn +
                expectedTotalEventsCapturedAtInfo + expectedTotalEventsCapturedAtDebug +
                expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsWarnLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Logging is WARN
        logger.setLevel(Level.WARN);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.fatal(CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.error(CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.warn(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.debug(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.trace(NOT_CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtFatal +
                expectedTotalEventsCapturedAtError + expectedTotalEventsCapturedAtWarn +
                expectedTotalEventsCapturedAtInfo + expectedTotalEventsCapturedAtDebug +
                expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsInfoLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Logging is INFO
        logger.setLevel(Level.INFO);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.fatal(CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.error(CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.warn(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 1;
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.debug(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.trace(NOT_CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtFatal +
                expectedTotalEventsCapturedAtError + expectedTotalEventsCapturedAtWarn +
                expectedTotalEventsCapturedAtInfo + expectedTotalEventsCapturedAtDebug +
                expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsDebugLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Logging is DEBUG
        logger.setLevel(Level.DEBUG);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.fatal(CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.error(CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.warn(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 1;
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 1;
        logger.debug(CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.trace(NOT_CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtFatal +
                expectedTotalEventsCapturedAtError + expectedTotalEventsCapturedAtWarn +
                expectedTotalEventsCapturedAtInfo + expectedTotalEventsCapturedAtDebug +
                expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsTraceLevel() {
        final Logger logger = LogManager.getLogger(Category_InstrumentationTest.class);
        // Logging is TRACE
        logger.setLevel(Level.TRACE);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.fatal(CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.error(CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.warn(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 1;
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 1;
        logger.debug(CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 1;
        logger.trace(CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtFatal +
                expectedTotalEventsCapturedAtError + expectedTotalEventsCapturedAtWarn +
                expectedTotalEventsCapturedAtInfo + expectedTotalEventsCapturedAtDebug +
                expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> fatalLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.FATAL);
        List<LogEvent> errorLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.ERROR);
        List<LogEvent> warnLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.WARN);
        List<LogEvent> infoLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.INFO);
        List<LogEvent> debugLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.DEBUG);
        List<LogEvent> traceLevelLogEvents = filterLoggingEventsAtLevel(logEvents, Level.TRACE);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void shouldIncrementEmittedLogsCountersIndependentlyIfLogLevelEnabled() {
        // Given
        final Logger logger = Logger.getLogger(Category_InstrumentationTest.class);
        logger.setLevel(Level.INFO);

        // When
        logger.trace(NOT_CAPTURED);
        logger.debug(NOT_CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.warn(CAPTURED);
        logger.warn(CAPTURED);
        logger.warn(CAPTURED);
        logger.warn(CAPTURED);
        logger.error(CAPTURED);

        // Then
        assertEquals(8, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
        assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
        assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
        assertEquals(3, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
    }

    @Test
    public void shouldIncrementAllEmittedLogCountersIfLogLevelIsSetToTrace() {
        // Given
        final Logger logger = Logger.getLogger(Category_InstrumentationTest.class);
        logger.setLevel(Level.TRACE);

        // When
        logger.trace(CAPTURED);
        logger.debug(CAPTURED);
        logger.info(CAPTURED);
        logger.warn(CAPTURED);
        logger.error(CAPTURED);

        // Then
        assertEquals(5, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
    }

}
