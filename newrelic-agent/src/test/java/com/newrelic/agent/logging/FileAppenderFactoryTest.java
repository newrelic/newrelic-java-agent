package com.newrelic.agent.logging;

import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.NoOpTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class FileAppenderFactoryTest {

    private static final String FILENAME = "newrelic_agent.log";
    private static final long BYTE_LIMIT = 1024L;

    @Test
    public void build_withLogDailyTrueAndByteLimitGreaterThanZero_createsDailyRollingFileAppender() {
        FileAppenderFactory factory = new FileAppenderFactory(1, BYTE_LIMIT, FILENAME + "0", true, "newrelic.log");
        AbstractOutputStreamAppender<? extends FileManager> fileAppender = factory.build();
        RollingFileAppender rollingFileAppender = (RollingFileAppender) fileAppender;

        Assert.assertEquals(RollingFileAppender.class, fileAppender.getClass());
        Assert.assertEquals(CompositeTriggeringPolicy.class, rollingFileAppender.getTriggeringPolicy().getClass());
        Assert.assertEquals(FILENAME + "0", rollingFileAppender.getFileName());

        CompositeTriggeringPolicy policy = rollingFileAppender.getTriggeringPolicy();
        Assert.assertTrue((policy.getTriggeringPolicies()[0] instanceof TimeBasedTriggeringPolicy) ||
                (policy.getTriggeringPolicies()[0] instanceof SizeBasedTriggeringPolicy));
        Assert.assertTrue((policy.getTriggeringPolicies()[1] instanceof TimeBasedTriggeringPolicy) ||
                (policy.getTriggeringPolicies()[1] instanceof SizeBasedTriggeringPolicy));
    }

    @Test
    public void build_withLogDailyTrueAndByteLimitEqualZero_createsDailyRollingFileAppender() {
        FileAppenderFactory factory = new FileAppenderFactory(1, 0, FILENAME + "1", true, "newrelic.log");
        AbstractOutputStreamAppender<? extends FileManager> fileAppender = factory.build();
        RollingFileAppender rollingFileAppender = (RollingFileAppender) fileAppender;

        Assert.assertEquals(RollingFileAppender.class, fileAppender.getClass());
        Assert.assertEquals(CompositeTriggeringPolicy.class, rollingFileAppender.getTriggeringPolicy().getClass());
        Assert.assertEquals(FILENAME + "1", rollingFileAppender.getFileName());

        CompositeTriggeringPolicy policy = rollingFileAppender.getTriggeringPolicy();
        Assert.assertTrue((policy.getTriggeringPolicies()[0] instanceof TimeBasedTriggeringPolicy) ||
                (policy.getTriggeringPolicies()[0] instanceof NoOpTriggeringPolicy));
        Assert.assertTrue((policy.getTriggeringPolicies()[1] instanceof TimeBasedTriggeringPolicy) ||
                (policy.getTriggeringPolicies()[1] instanceof NoOpTriggeringPolicy));
    }

    @Test
    public void build_withLogDailyFalseAndByteLimitGreaterThanZero_createsSizeBasedRollingFileAppender() {
        FileAppenderFactory factory = new FileAppenderFactory(1, BYTE_LIMIT, FILENAME + "2", false, "newrelic.log");
        AbstractOutputStreamAppender<? extends FileManager> fileAppender = factory.build();
        RollingFileAppender rollingFileAppender = (RollingFileAppender) fileAppender;

        Assert.assertEquals(RollingFileAppender.class, fileAppender.getClass());
        Assert.assertEquals(FILENAME + "2", rollingFileAppender.getFileName());

        SizeBasedTriggeringPolicy policy = rollingFileAppender.getTriggeringPolicy();
        Assert.assertEquals(SizeBasedTriggeringPolicy.class, policy.getClass());
        Assert.assertEquals(BYTE_LIMIT, policy.getMaxFileSize());
    }

    @Test
    public void build_withLogDailyFalseAndByteLimitEqualZero_createsSizeBasedRollingFileAppender() {
        FileAppenderFactory factory = new FileAppenderFactory(1, 0, FILENAME + "3", false, "newrelic.log");
        AbstractOutputStreamAppender<? extends FileManager> fileAppender = factory.build();
        FileAppender rollingFileAppender = (FileAppender) fileAppender;

        Assert.assertEquals(FileAppender.class, fileAppender.getClass());
        Assert.assertEquals(FILENAME + "3", rollingFileAppender.getFileName());
    }

    @AfterClass
    public static void afterTests() {
        for (int idx = 0; idx < 4; idx++) {
            new File(FILENAME + Integer.toString(idx)).delete();
        }
    }
}
