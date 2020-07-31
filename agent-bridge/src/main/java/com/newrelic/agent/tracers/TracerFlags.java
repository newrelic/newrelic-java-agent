/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

public final class TracerFlags {

    private TracerFlags() {
    }

    // 0 represents no flags
    public static final int GENERATE_SCOPED_METRIC = (1 << 1);
    public static final int TRANSACTION_TRACER_SEGMENT = (1 << 2);
    public static final int DISPATCHER = (1 << 3);
    public static final int CUSTOM = (1 << 4);
    public static final int LEAF = (1 << 5);
    public static final int ASYNC = (1 << 6);

    public static boolean isRoot(int flags) {
        return isDispatcher(flags) || isAsync(flags);
    }

    public static int forceMandatoryRootFlags(int flags) {
        return flags | TRANSACTION_TRACER_SEGMENT | GENERATE_SCOPED_METRIC;
    }

    public static boolean isAsync(int flags) {
        return (flags & ASYNC) == ASYNC;
    }

    public static int clearAsync(int flags) {
        return flags & ~ASYNC;
    }

    public static boolean isDispatcher(int flags) {
        return (flags & DISPATCHER) == DISPATCHER;
    }

    public static int clearSegment(int flags) {
        return flags & ~TRANSACTION_TRACER_SEGMENT;
    }

    public static boolean isCustom(int flags) {
        return (flags & CUSTOM) == CUSTOM;
    }

    public static int getDispatcherFlags(boolean dispatcher) {
        return dispatcher ? DISPATCHER : 0;
    }

}
