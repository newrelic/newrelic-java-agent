/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.cluster;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class ClusterServer {
    public void setPort(int port) {

        AgentBridge.publicApi.setAppServerPort(port);

        Weaver.callOriginal();
    }
}
