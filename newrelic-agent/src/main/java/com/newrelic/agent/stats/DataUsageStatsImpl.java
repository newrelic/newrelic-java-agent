/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.api.agent.NewRelic;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class DataUsageStatsImpl implements DataUsageStats {

    private static final int UNUSED = 0; // min, max, sum of squares
    private final AtomicInteger count = new AtomicInteger(0); // count
    private final AtomicLong bytesSent = new AtomicLong(0); // total time
    private final AtomicLong bytesReceived = new AtomicLong(0); // exclusive time

    protected DataUsageStatsImpl() {
        super();
    }

    /**
     * Record the uncompressed sizes of sent and received payloads in bytes for each agent endpoint.
     *
     * @param bytesSent     uncompressed bytes sent to an agent endpoint
     * @param bytesReceived uncompressed bytes received from an agent endpoint
     */
    @Override
    public void recordDataUsage(long bytesSent, long bytesReceived) {
        this.count.incrementAndGet();
        this.bytesSent.addAndGet(bytesSent);
        this.bytesReceived.addAndGet(bytesReceived);

        if (NewRelic.getAgent().getConfig().getValue(AgentConfigImpl.METRIC_DEBUG, AgentConfigImpl.DEFAULT_METRIC_DEBUG)) {
            if (this.count.get() < 0 || this.bytesSent.get() < 0 || this.bytesReceived.get() < 0) {
                NewRelic.incrementCounter("Supportability/DataUsageStatsImpl/NegativeValue");
                Agent.LOG.log(Level.INFO, "Invalid count {0}, bytesSent {1}, or bytesReceived {2}",
                        this.count.get(), this.bytesSent.get(), this.bytesReceived.get());
            }
        }

    }

    /**
     * Get the count of the number of times the metric was set.
     *
     * @return count
     */
    @Override
    public int getCount() {
        return count.get();
    }

    /**
     * Get the amount of uncompressed bytes sent for a metric representing calls to an agent endpoint.
     *
     * @return bytes sent
     */
    @Override
    public long getBytesSent() {
        return bytesSent.get();
    }

    /**
     * Get the amount of uncompressed bytes received for a metric representing responses from an agent endpoint.
     *
     * @return bytes sent
     */
    @Override
    public long getBytesReceived() {
        return bytesReceived.get();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DataUsageStatsImpl newStats = new DataUsageStatsImpl();
        newStats.count.set(count.get());
        newStats.bytesSent.set(bytesSent.get());
        newStats.bytesReceived.set(bytesReceived.get());
        return newStats;
    }

    @Override
    public String toString() {
        return super.toString() + " [count=" + count.get() + ", bytesSent=" + bytesSent.get() + ", bytesReceived=" + bytesReceived.get() + "]";
    }

    @Override
    public boolean hasData() {
        return count.get() > 0 || bytesSent.get() > 0 || bytesReceived.get() > 0;
    }

    @Override
    public void reset() {
        count.set(0);
        bytesSent.set(0);
        bytesReceived.set(0);
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        List<Number> data;
        // These map to traditional metric values of: count, total time, exclusive time, min, max, sum of squares
        data = Arrays.asList(count.get(), bytesSent.get(), bytesReceived.get(), UNUSED, UNUSED, UNUSED);
        org.json.simple.JSONArray.writeJSONString(data, writer);
    }

    @Override
    public void merge(StatsBase statsObj) {
        if (statsObj instanceof DataUsageStatsImpl) {
            DataUsageStatsImpl stats = (DataUsageStatsImpl) statsObj;
            count.addAndGet(stats.count.get());
            bytesSent.addAndGet(stats.bytesSent.get());
            bytesReceived.addAndGet(stats.bytesReceived.get());
        }
    }
}
