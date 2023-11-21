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

    // There are some static methods that could be used instead of these constants.
    // But to use those methods, we'd add some intrumentation to them, adding to the overhead
    // of something that should be really fast.
    private static final int BOUND = Weaver.callOriginal();
    private static final int CONNECTED = Weaver.callOriginal();
    private volatile int state = Weaver.callOriginal();

    public void connect(SocketAddress endpoint, int timeout) {
        Weaver.callOriginal();
        boolean connected = (state & CONNECTED) != 0;
        boolean bound = (state & BOUND) != 0;
        if (connected && bound && DatastoreInstanceDetection.shouldDetectConnectionAddress() && (endpoint instanceof InetSocketAddress)) {
            DatastoreInstanceDetection.saveAddress((InetSocketAddress) endpoint);
        }
    }
}
