package com.nr.agent.instrumentation.log4j2;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.model.LogEvent;
import junit.framework.TestCase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.logging.log4j.core" }, configName = "application_logging_enabled.yml")
public class LoggerConfig_InstrumentationTest extends TestCase {

    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void reset() {
        Configurator.reconfigure();
        introspector.clearLogEvents();
    }

    @Test
    public void testLogEventsAllLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Log at ALL level
        setLoggerLevel(Level.ALL);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsOffLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Logging is OFF at all levels
        setLoggerLevel(Level.OFF);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsFatalLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Log at FATAL level
        setLoggerLevel(Level.FATAL);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsErrorLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Log at ERROR level
        setLoggerLevel(Level.ERROR);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsWarnLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Log at WARN level
        setLoggerLevel(Level.WARN);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsInfoLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Log at INFO level
        setLoggerLevel(Level.INFO);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsDebugLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Log at DEBUG level
        setLoggerLevel(Level.DEBUG);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    @Test
    public void testLogEventsTraceLevel() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        // Log at TRACE level
        setLoggerLevel(Level.TRACE);

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

        List<LogEvent> fatalLevelLogEvents = getFatalLevelLogEvents(logEvents);
        List<LogEvent> errorLevelLogEvents = getErrorLevelLogEvents(logEvents);
        List<LogEvent> warnLevelLogEvents = getWarnLevelLogEvents(logEvents);
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logEvents);
        List<LogEvent> debugLevelLogEvents = getDebugLevelLogEvents(logEvents);
        List<LogEvent> traceLevelLogEvents = getTraceLevelLogEvents(logEvents);

        assertEquals(expectedTotalEventsCapturedAtFatal, fatalLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtError, errorLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarn, warnLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtDebug, debugLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtTrace, traceLevelLogEvents.size());
    }

    private List<LogEvent> getTraceLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.TRACE.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getDebugLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.DEBUG.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getInfoLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.INFO.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getWarnLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.WARN.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getErrorLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.ERROR.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getFatalLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.FATAL.toString()))
                .collect(Collectors.toList());
    }

    @Test
    public void shouldIncrementEmittedLogsCountersIndependentlyIfLogLevelEnabled() {
        // Given
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        setLoggerLevel(Level.INFO);

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
        Assert.assertEquals(8, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
        Assert.assertEquals(4, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
    }

    @Test
    public void shouldIncrementAllEmittedLogCountersIfLogLevelIsSetToTrace() {
        // Given
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        setLoggerLevel(Level.TRACE);

        // When
        logger.trace(CAPTURED);
        logger.debug(CAPTURED);
        logger.info(CAPTURED);
        logger.warn(CAPTURED);
        logger.error(CAPTURED);

        // Then
        Assert.assertEquals(5, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
    }

    @Test
    public void shouldIncrementAllEmittedLogCountersOnlyOnceWhenMultipleLoggersAreSet() {
        // Given
        createLogger("A_SPECIAL_LOGGER", createAppender("ConsoleAppender"), Level.TRACE, true);
        final Logger logger = LogManager.getLogger("A_SPECIAL_LOGGER");
        setLoggerLevel(Level.TRACE);

        // When
        logger.trace(CAPTURED);
        logger.debug(CAPTURED);
        logger.info(CAPTURED);
        logger.warn(CAPTURED);
        logger.error(CAPTURED);

        // Then
        Assert.assertEquals(5, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
    }

    @Test
    public void shouldIncrementAllEmittedLogCountersRespectingLevelFromOriginalLogger() {
        // Given
        createLogger("A_SPECIAL_LOGGER", createAppender("ConsoleAppender"), Level.INFO, true);
        final Logger logger = LogManager.getLogger("A_SPECIAL_LOGGER");
        setLoggerLevel(Level.ERROR);

        // When
        logger.trace(NOT_CAPTURED);
        logger.debug(NOT_CAPTURED);
        logger.info(CAPTURED);
        logger.warn(CAPTURED);
        logger.error(CAPTURED);

        // Then
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
    }

    private void createLogger(String name, Appender appender, Level level, boolean additivity) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
        AppenderRef[] refs = new AppenderRef[] { ref };
        LoggerConfig loggerConfig = LoggerConfig.createLogger(additivity, level, name, "true", refs, null, config, null);
        loggerConfig.addAppender(appender, level, null);
        config.addLogger(name, loggerConfig);
    }

    private Appender createAppender(String name) {
        Layout<String> layout = PatternLayout.newBuilder()
                .withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .build();
        Appender appender = ConsoleAppender.newBuilder()
                .setName(name)
                .setLayout(layout)
                .build();
        appender.start();
        return appender;
    }

    private void setLoggerLevel(Level level) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = context.getConfiguration();
        final LoggerConfig rootConfig = configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        rootConfig.setLevel(level);
        context.updateLoggers();
    }

    @Test
    public void contextDataDisabledTest() {
        final Logger logger = LogManager.getLogger(LoggerConfig_InstrumentationTest.class);
        ThreadContext.put("include", "42");
        ThreadContext.put("exclude", "panic");

        logger.error("message");

        Collection<LogEvent> logEvents = introspector.getLogEvents();
        assertEquals(1, logEvents.size());

        // verify no context attrs
        Map<String, Object> attributes = logEvents.iterator().next().getUserAttributesCopy();
        long contextAttrCount = attributes.keySet().stream()
                .filter(key -> key.startsWith("context."))
                .count();
        assertEquals(0L, contextAttrCount);
    }

}
