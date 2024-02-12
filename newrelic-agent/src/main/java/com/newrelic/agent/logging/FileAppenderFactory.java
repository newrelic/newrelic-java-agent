/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.api.agent.NewRelic;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.NoOpTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.newrelic.agent.logging.Log4jLogger.CONVERSION_PATTERN;

public class FileAppenderFactory {

    /**
     * The minimum number of files.
     */
    private static final int MIN_FILE_COUNT = 1;

    /**
     * The default is to append to the file.
     */
    private static final boolean APPEND_TO_FILE = true;
    private static final String DAILY_CRON = "0 0 0 * * ?";
    /**
     * The name of the file appender.
     */
    static final String FILE_APPENDER_NAME = "File";

    private final int fileCount;
    private final long logLimitBytes;
    private final String fileName;
    private final boolean isDaily;

    /**
     * @param fileCount maximum number of log files
     * @param logLimitBytes maximum size of a given log file
     * @param fileName prefix for log file names
     * @param isDaily if the logs are to be rolled over daily
     */
    public FileAppenderFactory(int fileCount, long logLimitBytes, String fileName, boolean isDaily) {
        this.fileCount = fileCount;
        this.logLimitBytes = logLimitBytes;
        this.fileName = fileName;
        this.isDaily = isDaily;
    }

    /**
     * Create a full initialized FileAppender with a {@link TriggeringPolicy} set based on the configuration.
     *
     * @return file appender to log to
     */
    AbstractOutputStreamAppender<? extends FileManager> build() {
        AbstractOutputStreamAppender<? extends FileManager> rollingFileAppender = buildRollingFileAppender();
        rollingFileAppender.start();
        return rollingFileAppender;
    }

    private AbstractOutputStreamAppender<? extends FileManager> buildRollingFileAppender() {
        if (isDaily) {
            return buildDailyRollingAppender();
        }

        if (logLimitBytes > 0) {
            return initializeRollingFileAppender()
                    .withStrategy(DefaultRolloverStrategy.newBuilder()
                            .withMin(String.valueOf(MIN_FILE_COUNT))
                            .withMax(String.valueOf(Math.max(1, fileCount)))
                            .build())
                    .withPolicy(sizeBasedPolicy())
                    .withFilePattern(fileName + ".%i")
                    .build();
        }

        return buildDefaultFileAppender(fileName);
    }

    private AbstractOutputStreamAppender<? extends FileManager> buildDefaultFileAppender(String fileName) {
        return ((FileAppender.Builder) FileAppender.newBuilder()
                .withFileName(fileName)
                .withAppend(APPEND_TO_FILE)
                .setName(FILE_APPENDER_NAME)
                .setLayout(PatternLayout.newBuilder().withPattern(CONVERSION_PATTERN).build()))
                .build();
    }

    private RollingFileAppender buildDailyRollingAppender() {

        TriggeringPolicy policy = buildRollingAppenderTriggeringPolicy();
        DefaultRolloverStrategy rolloverStrategy = DefaultRolloverStrategy.newBuilder()
                .withMax(String.valueOf(fileCount))
                .build();

        String filePattern = fileName + ".%d{yyyy-MM-dd}.%i";
        if (logLimitBytes > 0) {
            // If we might roll within a day, use a number ordering suffix
            filePattern = fileName + ".%d{yyyy-MM-dd}.%i";
        }

        long initialDelaySeconds = 60;
        int repeatIntervalSeconds = fileCount * 24 * 60 * 60;
        String fileNamePrefix = NewRelic.getAgent().getConfig().getValue("log_file_name");
        String filePath = NewRelic.getAgent().getConfig().getValue("log_file_path");
        Path directory = new File(filePath).toPath();

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                new DeleteLogFilesRunnable(directory, fileCount, fileNamePrefix),
                initialDelaySeconds,
                repeatIntervalSeconds,
                TimeUnit.SECONDS
        );

        return initializeRollingFileAppender()
                .withPolicy(policy)
                .withFilePattern(filePattern)
                .withStrategy(rolloverStrategy)
                .build();

    }

    private TriggeringPolicy buildRollingAppenderTriggeringPolicy() {
        TimeBasedTriggeringPolicy timeBasedTriggeringPolicy = TimeBasedTriggeringPolicy.newBuilder().withInterval(1).withModulate(true).build();
        TriggeringPolicy sizeBasedTriggeringPolicy = sizeBasedPolicy();
        return CompositeTriggeringPolicy.createPolicy(timeBasedTriggeringPolicy, sizeBasedTriggeringPolicy);
    }

    private RollingFileAppender.Builder initializeRollingFileAppender() {
        return (RollingFileAppender.Builder) RollingFileAppender.newBuilder()
                .withFileName(fileName)
                .withAppend(APPEND_TO_FILE)
                .setName(FILE_APPENDER_NAME)
                .setLayout(PatternLayout.newBuilder().withPattern(CONVERSION_PATTERN).build());
    }

    private TriggeringPolicy sizeBasedPolicy() {
        return (logLimitBytes > 0) ?
                SizeBasedTriggeringPolicy.createPolicy(String.valueOf(logLimitBytes)) :
                NoOpTriggeringPolicy.createPolicy();
    }

}
