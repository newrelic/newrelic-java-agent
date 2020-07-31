/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

public enum ApdexPerfZone {
    SATISFYING("S"), TOLERATING("T"), FRUSTRATING("F");

    private final String zone;

    ApdexPerfZone(String zone) {
        this.zone = zone;
    }

    public String getZone() {
        return zone;
    }
}