/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala;

import com.newrelic.agent.bridge.AgentBridge;
import scala.Function0;
import scala.runtime.AbstractFunction0;

public class WrappedFunction0 extends AbstractFunction0 {

    public Function0 original;
    public AgentBridge.TokenAndRefCount tokenAndRefCount;

    public WrappedFunction0(Function0 original, AgentBridge.TokenAndRefCount tokenAndRefCount) {
        this.original = original;
        this.tokenAndRefCount = tokenAndRefCount;
    }

    @Override
    public Object apply() {
        return original.apply();
    }

}
