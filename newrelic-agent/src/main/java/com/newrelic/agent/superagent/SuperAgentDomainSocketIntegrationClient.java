/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.superagent.protos.AgentToServer;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

public class SuperAgentDomainSocketIntegrationClient implements SuperAgentIntegrationClient {
    private final File socketAddress;

    public SuperAgentDomainSocketIntegrationClient(String socketAddress) {
        this.socketAddress = new File(socketAddress);
    }

    public void sendAgentToServerMessage(AgentToServer agentToServerMessage) {
        Agent.LOG.log(Level.FINEST, "Sending AgentToServer message to domain socket address: {0}\n{1}", this.socketAddress.getAbsolutePath(), agentToServerMessage.toString());

        try (AFUNIXSocket socket = AFUNIXSocket.connectTo(AFUNIXSocketAddress.of(this.socketAddress));
             OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write(agentToServerMessage.toByteArray());
            outputStream.flush();
        } catch (IOException se) {
            Agent.LOG.log(Level.WARNING, "Exception attempting to connect to UnixSocketAddress {0}: - {1}", this.socketAddress.getAbsolutePath(), se.getMessage());
        }

    }
}
