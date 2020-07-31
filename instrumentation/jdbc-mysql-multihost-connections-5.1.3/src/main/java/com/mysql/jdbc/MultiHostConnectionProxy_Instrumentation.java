/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mysql.jdbc;

import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass, originalName = "com.mysql.jdbc.MultiHostConnectionProxy")
public class MultiHostConnectionProxy_Instrumentation {

    MySQLConnection thisAsConnection = Weaver.callOriginal();

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
