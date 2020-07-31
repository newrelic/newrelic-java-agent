/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import org.json.simple.JSONObject;

public class DatastoreInstanceCrossAgentTest {

    final Integer port;
    final String product;
    final String dbHostname;
    final String systemHostname;
    final String dbPath;
    final String unixSocket;
    final String expectedInstanceMetric;
    final String testName;

    public DatastoreInstanceCrossAgentTest(JSONObject testSpecification) {
        Object port = testSpecification.get("port");
        if (port == null) {
            this.port = null;
        } else if (port instanceof String) {
            if (((String) port).isEmpty()) {
                this.port = null;
            }
            else  {
                this.port = null;
            }
        } else {
            this.port = ((Long) testSpecification.get("port")).intValue();
        }

        product = (String) testSpecification.get("product");
        dbHostname = (String) testSpecification.get("db_hostname");
        systemHostname = (String) testSpecification.get("system_hostname");
        expectedInstanceMetric = (String) testSpecification.get("expected_instance_metric");
        dbPath = (String) testSpecification.get("database_path");
        unixSocket = (String) testSpecification.get("unix_socket");
        testName = (String) testSpecification.get("name");
    }

    public Integer getPort() {
        return port;
    }

    public String getProduct() {
        return product;
    }

    public String getDbHostname() {
        return dbHostname;
    }

    public String getSystemHostname() {
        return systemHostname;
    }

    public String getExpectedInstanceMetric() {
        return expectedInstanceMetric;
    }

    public String getTestName() {
        return testName;
    }

    public String getDbPath() {
        return dbPath;
    }

    public String getUnixSocket() {
        return unixSocket;
    }

}
