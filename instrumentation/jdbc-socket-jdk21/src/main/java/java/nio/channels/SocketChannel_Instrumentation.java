/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.nio.channels;

import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Weave(originalName = "java.nio.channels.SocketChannel", type = MatchType.BaseClass)
public abstract class SocketChannel_Instrumentation {

    public static SocketChannel_Instrumentation open(SocketAddress remote) {
        SocketChannel_Instrumentation channel = Weaver.callOriginal();

        if (channel.isConnected() && DatastoreInstanceDetection.shouldDetectConnectionAddress() && (remote instanceof InetSocketAddress)) {
            DatastoreInstanceDetection.saveAddress((InetSocketAddress) remote);
        }
        return channel;
    }

    public abstract boolean isConnected();
}
