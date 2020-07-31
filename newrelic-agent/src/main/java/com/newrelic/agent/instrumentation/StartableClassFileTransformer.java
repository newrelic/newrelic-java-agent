/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import com.newrelic.agent.InstrumentationProxy;

public interface StartableClassFileTransformer extends ClassFileTransformer {
    /**
     * Start method to be called after this transformer is added to {@link Instrumentation}.
     */
    void start(InstrumentationProxy instrumentation, boolean isRetransformSupported);
}
