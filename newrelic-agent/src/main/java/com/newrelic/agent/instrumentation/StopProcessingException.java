/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

/**
 * This exception can be thrown to stop processing class bytes if it is determined that the class is not of interest.
 */
public class StopProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StopProcessingException(String msg) {
        super(msg);
    }

}
