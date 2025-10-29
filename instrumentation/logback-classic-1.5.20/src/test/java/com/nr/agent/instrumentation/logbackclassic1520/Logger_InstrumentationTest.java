package com.nr.agent.instrumentation.logbackclassic1520;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.MetricsHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "ch.qos.logback" }, configName = "application_logging_enabled.yml")
public class Logger_InstrumentationTest {

    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";

    @Test
    public void shouldIncrementEmittedLogsCountersIndependentlyIfLogLevelEnabled() {
        // Given
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger_InstrumentationTest.class);
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
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger_InstrumentationTest.class);
        logger.setLevel(Level.TRACE);

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
}