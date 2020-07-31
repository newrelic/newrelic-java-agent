/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.connection;

import com.caucho.vfs.WriteStream;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class AbstractHttpResponse {

    protected boolean writeHeaders(WriteStream stream, int i) {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        return Weaver.callOriginal();
    }

    public abstract int getStatusCode();

    public abstract String getContentType();

    public abstract void setHeader(String name, String value);
}
