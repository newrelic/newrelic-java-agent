/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.net;

import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "java.net.Socket")
public class Socket_Instrumentation {

    private boolean bound;
    private boolean connected;

    public void connect(SocketAddress endpoint, int timeout) {
        Weaver.callOriginal();

        if (connected && bound && DatastoreInstanceDetection.shouldDetectConnectionAddress() && (endpoint instanceof InetSocketAddress)) {
            DatastoreInstanceDetection.saveAddress((InetSocketAddress) endpoint);
        }
    }
}
