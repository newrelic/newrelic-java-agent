/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

// Transaction timeout cause
// https://pages.datanerd.us/engineering-management/architecture-notes/notes/123/#nrtimeoutCause
public enum TimeoutCause {
    SEGMENT("segment"),
    TOKEN("token");

    public final String cause;

    TimeoutCause(String cause) {
        this.cause = cause;
    }

    @Override
    public String toString() {
        return this.cause;
    }
}
