/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package oracle.jdbc.driver;

import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.api.agent.weaver.Weave;
import oracle.jdbc.pool.OraclePooledConnection;

import java.net.InetSocketAddress;

@Weave(originalName = "oracle.jdbc.driver.LogicalConnection")
public abstract class LogicalConnection_Instrumentation extends OracleConnection {

    protected LogicalConnection_Instrumentation(OraclePooledConnection pooledConnection,
            oracle.jdbc.internal.OracleConnection conn, boolean var3)  {
        final InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(conn);
        if (address != null) {
            DatastoreInstanceDetection.detectConnectionAddress();
            DatastoreInstanceDetection.associateAddress(this, address);
            DatastoreInstanceDetection.stopDetectingConnectionAddress();
        }
    }
}
