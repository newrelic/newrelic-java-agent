/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.api.agent.Token;

public class NRComparableRunnable<T> extends NRRunnable implements Comparable<T> {
    private Comparable<T> comparableDelegate = null;

    @SuppressWarnings("unchecked")
    public NRComparableRunnable(Runnable runnable, Token token) {
        super(runnable, token);
        if (runnable instanceof Comparable) {
            comparableDelegate = (Comparable<T>)runnable;
        }
    }

    @Override
    public int compareTo(T o) {
        if (comparableDelegate != null) {
            return comparableDelegate.compareTo(o);
        }

        return 0;
    }
}
