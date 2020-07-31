/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.tcpchannel.internal;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.Map;
import java.util.logging.Level;

@Weave
public class TCPChannelFactory {
    @NewField
    private static final String DEFAULT_HTTP_ENDPOINT = "defaultHttpEndpoint";

    protected Channel createChannel(final ChannelData channelData) throws ChannelException {
        if (channelData.isInbound() && DEFAULT_HTTP_ENDPOINT.equals(channelData.getExternalName())) {
            Map<Object, Object> propertyBag = channelData.getPropertyBag();
            if (propertyBag.containsKey("port")) {
                try {
                    int port = Integer.valueOf((String) propertyBag.get("port"));
                    AgentBridge.publicApi.setAppServerPort(port);
                } catch (NumberFormatException e) {
                    AgentBridge.getAgent().getLogger().log(Level.SEVERE,
                            "could not find server port for WebSphere Liberty Profile. Found {0}",
                            propertyBag.get("port"));
                }
            } else {
                AgentBridge.getAgent().getLogger().log(Level.FINE,
                        "could not find server port for WebSphere Liberty Profile.");
            }
        } else {
            AgentBridge.getAgent().getLogger().log(Level.FINE,
                    "could not find server port for WebSphere Liberty Profile.");
        }
        return Weaver.callOriginal();
    }
}
