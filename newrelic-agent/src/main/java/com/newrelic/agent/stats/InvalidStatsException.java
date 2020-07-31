/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

public class InvalidStatsException extends RuntimeException {

    private static final long serialVersionUID = -4680720624395039293L;

    InvalidStatsException(String message) {
        super(message);
    }

}
