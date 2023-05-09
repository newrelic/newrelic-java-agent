/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.service.ServiceFactory;

import java.util.concurrent.TimeUnit;

/**
 * NEVER USE THIS IT ONLY SERVES THE TRANSACTION CLASS!
 * <p>
 * Do not use this class in any new code (unless you find a duplicated method in Transaction)
 * <p>
 * These calls through the ServiceFactory used to be done statically at
 * class load time in the Transaction, which makes creating instances
 * (or even class loading) impossible without additional service locator
 * setup.  By making these lazy, we are able to load the Transaction class
 * without requiring the entire configuration subsystem to be initialized.
 */
class TransactionStaticsHolder {
    private static long _segmentTimeoutMillis = Long.MIN_VALUE;
    private static int _asyncTimeoutSeconds = Integer.MIN_VALUE;

    private UncoveredCodeExample uncoveredCodeExample = new UncoveredCodeExample();

    static long SEGMENT_TIMEOUT_MILLIS() {
        if (_segmentTimeoutMillis == Long.MIN_VALUE) {
            _segmentTimeoutMillis = TimeUnit.SECONDS.toMillis(
                    ServiceFactory.getConfigService()
                            .getDefaultAgentConfig()
                            .getSegmentTimeoutInSec());
        }
        return _segmentTimeoutMillis;
    }

    static int ASYNC_TIMEOUT_SECONDS() {
        if (_asyncTimeoutSeconds == Integer.MIN_VALUE) {
            _asyncTimeoutSeconds = ServiceFactory.getConfigService()
                    .getDefaultAgentConfig()
                    .getTokenTimeoutInSec();
        }
        return _asyncTimeoutSeconds;
    }

    static long ASYNC_TIMEOUT_NANO() {
        return TimeUnit.SECONDS.toNanos(ASYNC_TIMEOUT_SECONDS());
    }
}
