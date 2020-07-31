/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.newrelic.api.agent.NewRelic;
import org.eclipse.jetty.http.HttpGenerator.ResponseInfo;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class HttpChannel {

    protected boolean commitResponse(ResponseInfo info, ByteBuffer content, boolean complete) throws IOException {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        return Weaver.callOriginal();
    }

    public abstract Request getRequest();
}
