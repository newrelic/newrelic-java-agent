/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.net;

import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "java.net.Socket")
public class Socket_Instrumentation {

    private volatile int state = Weaver.callOriginal();

    public void connect(SocketAddress endpoint, int timeout) {
        Weaver.callOriginal();
        boolean connected = Socket_Instrumentation.isConnected(state);
        boolean bound = Socket_Instrumentation.isBound(state);
        if (connected && bound && DatastoreInstanceDetection.shouldDetectConnectionAddress() && (endpoint instanceof InetSocketAddress)) {
            DatastoreInstanceDetection.saveAddress((InetSocketAddress) endpoint);
        }
    }

    private static boolean isConnected(int s) {
        return Weaver.callOriginal();
    }

    private static boolean isBound(int s) {
        return Weaver.callOriginal();
    }
}
