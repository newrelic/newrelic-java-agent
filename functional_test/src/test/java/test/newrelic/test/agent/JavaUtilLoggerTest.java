package test.newrelic.test.agent;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Test for com.newrelic.instrumentation.java.logging-jdk8 instrumentation
 */
public class JavaUtilLoggerTest {

    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";

    @Before
    public void setup() {
        Transaction.clearTransaction();
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
        Assert.assertEquals(8, (int) metrics.get("Logging/lines"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/FINEST"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/FINER"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/FINE"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/CONFIG"));
        Assert.assertEquals(3, (int) metrics.get("Logging/lines/INFO"));
        Assert.assertEquals(4, (int) metrics.get("Logging/lines/WARNING"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/SEVERE"));
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
        Assert.assertEquals(7, (int) metrics.get("Logging/lines"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/FINEST"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/FINER"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/FINE"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/CONFIG"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/INFO"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/WARNING"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/SEVERE"));
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
        Assert.assertEquals(8, (int) metrics.get("Logging/lines"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/FINEST"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/FINER"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/FINE"));
        Assert.assertEquals(0, (int) metrics.get("Logging/lines/CONFIG"));
        Assert.assertEquals(3, (int) metrics.get("Logging/lines/INFO"));
        Assert.assertEquals(4, (int) metrics.get("Logging/lines/WARNING"));
        Assert.assertEquals(1, (int) metrics.get("Logging/lines/SEVERE"));
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
}
