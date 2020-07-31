/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import org.json.simple.JSONObject;

public class RequestUriConfigTransactionTest extends RequestUriConfigTest {

    public final String txnName;

    public RequestUriConfigTransactionTest(JSONObject testSpecification) {
        super(testSpecification);

        txnName = (String) input.get("txn_name");
    }

    public String getTxnName() {
        return txnName;
    }
}
