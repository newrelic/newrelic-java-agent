/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.wildfly.extension.undertow;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/*
 * The CAT point cut for wildfly had to be pulled out because 
 * the jar was not getting loaded correctly.
 */
@Weave
public abstract class HttpListenerService {

    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress,
            ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException {

        AgentBridge.publicApi.setAppServerPort(socketAddress.getPort());
        Weaver.callOriginal();
    }

}
