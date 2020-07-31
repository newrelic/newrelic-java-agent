/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

public class LicenseException extends Exception {

    private static final long serialVersionUID = 7083837395917957355L;

    public LicenseException(String message) {
        super(message);
    }

}
