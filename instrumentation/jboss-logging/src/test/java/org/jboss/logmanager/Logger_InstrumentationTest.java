package org.jboss.logmanager;

import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.model.LogEvent;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.jboss.logmanager" }, configName = "application_logging_enabled.yml")
public class Logger_InstrumentationTest extends TestCase {

    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @BeforeClass
    public static void init() {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @Before
    public void reset() {
//        Configurator.reconfigure();
//        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        introspector.clearLogEvents();
    }

//    public static final Level FATAL = new Level("FATAL", 1100);
//    public static final Level ERROR = new Level("ERROR", 1000);
//    public static final Level WARN = new Level("WARN", 900);
//    public static final Level INFO = new Level("INFO", 800);
//    public static final Level DEBUG = new Level("DEBUG", 500);
//    public static final Level TRACE = new Level("TRACE", 400);

    @Test
    public void testLogEventsFatalLevel() {
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        // Log at FATAL level
        logger.setLevel(Level.FATAL);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.log(Level.FATAL, CAPTURED);

        int expectedTotalEventsCapturedAtError = 0;
        logger.log(Level.ERROR, NOT_CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 0;
        logger.log(Level.WARN, NOT_CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.log(Level.DEBUG, NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.log(Level.TRACE, NOT_CAPTURED);

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
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        // Log at ERROR level
        logger.setLevel(Level.ERROR);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.log(Level.FATAL, CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.log(Level.ERROR, CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 0;
        logger.log(Level.WARN, NOT_CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.log(Level.DEBUG, NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.log(Level.TRACE, NOT_CAPTURED);

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
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        // Log at WARN level
        logger.setLevel(Level.WARN);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.log(Level.FATAL, CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.log(Level.ERROR, CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.log(Level.WARN, CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.log(Level.DEBUG, NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.log(Level.TRACE, NOT_CAPTURED);

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
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        // Log at INFO level
        logger.setLevel(Level.INFO);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.log(Level.FATAL, CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.log(Level.ERROR, CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.log(Level.WARN, CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 1;
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 0;
        logger.log(Level.DEBUG, NOT_CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.log(Level.TRACE, NOT_CAPTURED);

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
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        // Log at DEBUG level
        logger.setLevel(Level.DEBUG);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.log(Level.FATAL, CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.log(Level.ERROR, CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.log(Level.WARN, CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 1;
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 1;
        logger.log(Level.DEBUG, CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.log(Level.TRACE, NOT_CAPTURED);

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
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        // Log at TRACE level
        logger.setLevel(Level.TRACE);

        int expectedTotalEventsCapturedAtFatal = 1;
        logger.log(Level.FATAL, CAPTURED);

        int expectedTotalEventsCapturedAtError = 1;
        logger.log(Level.ERROR, CAPTURED);

        int expectedTotalEventsCapturedAtWarn = 1;
        logger.log(Level.WARN, CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 1;
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtDebug = 1;
        logger.log(Level.DEBUG, CAPTURED);

        int expectedTotalEventsCapturedAtTrace = 0;
        logger.log(Level.TRACE, CAPTURED);

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
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        logger.setLevel(Level.INFO);

        // When
        logger.log(Level.TRACE, NOT_CAPTURED);
        logger.log(Level.DEBUG, NOT_CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.log(Level.WARN, CAPTURED);
        logger.log(Level.WARN, CAPTURED);
        logger.log(Level.WARN, CAPTURED);
        logger.log(Level.WARN, CAPTURED);
        logger.log(Level.ERROR, CAPTURED);

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
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        logger.setLevel(Level.TRACE);

        // When
        logger.log(Level.TRACE, CAPTURED);
        logger.log(Level.DEBUG, CAPTURED);
        logger.info(CAPTURED);
        logger.log(Level.WARN, CAPTURED);
        logger.log(Level.ERROR, CAPTURED);

        // Then
        Assert.assertEquals(5, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
    }

//    @Test
//    public void shouldIncrementAllEmittedLogCountersOnlyOnceWhenMultipleLoggersAreSet() {
//        // Given
//        createLogger("A_SPECIAL_LOGGER", createAppender("ConsoleAppender"), Level.TRACE, true);
//        final Logger logger = LogManager.getLogger("A_SPECIAL_LOGGER");
//        logger.setLevel(Level.TRACE);
//
//        // When
//        logger.log(Level.TRACE, CAPTURED);
//        logger.log(Level.DEBUG, CAPTURED);
//        logger.info(CAPTURED);
//        logger.log(Level.WARN, CAPTURED);
//        logger.log(Level.ERROR, CAPTURED);
//
//        // Then
//        Assert.assertEquals(5, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
//    }
//
//    @Test
//    public void shouldIncrementAllEmittedLogCountersRespectingLevelFromOriginalLogger() {
//        // Given
//        createLogger("A_SPECIAL_LOGGER", createAppender("ConsoleAppender"), Level.INFO, true);
//        final Logger logger = LogManager.getLogger("A_SPECIAL_LOGGER");
//        logger.setLevel(Level.ERROR);
//
//        // When
//        logger.log(Level.TRACE, NOT_CAPTURED);
//        logger.log(Level.DEBUG, NOT_CAPTURED);
//        logger.info(CAPTURED);
//        logger.log(Level.WARN, CAPTURED);
//        logger.log(Level.ERROR, CAPTURED);
//
//        // Then
//        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("Logging/lines"));
//        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/TRACE"));
//        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("Logging/lines/DEBUG"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/INFO"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/WARN"));
//        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("Logging/lines/ERROR"));
//    }
//
//    private void createLogger(String name, Appender appender, Level level, boolean additivity) {
//        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
//        final Configuration config = ctx.getConfiguration();
//        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
//        AppenderRef[] refs = new AppenderRef[] { ref };
//        LoggerConfig loggerConfig = LoggerConfig.createLogger(additivity, level, name, "true", refs, null, config, null);
//        loggerConfig.addAppender(appender, level, null);
//        config.addLogger(name, loggerConfig);
//    }
//
//    private Appender createAppender(String name) {
//        Layout<String> layout = PatternLayout.newBuilder()
//                .withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
//                .build();
//        Appender appender = ConsoleAppender.newBuilder()
//                .setName(name)
//                .setLayout(layout)
//                .build();
//        appender.start();
//        return appender;
//    }

    @Test
    public void contextDataDisabledTest() {
        final Logger logger = Logger.getLogger(Logger_InstrumentationTest.class.getName());
        MDC.put("include", "42");
        MDC.put("exclude", "panic");

        logger.log(Level.ERROR, "message");

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
