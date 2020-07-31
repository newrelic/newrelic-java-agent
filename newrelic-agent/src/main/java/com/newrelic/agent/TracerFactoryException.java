/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

/**
 * Thrown if a tracer factory can't be loaded.
 */
public class TracerFactoryException extends Exception {

    private static final long serialVersionUID = -6103280171903439862L;

    public TracerFactoryException(String message) {
        super(message);
    }

    public TracerFactoryException(String message, Exception e) {
        super(message, e);
    }

}
