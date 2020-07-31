/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.sql;

import java.util.Properties;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.DatastoreInstanceDetection;
import com.newrelic.agent.bridge.datastore.JdbcDriverConnectionFactory;
import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "java.sql.Driver", type = MatchType.Interface)
public abstract class Driver_Weaved {

    public Connection connect(String url, Properties props) {
        boolean firstInConnectPath = !DatastoreInstanceDetection.shouldDetectConnectionAddress();
        try {
            DatastoreInstanceDetection.detectConnectionAddress();
            Connection connection = Weaver.callOriginal();
            DatastoreInstanceDetection.associateAddress(connection);

            if (!JdbcHelper.connectionFactoryExists(connection)) {
                String detectedUrl = JdbcHelper.getConnectionURL(connection);
                if (detectedUrl == null) {
                    return connection;
                }

                // Detect correct vendor type and then store new connection factory based on URL
                DatabaseVendor vendor = JdbcHelper.getVendor(getClass(), detectedUrl);
                JdbcHelper.putConnectionFactory(detectedUrl, new JdbcDriverConnectionFactory(vendor, (Driver) this,
                        url, props));
            }

            return connection;
        } finally {
            if (firstInConnectPath) {
                DatastoreInstanceDetection.stopDetectingConnectionAddress();
            }
        }
    }

}
