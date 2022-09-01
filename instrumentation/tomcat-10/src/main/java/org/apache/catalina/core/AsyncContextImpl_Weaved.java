/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.AsyncContext;

@Weave(originalName = "org.apache.catalina.core.AsyncContextImpl")
public abstract class AsyncContextImpl_Weaved implements AsyncContext {

    public void complete() {

        AgentBridge.asyncApi.completeAsync(this);

        Weaver.callOriginal();
    }

}
