/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import java.nio.ByteBuffer;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.util.Callback;

@Weave(type = MatchType.Interface)
public abstract class HttpTransport {
    public void send(MetaData.Response info, boolean head, ByteBuffer content, boolean lastContent, Callback callback) {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        Weaver.callOriginal();
    }
}
