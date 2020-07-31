/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.spans;

public class CustomFooException extends RuntimeException {
    public CustomFooException(String message, Throwable cause) {
        super(message, cause);
    }
}
