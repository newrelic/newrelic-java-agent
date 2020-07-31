/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import java.util.Map;

public interface SqlTrace {

    long getId();

    String getQuery();

    int getCallCount();

    long getTotal();

    long getMax();

    long getMin();

    String getBlameMetricName();

    String getUri();

    String getMetricName();

    Map<String, Object> getParameters();

}
