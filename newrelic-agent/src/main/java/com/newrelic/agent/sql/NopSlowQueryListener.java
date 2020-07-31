/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.SlowQueryDatastoreParameters;

import java.util.Collections;
import java.util.List;

/**
 * This class is immutable and therefore thread safe.
 */
public class NopSlowQueryListener implements SlowQueryListener {

    @Override
    public <T> void noticeTracer(Tracer tracer, SlowQueryDatastoreParameters<T> datastoreParameters) {
        // do nothing
    }

    @Override
    public List<SlowQueryInfo> getSlowQueries() {
        return Collections.emptyList();
    }

}
