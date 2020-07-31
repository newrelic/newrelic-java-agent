/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.AsyncApi;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.atomic.AtomicReference;

public enum TransactionErrorPriority {
    /**
     * Set by the user via a call to {@link NewRelic#noticeError} or {@link AsyncApi#errorAsync(Object, Throwable)}.
     * Only take the first.
     */
    API {
        @Override
        protected boolean updatePriority(AtomicReference<TransactionErrorPriority> current) {
            // Only take the first api level error.
            if (this == current.get()) {
                return false;
            }
            return current.compareAndSet(TRACER, this);
        }
    },

    /**
     * Set by finishing a root tracer with a throwable. Last set wins.
     */
    TRACER {
        @Override
        protected boolean updatePriority(AtomicReference<TransactionErrorPriority> current) {
            // if TRACER, allow setting with the same priority, last set wins.
            return this == current.get();
        }
    };

    protected abstract boolean updatePriority(AtomicReference<TransactionErrorPriority> current);

    /**
     * TRACER < API
     *
     * "this" references the new priority. "other" is the existing priority.
     *
     * @return value is whether to update the throwable field.
     */
    public boolean updateCurrentPriority(AtomicReference<TransactionErrorPriority> current) {
        // always set if null
        if (current.compareAndSet(null, this)) {
            return true;
        }
        return updatePriority(current);
    }
}
