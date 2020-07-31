/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.net;

import java.io.IOException;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.external.ExternalParametersFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
class SocketInputStream {
    
    private Socket socket;
    
    @Trace(leaf=true)
    int read(byte b[], int off, int length, int timeout) throws IOException {
        
        String hostName = socket.getInetAddress().getHostName();
        try {
            URI uri = new URI("socket", hostName, "", "");
            AgentBridge.getAgent().getTracedMethod().reportAsExternal(ExternalParametersFactory.createForGenericExternal("SocketInputStream",
                    uri, "read"));
        } catch (URISyntaxException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, e.getMessage());
        }
        
        return Weaver.callOriginal();
    }
}
