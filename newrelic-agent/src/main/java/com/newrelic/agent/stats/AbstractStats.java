/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONArray;

/**
 * This class is not thread-safe.
 */
public abstract class AbstractStats implements CountStats {

    private static final List<Number> ZERO_ARRAY_LIST;
    static {
        Number zero = 0;
        ZERO_ARRAY_LIST = Arrays.asList(zero, zero, zero, zero, zero, zero);
    }
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Used when we want to send up a metric with a zero call count. Not available in the public api.
     */
    public static final StatsBase EMPTY_STATS = new StatsBase() {

        @Override
        public boolean hasData() {
            return true;
        }

        @Override
        public void merge(StatsBase stats) {
        }

        @Override
        public void reset() {
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public void writeJSONString(Writer writer) throws IOException {
            JSONArray.writeJSONString(ZERO_ARRAY_LIST, writer);
        }
    };

    public AbstractStats() {
        super();
    }

    public AbstractStats(int count) {
        super();
        this.count.set(count);
    }

    @Override
    public void incrementCallCount(int value) {
        this.count.addAndGet(value);
    }

    @Override
    public void incrementCallCount() {
        this.count.incrementAndGet();
    }

    @Override
    public int getCallCount() {
        return count.get();
    }

    @Override
    public void setCallCount(int count) {
        this.count.set(count);
    }

    @Override
    public final void writeJSONString(Writer writer) throws IOException, InvalidStatsException {
        List<Number> list;
        if (count.get() < 0) {
            list = ZERO_ARRAY_LIST;
        } else {
            list = Arrays.asList(count, getTotal(), getTotalExclusiveTime(), getMinCallTime(),
                    getMaxCallTime(), (Number) getSumOfSquares());
        }
        JSONArray.writeJSONString(list, writer);
    }

    @Override
    public abstract Object clone() throws CloneNotSupportedException;

}
