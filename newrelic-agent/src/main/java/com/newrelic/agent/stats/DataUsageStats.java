/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

/**
 * Used to generate metrics on data usage
 */
public interface DataUsageStats extends StatsBase {

    /**
     * Record the uncompressed sizes of sent and received payloads in bytes for each agent endpoint.
     *
     * @param bytesSent uncompressed bytes sent to an agent endpoint
     * @param bytesReceived uncompressed bytes received from an agent endpoint
     */
    void recordDataUsage(long bytesSent, long bytesReceived);

    /**
     * Get the count of the number of times the metric was set.
     *
     * @return count
     */
    int getCount();

    /**
     * Get the amount of uncompressed bytes sent for a metric representing calls to an agent endpoint.
     *
     * @return bytes sent
     */
    long getBytesSent();

    /**
     * Get the amount of uncompressed bytes received for a metric representing responses from an agent endpoint.
     *
     * @return bytes sent
     */
    long getBytesReceived();

}
