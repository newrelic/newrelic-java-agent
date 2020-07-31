/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.model.ApdexPerfZone;

import static com.newrelic.agent.model.ApdexPerfZone.FRUSTRATING;
import static com.newrelic.agent.model.ApdexPerfZone.SATISFYING;
import static com.newrelic.agent.model.ApdexPerfZone.TOLERATING;

public class ApdexPerfZoneDetermination {

    public static ApdexPerfZone getZone(long responseTimeMillis, long apdexTInMillis) {
        if (responseTimeMillis <= apdexTInMillis) {
            return SATISFYING;
        } else if (responseTimeMillis <= 4 * apdexTInMillis) {
            return TOLERATING;
        } else {
            return FRUSTRATING;
        }
    }
}
