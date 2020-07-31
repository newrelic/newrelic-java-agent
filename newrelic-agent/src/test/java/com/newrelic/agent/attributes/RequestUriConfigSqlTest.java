/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import org.json.simple.JSONObject;

public class RequestUriConfigSqlTest extends RequestUriConfigTest {

    public final String txnName;
    public final String sql;

    public RequestUriConfigSqlTest(JSONObject testSpecification) {
        super(testSpecification);

        txnName = (String) input.get("txn_name");
        sql = (String) input.get("sql");
    }

    public String getTxnName() {
        return txnName;
    }

    public String getSql() {
        return sql;
    }
}
