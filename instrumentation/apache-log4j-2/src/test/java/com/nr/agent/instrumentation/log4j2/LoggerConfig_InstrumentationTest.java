package com.nr.agent.instrumentation.log4j2;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.MetricsHelper;
import junit.framework.TestCase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.logging.log4j.core" }, configName = "application_logging_enabled.yml")
public class LoggerConfig_InstrumentationTest extends TestCase {

    private static final String CAPTURED = "This log message should be captured";
    private static final String NOT_CAPTURED = "This message should NOT be captured";

    // TODO add tests for LogEvents being created when recordNewRelicLogEvent is called
    //  this is probably blocked until the Introspector is updated

    @Before
    public void resetLoggerConfiguration() {
        Configurator.reconfigure();
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
        AppenderRef[] refs = new AppenderRef[] {ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(additivity, level, name, "true", refs, null, config, null );
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

}
