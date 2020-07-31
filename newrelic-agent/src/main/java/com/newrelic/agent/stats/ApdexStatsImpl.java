/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.util.TimeConversion;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/**
 * This class is not thread-safe.
 */
public class ApdexStatsImpl implements ApdexStats {

    private static final Integer ZERO = 0;

    private int satisfying; // count
    private int tolerating; // total
    private int frustrating; // exclusive
    private long apdexTInMillis = ZERO; // min, max

    protected ApdexStatsImpl() {
        super();
    }

    // Used by the server mode
    public ApdexStatsImpl(int s, int t, int f) {
        super();
        this.satisfying = s;
        this.tolerating = t;
        this.frustrating = f;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ApdexStatsImpl newStats = new ApdexStatsImpl();
        newStats.frustrating = frustrating;
        newStats.satisfying = satisfying;
        newStats.tolerating = tolerating;
        return newStats;
    }

    @Override
    public String toString() {
        return super.toString() + " [s=" + satisfying + ", t=" + tolerating + ", f=" + frustrating + "]";
    }

    @Override
    public void recordApdexFrustrated() {
        frustrating++;
    }

    @Override
    public int getApdexSatisfying() {
        return satisfying;
    }

    @Override
    public int getApdexTolerating() {
        return tolerating;
    }

    @Override
    public int getApdexFrustrating() {
        return frustrating;
    }

    @Override
    public void recordApdexResponseTime(long responseTimeMillis, long apdexTInMillis) {
        this.apdexTInMillis = apdexTInMillis;
        ApdexPerfZone perfZone = ApdexPerfZoneDetermination.getZone(responseTimeMillis, apdexTInMillis);
        switch (perfZone) {
        case SATISFYING:
            satisfying++;
            break;
        case TOLERATING:
            tolerating++;
            break;
        case FRUSTRATING:
            recordApdexFrustrated();
            break;
        }
    }

    @Override
    public boolean hasData() {
        return satisfying > 0 || tolerating > 0 || frustrating > 0;
    }

    @Override
    public void reset() {
        satisfying = 0;
        tolerating = 0;
        frustrating = 0;
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        double apdexT = Long.valueOf(apdexTInMillis).doubleValue() / TimeConversion.MILLISECONDS_PER_SECOND;
        List<Number> data;
        // count, time, exclusive_time, min_time, max_time, sum_of_squares
        data = Arrays.asList(satisfying, tolerating, frustrating,
                apdexT, apdexT, (Number) ZERO);
        org.json.simple.JSONArray.writeJSONString(data, writer);
    }

    @Override
    public void merge(StatsBase statsObj) {
        if (statsObj instanceof ApdexStatsImpl) {
            ApdexStatsImpl stats = (ApdexStatsImpl) statsObj;

            satisfying += stats.satisfying;
            tolerating += stats.tolerating;
            frustrating += stats.frustrating;
        }
    }

}
