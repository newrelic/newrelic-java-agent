/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.jruby;

/**
 * What's the deal with this class? What's the deal with this package?
 * 
 * Well, we need a class a pointcut will instrument so we can test with it.
 */
public class DoNothingClassThatExistsForTesting {

    public void trace() {
        // I don't do anything!
    }

}
