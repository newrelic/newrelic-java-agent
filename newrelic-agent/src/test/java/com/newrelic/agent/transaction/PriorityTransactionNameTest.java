/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.bridge.TransactionNamePriority;

public class PriorityTransactionNameTest {

    @Test
    public void createTransactionName() {
        String expectedName = "MyTransactionName";
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName txName = PriorityTransactionName.create(expectedName, null, expectedPriority);
        Assert.assertEquals(expectedPriority, txName.getPriority());
        Assert.assertEquals(expectedName, txName.getName());
    }

    @Test
    public void category() {
        String expectedName = "MyTransactionName";
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName txName = PriorityTransactionName.create(expectedName, "MyCategory", expectedPriority);
        Assert.assertEquals("MyCategory", txName.getCategory());
    }

    @Test
    public void equals() {
        PriorityTransactionName ptn = PriorityTransactionName.create("MyTransactionName", null,
                TransactionNamePriority.FILTER_INIT_PARAM);
        Assert.assertTrue(ptn.equals(ptn));

        PriorityTransactionName ptn2 = PriorityTransactionName.create("MyTransactionName", null,
                TransactionNamePriority.FILTER_NAME);
        Assert.assertFalse(ptn.equals(ptn2));

        PriorityTransactionName ptn3 = PriorityTransactionName.create("MyOtherTransactionName", null,
                TransactionNamePriority.FILTER_INIT_PARAM);
        Assert.assertFalse(ptn.equals(ptn3));

        Assert.assertFalse(ptn.equals(null));
        Assert.assertFalse(ptn.equals(TransactionNamePriority.NONE));
        Assert.assertFalse(PriorityTransactionName.NONE.equals(ptn));
        Assert.assertTrue(PriorityTransactionName.NONE.equals(PriorityTransactionName.NONE));
    }

}
