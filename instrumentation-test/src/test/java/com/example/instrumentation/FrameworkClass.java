/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.example.instrumentation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;

public class FrameworkClass {
    public String transaction() {
        return "transaction";
    }

    public String noTransaction() {
        return "no transaction";
    }

    public String namedTransaction() {
        return "named transaction";
    }

    public String namedTransactionSupportability() {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "newrelic",
                "is", "ok");
        return "named transaction supportability";
    }

    public String nrApiNameTransaction() {
        return "named transaction";
    }
}
