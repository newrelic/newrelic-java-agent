/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.servlet;

import com.newrelic.agent.Agent;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueTimeTracker {

    protected static final String REQUEST_X_QUEUE_START_HEADER = "X-Queue-Start";
    private static final Pattern REQUEST_X_QUEUE_HEADER_PATTERN = Pattern.compile("\\s*(?:t=)?(-?[0-9.]+)");

    public static final String REQUEST_X_START_HEADER = "X-Request-Start";
    private static final Pattern REQUEST_X_START_HEADER_PATTERN = Pattern.compile("([^\\s\\/,=\\(\\)]+)? ?t=(-?[0-9.]+)|(-?[0-9.]+)");

    private final long queueTime;

    private QueueTimeTracker(Request httpRequest, long txStartTimeInNanos) {
        String requestXQueueStartHeader = ExternalTimeTracker.getRequestHeader(httpRequest,
                REQUEST_X_QUEUE_START_HEADER);
        String requestXStartHeader = ExternalTimeTracker.getRequestHeader(httpRequest, REQUEST_X_START_HEADER);
        queueTime = initQueueTime(requestXQueueStartHeader, requestXStartHeader, txStartTimeInNanos);
    }

    private long initQueueTime(String requestXQueueStartHeader, String requestXStartHeader, long txStartTimeInNanos) {
        long queueStartTimeInNanos = getQueueStartTimeFromHeader(requestXQueueStartHeader, requestXStartHeader);
        if (queueStartTimeInNanos > 0) {
            long queueTime = txStartTimeInNanos - queueStartTimeInNanos;
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                String msg = MessageFormat.format(
                        "Transaction start time (nanoseconds): {0}, queue start time (nanoseconds): {1}, queue time (nanoseconds): {2}",
                        txStartTimeInNanos, queueStartTimeInNanos, queueTime);
                Agent.LOG.finest(msg);
            }
            return Math.max(0L, queueTime);
        }
        return 0L;
    }

    private long getQueueStartTimeFromHeader(String requestXQueueStartHeader, String requestXStartHeader) {
        if (requestXQueueStartHeader != null) {
            Matcher matcher = REQUEST_X_QUEUE_HEADER_PATTERN.matcher(requestXQueueStartHeader);
            if (matcher.find()) {
                String queueStartTime = matcher.group(1);
                try {
                    return ExternalTimeTracker.parseTimestampToNano(queueStartTime);
                } catch (NumberFormatException e) {
                    String msg = MessageFormat.format("Error parsing queue start time {0} in {1} header: {2}",
                            queueStartTime, REQUEST_X_QUEUE_START_HEADER, e);
                    Agent.LOG.log(Level.FINER, msg);
                }
            } else {
                String msg = MessageFormat.format("Failed to parse queue start time in {0} header: {1}",
                        REQUEST_X_QUEUE_START_HEADER, requestXQueueStartHeader);
                Agent.LOG.log(Level.FINER, msg);
            }
        }

        if (requestXStartHeader != null) {
            Matcher matcher = REQUEST_X_START_HEADER_PATTERN.matcher(requestXStartHeader);
            while (matcher.find()) {
                String serverName = matcher.group(1); // Not used
                String serverStartTime = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
                try {
                    return ExternalTimeTracker.parseTimestampToNano(serverStartTime);
                } catch (NumberFormatException e) {
                    String msg = MessageFormat.format("Error parsing server time {0} in {1}: {2}", serverStartTime,
                            REQUEST_X_START_HEADER, e);
                    Agent.LOG.log(Level.FINER, msg);
                }
            }
        }

        return 0L;
    }

    /**
     * @return the queue time in nanoseconds
     */
    public long getQueueTime() {
        return queueTime;
    }

    public void recordMetrics(TransactionStats statsEngine) {
        if (queueTime > 0L) {
            MetricName name = MetricName.QUEUE_TIME;
            statsEngine.getUnscopedStats().getOrCreateResponseTimeStats(name.getName()).recordResponseTimeInNanos(queueTime);
        }
    }

    public static QueueTimeTracker create(Request httpRequest, long txStartTimeInNanos) {
        return new QueueTimeTracker(httpRequest, txStartTimeInNanos);
    }
}
