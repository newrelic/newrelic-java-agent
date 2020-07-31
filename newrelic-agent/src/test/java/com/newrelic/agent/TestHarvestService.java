/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TestHarvestService extends HarvestServiceImpl {

    private final AtomicLong reportPeriod = new AtomicLong(60000);
    private final long initialDelay;

    public TestHarvestService() {
        this(0L);
    }

    public TestHarvestService(long initialDelay) {
        super();
        this.initialDelay = initialDelay;
    }

    @Override
    public long getInitialDelay() {
        return initialDelay;
    }

    @Override
    public long getReportingPeriod() {
        return reportPeriod.get();
    }

    public void setReportingPeriod(long period) {
        reportPeriod.set(period);
    }

    public long getMinHarvestInterval() {
        return TimeUnit.NANOSECONDS.convert(reportPeriod.get(), TimeUnit.MILLISECONDS);
    }
}
