/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.api.agent.weaver.CatchAndLog;
import com.newrelic.api.agent.weaver.Weave;

/**
 * CatchAndLogContext.java
 */
@Weave
public class CatchAndLogContext {
    public CatchAndLogListener catchAndLogListener = new CatchAndLogListener() {
        @CatchAndLog
        public void listen() {
            throw new RuntimeException("should be caught and logged");
        }
    };

    public CatchAndLogListener throwsListener = new CatchAndLogListener() {
        @Override
        public void listen() {
            throw new RuntimeException("should be thrown");
        }
    };
}
