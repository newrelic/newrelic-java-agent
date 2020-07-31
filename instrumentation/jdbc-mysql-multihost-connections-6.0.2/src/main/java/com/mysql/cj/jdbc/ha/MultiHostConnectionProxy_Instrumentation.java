/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mysql.cj.jdbc.ha;

import com.mysql.cj.api.jdbc.JdbcConnection;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass, originalName = "com.mysql.cj.jdbc.ha.MultiHostConnectionProxy")
public class MultiHostConnectionProxy_Instrumentation {

    JdbcConnection thisAsConnection = Weaver.callOriginal();

    void pickNewConnection() {
        boolean firstInConnectPath = !DatastoreInstanceDetection.shouldDetectConnectionAddress();
        try {
            DatastoreInstanceDetection.detectConnectionAddress();
            Weaver.callOriginal();
            DatastoreInstanceDetection.associateAddress(thisAsConnection);
        } finally {
            if (firstInConnectPath) {
                DatastoreInstanceDetection.stopDetectingConnectionAddress();
            }
        }
    }

}
