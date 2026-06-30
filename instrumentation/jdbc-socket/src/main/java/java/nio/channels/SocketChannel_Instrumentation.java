/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.nio.channels;

import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;

@Weave(originalName = "java.nio.channels.SocketChannel", type = MatchType.BaseClass)
public abstract class SocketChannel_Instrumentation {

    @NewField
    InetSocketAddress address;

    public static SocketChannel_Instrumentation open(SocketAddress remote) {
        SocketChannel_Instrumentation channel = Weaver.callOriginal();
        if (channel.isConnected() && DatastoreInstanceDetection.shouldDetectConnectionAddress() && (remote instanceof InetSocketAddress)) {
            DatastoreInstanceDetection.saveAddress((InetSocketAddress) remote);
        }
        return channel;
    }

    public boolean connect(SocketAddress remote) throws IOException {
        boolean result = Weaver.callOriginal();
        if (DatastoreInstanceDetection.shouldDetectConnectionAddress() && (remote instanceof InetSocketAddress)) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "JDBC Connection Debug: method=connect, storing address {0}", remote);
            this.address = (InetSocketAddress) remote;
        }
        return result;
    }

    public boolean finishConnect() {
        boolean result = Weaver.callOriginal();
        if (isConnected() && DatastoreInstanceDetection.shouldDetectConnectionAddress() && this.address != null) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "JDBC Connection Debug: method=finishConnect, saving address {0}", this.address);
            DatastoreInstanceDetection.saveAddress(this.address);
        }
        return result;
    }

    public abstract boolean isConnected();
}
