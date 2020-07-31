/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import com.newrelic.agent.stats.StatsEngine;

public interface MetricSampler {

    void sample(StatsEngine statsEngine);

}
