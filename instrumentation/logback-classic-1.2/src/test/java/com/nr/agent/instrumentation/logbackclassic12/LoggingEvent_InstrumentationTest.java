package com.nr.agent.instrumentation.logbackclassic12;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.model.LogEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "ch.qos.logback" }, configName = "application_logging_enabled.yml")
public class LoggingEvent_InstrumentationTest {
    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void reset() {
        introspector.clearLogEvents();
    }

    @Test
    public void testLogEventsAllLevel() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingEvent_InstrumentationTest.class);
        // Log at ALL level
        logger.setLevel(Level.ALL);

        int expectedTotalEventsCapturedAtError = 1;
        logger.error(CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 2;
        logger.warn(CAPTURED);
        logger.warn(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 3;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 4;
        logger.debug(CAPTURED);
        logger.debug(CAPTURED);
        logger.debug(CAPTURED);
        logger.debug(CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 5;
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);
        logger.trace(CAPTURED);

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtError +
                expectedTotalEventsCapturedAtWarn + expectedTotalEventsCapturedAtInfo +
                expectedTotalEventsCapturedAtDebug + expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsOffLevel() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingEvent_InstrumentationTest.class);
        // Logging is OFF at all levels
        logger.setLevel(Level.OFF);

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

        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsErrorLevel() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingEvent_InstrumentationTest.class);
        // Log at ERROR level
        logger.setLevel(Level.ERROR);

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

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtError +
                expectedTotalEventsCapturedAtWarn + expectedTotalEventsCapturedAtInfo +
                expectedTotalEventsCapturedAtDebug + expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsWarnLevel() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingEvent_InstrumentationTest.class);
        // Log at WARN level
        logger.setLevel(Level.WARN);

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

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtError +
                expectedTotalEventsCapturedAtWarn + expectedTotalEventsCapturedAtInfo +
                expectedTotalEventsCapturedAtDebug + expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsInfoLevel() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingEvent_InstrumentationTest.class);
        // Log at INFO level
        logger.setLevel(Level.INFO);

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

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtError +
                expectedTotalEventsCapturedAtWarn + expectedTotalEventsCapturedAtInfo +
                expectedTotalEventsCapturedAtDebug + expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsDebugLevel() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingEvent_InstrumentationTest.class);
        // Log at DEBUG level
        logger.setLevel(Level.DEBUG);

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

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtError +
                expectedTotalEventsCapturedAtWarn + expectedTotalEventsCapturedAtInfo +
                expectedTotalEventsCapturedAtDebug + expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsTraceLevel() {
        final Logger logger = (Logger) LoggerFactory.getLogger(LoggingEvent_InstrumentationTest.class);
        // Log at TRACE level
        logger.setLevel(Level.TRACE);

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

        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtError +
                expectedTotalEventsCapturedAtWarn + expectedTotalEventsCapturedAtInfo +
                expectedTotalEventsCapturedAtDebug + expectedTotalEventsCapturedAtTrace;

        Collection<LogEvent> logEvents = introspector.getLogEvents();

        assertEquals(expectedTotalEventsCaptured, logEvents.size());

        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    private List<LogEvent> getTraceLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.TRACE))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getDebugLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.DEBUG))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getInfoLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.INFO))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getWarnLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.WARN))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getErrorLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.ERROR))
                .collect(Collectors.toList());
    }

    @Test
    public void contextDataDisabledTest() {

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger_InstrumentationTest.class);
        MDC.put("include", "42");
        MDC.put("common", "life, the universe and everything");
        MDC.put("exclude", "panic");
        logger.error("message");

        Collection<LogEvent> logEvents = introspector.getLogEvents();
        assertEquals(1, logEvents.size());

        // verify no context attrs
        LogEvent logEvent = logEvents.iterator().next();
        long contextAttrCount = logEvent.getUserAttributesCopy().keySet().stream()
                .filter(key -> key.startsWith("context."))
                .count();
        assertEquals(0, contextAttrCount);
    }
}
