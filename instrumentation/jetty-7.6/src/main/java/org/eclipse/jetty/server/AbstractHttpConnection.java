/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import java.io.IOException;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class AbstractHttpConnection {

    public void commitResponse(boolean last) throws IOException {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        Weaver.callOriginal();
    }

    public abstract Request getRequest();
}
