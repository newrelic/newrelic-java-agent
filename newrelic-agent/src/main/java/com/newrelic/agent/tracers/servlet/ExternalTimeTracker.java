/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.servlet;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;

public class ExternalTimeTracker {

    /* Any timestamps before this are thrown out and the parser will try again with a larger unit (2000/1/1 UTC) */
    static final long EARLIEST_ACCEPTABLE_TIMESTAMP_NANO = 946684800000000000l;
    static final List<Integer> MAGNITUDES = Arrays.asList(1 /* nano */, 1000 /* micro */, 1000000 /* milli */,
            1000000000 /* second */);

    private final QueueTimeTracker queueTimeTracker;
    private final long externalTime;

    private ExternalTimeTracker(Request httpRequest, long txStartTimeInMillis) {
        if (httpRequest == null) {
            Agent.LOG.finer("Unable to get headers: HttpRequest is null");
        }
        long txStartTimeInNanos = parseTimestampToNano(txStartTimeInMillis);
        queueTimeTracker = QueueTimeTracker.create(httpRequest, txStartTimeInNanos);
        long queueTime = queueTimeTracker.getQueueTime();
        externalTime = TimeUnit.MILLISECONDS.convert(queueTime, TimeUnit.NANOSECONDS);
    }

    /**
     * @return the total external time in milliseconds
     */
    public long getExternalTime() {
        return externalTime;
    }

    public void recordMetrics(TransactionStats statsEngine) {
        queueTimeTracker.recordMetrics(statsEngine);
    }

    /**
     * Get the given request header.
     */
    protected static String getRequestHeader(Request httpRequest, String headerName) {
        if (httpRequest == null) {
            return null;
        }
        try {
            String header = httpRequest.getHeader(headerName);
            if (header != null) {
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Got {0} header: {1}", headerName, header);
                    Agent.LOG.finer(msg);
                }
            }
            return header;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Error getting {0} header: {1}", headerName, t.toString());
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, t);
            } else {
                Agent.LOG.finer(msg);
            }
            return null;
        }
    }

    public static ExternalTimeTracker create(Request httpRequest, long txStartTimeInMillis) {
        return new ExternalTimeTracker(httpRequest, txStartTimeInMillis);
    }

    public static long parseTimestampToNano(String strTime) throws NumberFormatException {
        double time = Double.parseDouble(strTime);
        return parseTimestampToNano(time);
    }

    public static long parseTimestampToNano(double time) throws NumberFormatException {
        if (time > 0) {
            for (long magnitude : MAGNITUDES) {
                long candidate = (long) (time * magnitude);
                if (EARLIEST_ACCEPTABLE_TIMESTAMP_NANO < candidate
                        && candidate < EARLIEST_ACCEPTABLE_TIMESTAMP_NANO * 999) {
                    return candidate;
                }
            }
        }
        throw new NumberFormatException("The long " + time
                + " could not be converted to a timestamp in nanoseconds (wrong magnitude).");
    }
}
