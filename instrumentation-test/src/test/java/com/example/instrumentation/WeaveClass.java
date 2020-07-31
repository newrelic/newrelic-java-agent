/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.example.instrumentation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.example.instrumentation.FrameworkClass")
public class WeaveClass {

    @Trace(dispatcher = true)
    public String transaction() {
        return "weaved " + Weaver.callOriginal();
    }

    public String noTransaction() {
        return "weaved " + Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public String namedTransaction() {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "newrelic",
                "is", "good");
        return "weaved " + Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public String namedTransactionSupportability() {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "newrelic",
                "is", "good");
        return "weaved " + Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public String nrApiNameTransaction() {
        NewRelic.setTransactionName("newrelic", "isCool");
        return "weaved " + Weaver.callOriginal();
    }
}
