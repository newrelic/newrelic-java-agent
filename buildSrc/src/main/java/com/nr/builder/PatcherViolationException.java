/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

public class PatcherViolationException extends RuntimeException {
    public PatcherViolationException(String message) {
        super(message);
    }
}
