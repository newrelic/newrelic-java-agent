/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.net;

import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(originalName = "java.net.Socket")
public class Socket_Instrumentation {

    private boolean bound;
    private boolean connected;

    public void connect(SocketAddress endpoint, int timeout) {
        Weaver.callOriginal();
        NewRelic.getAgent().getLogger().log(Level.INFO, "T4C DEBUG: Calling connect in Socket with endpoint {0}", endpoint);
        if (connected && bound && DatastoreInstanceDetection.shouldDetectConnectionAddress() && (endpoint instanceof InetSocketAddress)) {
            DatastoreInstanceDetection.saveAddress((InetSocketAddress) endpoint);
        }
    }
}
