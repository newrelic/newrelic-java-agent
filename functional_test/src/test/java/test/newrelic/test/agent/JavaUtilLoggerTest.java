package test.newrelic.test.agent;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.logging.LogSenderServiceImpl;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Test for com.newrelic.instrumentation.java.logging-jdk8 instrumentation
 */
public class JavaUtilLoggerTest {
    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";
    private final String applicationName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
    private final LogSenderServiceImpl logSenderService = (LogSenderServiceImpl) ServiceFactory.getServiceManager().getLogSenderService();
    private final DistributedSamplingPriorityQueue<LogEvent> logReservoir = logSenderService.getReservoir(applicationName);

    @Before
    public void setup() {
        Transaction.clearTransaction();
        // Clear LogEvent reservoir between tests
        logSenderService.clearReservoir(applicationName);
    }

    @Test
    public void testLogEventsAllLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.ALL);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 2;
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 3;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 4;
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);

        int expectedTotalEventsCapturedAtFine = 5;
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 6;
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 7;
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsOffLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.OFF);

        // When
        int expectedTotalEventsCapturedAtSevere = 0;
        logger.severe(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 0;
        logger.warning(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 0;
        logger.config(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFine = 0;
        logger.fine(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 0;
        logger.finer(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 0;
        logger.finest(NOT_CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsSevereLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.SEVERE);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 0;
        logger.warning(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 0;
        logger.config(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFine = 0;
        logger.fine(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 0;
        logger.finer(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 0;
        logger.finest(NOT_CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsWarningLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.WARNING);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 2;
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 0;
        logger.info(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 0;
        logger.config(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFine = 0;
        logger.fine(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 0;
        logger.finer(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 0;
        logger.finest(NOT_CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsInfoLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.INFO);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 2;
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 3;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 0;
        logger.config(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFine = 0;
        logger.fine(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 0;
        logger.finer(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 0;
        logger.finest(NOT_CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsConfigLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.CONFIG);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 2;
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 3;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 4;
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);

        int expectedTotalEventsCapturedAtFine = 0;
        logger.fine(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 0;
        logger.finer(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 0;
        logger.finest(NOT_CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsFineLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.FINE);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 2;
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 3;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 4;
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);

        int expectedTotalEventsCapturedAtFine = 5;
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 0;
        logger.finer(NOT_CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 0;
        logger.finest(NOT_CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsFinerLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.FINER);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 2;
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 3;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 4;
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);

        int expectedTotalEventsCapturedAtFine = 5;
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 6;
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 0;
        logger.finest(NOT_CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Test
    public void testLogEventsFinestLevel() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.FINEST);

        // When
        int expectedTotalEventsCapturedAtSevere = 1;
        logger.severe(CAPTURED);

        int expectedTotalEventsCapturedAtWarning = 2;
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);

        int expectedTotalEventsCapturedAtInfo = 3;
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);

        int expectedTotalEventsCapturedAtConfig = 4;
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);
        logger.config(CAPTURED);

        int expectedTotalEventsCapturedAtFine = 5;
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);
        logger.fine(CAPTURED);

        int expectedTotalEventsCapturedAtFiner = 6;
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);
        logger.finer(CAPTURED);

        int expectedTotalEventsCapturedAtFinest = 7;
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);
        logger.finest(CAPTURED);

        // Then
        doAssertions(expectedTotalEventsCapturedAtSevere, expectedTotalEventsCapturedAtWarning, expectedTotalEventsCapturedAtInfo,
                expectedTotalEventsCapturedAtConfig, expectedTotalEventsCapturedAtFine, expectedTotalEventsCapturedAtFiner,
                expectedTotalEventsCapturedAtFinest);
    }

    @Trace(dispatcher = true)
    @Test
    public void shouldIncrementEmittedLogsCountersIndependentlyIfLogLevelEnabled() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.INFO);

        // When
        logger.finest(NOT_CAPTURED);
        logger.finer(NOT_CAPTURED);
        logger.fine(NOT_CAPTURED);
        logger.config(NOT_CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.info(CAPTURED);
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);
        logger.warning(CAPTURED);
        logger.severe(CAPTURED);

        // Then
        Map<String, Integer> metrics = getLogMetricsCounts();
        assertEquals(8, (int) metrics.get("Logging/lines"));
        assertEquals(0, (int) metrics.get("Logging/lines/FINEST"));
        assertEquals(0, (int) metrics.get("Logging/lines/FINER"));
        assertEquals(0, (int) metrics.get("Logging/lines/FINE"));
        assertEquals(0, (int) metrics.get("Logging/lines/CONFIG"));
        assertEquals(3, (int) metrics.get("Logging/lines/INFO"));
        assertEquals(4, (int) metrics.get("Logging/lines/WARNING"));
        assertEquals(1, (int) metrics.get("Logging/lines/SEVERE"));
    }

    @Trace(dispatcher = true)
    @Test
    public void shouldIncrementAllEmittedLogCountersIfLogLevelIsSetToFinest() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.FINEST);

        // When
        logger.finest(CAPTURED);
        logger.finer(CAPTURED);
        logger.fine(CAPTURED);
        logger.config(CAPTURED);
        logger.info(CAPTURED);
        logger.warning(CAPTURED);
        logger.severe(CAPTURED);

        // Then
        Map<String, Integer> metrics = getLogMetricsCounts();
        assertEquals(7, (int) metrics.get("Logging/lines"));
        assertEquals(1, (int) metrics.get("Logging/lines/FINEST"));
        assertEquals(1, (int) metrics.get("Logging/lines/FINER"));
        assertEquals(1, (int) metrics.get("Logging/lines/FINE"));
        assertEquals(1, (int) metrics.get("Logging/lines/CONFIG"));
        assertEquals(1, (int) metrics.get("Logging/lines/INFO"));
        assertEquals(1, (int) metrics.get("Logging/lines/WARNING"));
        assertEquals(1, (int) metrics.get("Logging/lines/SEVERE"));
    }

    @Trace(dispatcher = true)
    @Test
    public void shouldIncrementEmittedLogsCountersIndependentlyIfLogLevelEnabledEvenLoggingLogRecordsDirectly() {
        // Given
        final Logger logger = Logger.getLogger(JavaUtilLoggerTest.class.getName());
        logger.setLevel(Level.INFO);

        // When
        logger.log(new LogRecord(Level.FINEST, NOT_CAPTURED));
        logger.log(new LogRecord(Level.FINER, NOT_CAPTURED));
        logger.log(new LogRecord(Level.FINE, NOT_CAPTURED));
        logger.log(new LogRecord(Level.CONFIG, NOT_CAPTURED));
        logger.log(new LogRecord(Level.INFO, CAPTURED));
        logger.log(new LogRecord(Level.INFO, CAPTURED));
        logger.log(new LogRecord(Level.INFO, CAPTURED));
        logger.log(new LogRecord(Level.WARNING, CAPTURED));
        logger.log(new LogRecord(Level.WARNING, CAPTURED));
        logger.log(new LogRecord(Level.WARNING, CAPTURED));
        logger.log(new LogRecord(Level.WARNING, CAPTURED));
        logger.log(new LogRecord(Level.SEVERE, CAPTURED));

        // Then
        Map<String, Integer> metrics = getLogMetricsCounts();
        assertEquals(8, (int) metrics.get("Logging/lines"));
        assertEquals(0, (int) metrics.get("Logging/lines/FINEST"));
        assertEquals(0, (int) metrics.get("Logging/lines/FINER"));
        assertEquals(0, (int) metrics.get("Logging/lines/FINE"));
        assertEquals(0, (int) metrics.get("Logging/lines/CONFIG"));
        assertEquals(3, (int) metrics.get("Logging/lines/INFO"));
        assertEquals(4, (int) metrics.get("Logging/lines/WARNING"));
        assertEquals(1, (int) metrics.get("Logging/lines/SEVERE"));
    }

    private Map<String, Integer> getLogMetricsCounts() {
        Transaction transaction = Transaction.getTransaction();
        TransactionStats transactionStats = transaction.getTransactionActivity().getTransactionStats();
        SimpleStatsEngine engine = transactionStats.getUnscopedStats();
        final Map<String, Integer> metrics = new HashMap<>();
        metrics.put("Logging/lines", engine.getStats("Logging/lines").getCallCount());
        metrics.put("Logging/lines/FINEST", engine.getStats("Logging/lines/FINEST").getCallCount());
        metrics.put("Logging/lines/FINER", engine.getStats("Logging/lines/FINER").getCallCount());
        metrics.put("Logging/lines/FINE", engine.getStats("Logging/lines/FINE").getCallCount());
        metrics.put("Logging/lines/CONFIG", engine.getStats("Logging/lines/CONFIG").getCallCount());
        metrics.put("Logging/lines/INFO", engine.getStats("Logging/lines/INFO").getCallCount());
        metrics.put("Logging/lines/WARNING", engine.getStats("Logging/lines/WARNING").getCallCount());
        metrics.put("Logging/lines/SEVERE", engine.getStats("Logging/lines/SEVERE").getCallCount());
        return metrics;
    }

    private List<LogEvent> getFinestLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.FINEST.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getFinerLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.FINER.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getFineLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.FINE.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getConfigLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.CONFIG.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getInfoLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.INFO.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getWarningLevelLogEvents(Collection<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.WARNING.toString()))
                .collect(Collectors.toList());
    }

    private List<LogEvent> getSevereLevelLogEvents(List<LogEvent> logEvents) {
        return logEvents.stream()
                .filter(logEvent -> logEvent.getUserAttributesCopy().containsValue(Level.SEVERE.toString()))
                .collect(Collectors.toList());
    }

    private void doAssertions(int expectedTotalEventsCapturedAtSevere, int expectedTotalEventsCapturedAtWarning, int expectedTotalEventsCapturedAtInfo,
            int expectedTotalEventsCapturedAtConfig, int expectedTotalEventsCapturedAtFine, int expectedTotalEventsCapturedAtFiner,
            int expectedTotalEventsCapturedAtFinest) {
        int expectedTotalEventsCaptured = expectedTotalEventsCapturedAtSevere +
                expectedTotalEventsCapturedAtWarning + expectedTotalEventsCapturedAtInfo +
                expectedTotalEventsCapturedAtConfig + expectedTotalEventsCapturedAtFine +
                expectedTotalEventsCapturedAtFiner + expectedTotalEventsCapturedAtFinest;

        List<LogEvent> severeLevelLogEvents = getSevereLevelLogEvents(logReservoir.asList());
        List<LogEvent> warningLevelLogEvents = getWarningLevelLogEvents(logReservoir.asList());
        List<LogEvent> infoLevelLogEvents = getInfoLevelLogEvents(logReservoir.asList());
        List<LogEvent> configLevelLogEvents = getConfigLevelLogEvents(logReservoir.asList());
        List<LogEvent> fineLevelLogEvents = getFineLevelLogEvents(logReservoir.asList());
        List<LogEvent> finerLevelLogEvents = getFinerLevelLogEvents(logReservoir.asList());
        List<LogEvent> finestLevelLogEvents = getFinestLevelLogEvents(logReservoir.asList());

        assertEquals(expectedTotalEventsCaptured, logReservoir.size());
        assertEquals(expectedTotalEventsCapturedAtSevere, severeLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtWarning, warningLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtInfo, infoLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtConfig, configLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtFine, fineLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtFiner, finerLevelLogEvents.size());
        assertEquals(expectedTotalEventsCapturedAtFinest, finestLevelLogEvents.size());
    }
}
