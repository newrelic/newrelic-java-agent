/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class StandardServer {

    protected void startInternal() {

        AgentBridge.privateApi.setServerInfo(getServerInfo());

        Weaver.callOriginal();

        // I don't think customers use this sampler (the UI doesn't directly reference it)
        // AgentBridge.privateApi.addSampler(new TomcatSampler(this), 5, TimeUnit.SECONDS);

    }

    public abstract String getServerInfo();

    protected abstract String getDomainInternal();
}
