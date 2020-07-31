/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

public class MaintenanceError extends Exception {

    private static final long serialVersionUID = 8391541783636377551L;

    public MaintenanceError(String message) {
        super(message);
    }

}
