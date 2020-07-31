/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import java.util.Objects;

public class W3CTraceParent {

    private static final byte FLAG_SAMPLED = 1; // 00000001

    private final String version;
    private final String traceId;
    private final String parentId;
    private final int flags;

    public W3CTraceParent(String version, String traceId, String parentId, int flags) {
        this.version = version;
        this.traceId = traceId;
        this.parentId = parentId;
        this.flags = flags;
    }

    public String getParentId() {
        return parentId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getVersion() {
        return version;
    }

    public int getFlags() {
        return flags;
    }

    public boolean sampled() {
        return (flags & FLAG_SAMPLED) == FLAG_SAMPLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        W3CTraceParent that = (W3CTraceParent) o;
        return flags == that.flags &&
                Objects.equals(version, that.version) &&
                Objects.equals(traceId, that.traceId) &&
                Objects.equals(parentId, that.parentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, traceId, parentId, flags);
    }
}
