/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.javax;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

public class RestInterfaceTest {

    private Customer example = new CustomerImpl();

    @Trace(dispatcher = true)
    @Test
    public void create() {
        example.create("", "", "");

        Assert.assertEquals("OtherTransaction/RestWebService/customer/create (PUT)",
                Transaction.getTransaction().getPriorityTransactionName().getName());
    }

}
