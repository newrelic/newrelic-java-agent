/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.connector;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.apache.catalina.connector.Connector")
public abstract class Connector_Instrumentation {

    public void start() {

        AgentBridge.publicApi.setAppServerPort(getPort());
        Weaver.callOriginal();
    }

    public abstract int getPort();
}
