/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

public class IgnoredError extends Exception {

    public IgnoredError(String message) {
        super(message);
    }

}
