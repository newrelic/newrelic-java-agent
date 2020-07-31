/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

public class MetricDataException extends Exception {

    private static final long serialVersionUID = 1L;

    public MetricDataException(String message) {
        super(message);
    }
}
