/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package oracle.net.ns;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(originalName = "oracle.net.ns.Packet")
public class Packet_Instrumentation {
    public int type = Weaver.callOriginal();

    protected void receive() {
        Weaver.callOriginal();

        /*
         Clear stored address if we've been redirected to the real database by the Oracle TNS listener.

         http://www.thesprawl.org/research/oracle-tns-protocol/#data-packet

         (Oracle TNS Listener)
         http://www.thesprawl.org/research/oracle-default-ports/

         Type	Description
         1	    Connect
         2	    Accept
         3	    ACK
         4	    Refuse
         5	    Redirect
         6	    Data
         7	    NULL
         8      ----
         9	    ABORT
         10	    ----
         11	    Resend
         12	    Marker
         13	    Attention
         14	    Control
         */

        if (type == 5 && DatastoreInstanceDetection.shouldDetectConnectionAddress()) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "Clearing last address. Detected Oracle connection packet redirect");
            DatastoreInstanceDetection.clearAddress();
        }
    }

}
