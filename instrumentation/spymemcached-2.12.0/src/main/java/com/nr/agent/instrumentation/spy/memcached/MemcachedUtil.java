/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import com.newrelic.agent.bridge.AgentBridge;
import net.spy.memcached.MemcachedNode;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class MemcachedUtil {

    // This is used to store the node that a given operation executes on for datastore reporting purposes
    public static final ThreadLocal<MemcachedNode> OPERATION_NODE = new ThreadLocal<MemcachedNode>() {

    };

    /**
     * Returns the address of the node captured for the current operation and clears it, or null if unavailable.
     */
    public static InetSocketAddress getOperationNodeAddress() {
        try {
            MemcachedNode memcachedNode = OPERATION_NODE.get();
            if (memcachedNode != null) {
                SocketAddress address = memcachedNode.getSocketAddress();
                if (address instanceof InetSocketAddress) {
                    return (InetSocketAddress) address;
                }
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to capture host/port for memcached operation");
        } finally {
            OPERATION_NODE.set(null);
        }
        return null;
    }

    /**
     * Maps each address the client was configured with to the host string it was configured with.
     *
     * Some clients (e.g. the AWS ElastiCache Cluster Client used for ElastiCache Serverless over TLS) rebuild node
     * addresses from the resolved IP alone, losing the configured hostname. InetSocketAddress equality on resolved
     * addresses only compares IP and port, so this map lets an IP-only node address be traced back to the configured
     * endpoint hostname.
     */
    public static Map<InetSocketAddress, String> getConfiguredHosts(List<InetSocketAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<InetSocketAddress, String> configuredHosts = new HashMap<InetSocketAddress, String>();
        for (InetSocketAddress address : addresses) {
            if (address != null) {
                configuredHosts.put(address, address.getHostString());
            }
        }
        return configuredHosts;
    }

    /**
     * Returns the configured host for the node address if known, otherwise the node's host string.
     *
     * getHostString() is used instead of getHostName() because getHostName() can trigger a blocking reverse DNS
     * lookup on the application thread and report an unrelated hostname.
     */
    public static String getHost(InetSocketAddress nodeAddress, Map<InetSocketAddress, String> configuredHosts) {
        if (nodeAddress == null) {
            return null;
        }
        if (configuredHosts != null) {
            String configuredHost = configuredHosts.get(nodeAddress);
            if (configuredHost != null) {
                return configuredHost;
            }
        }
        return nodeAddress.getHostString();
    }

}
