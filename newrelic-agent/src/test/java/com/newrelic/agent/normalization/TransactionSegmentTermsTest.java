/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class TransactionSegmentTermsTest {

    @Test
    public void test() {
        TransactionSegmentTerms terms = new TransactionSegmentTerms("WebTransactions/Uri/",
                Collections.<String> emptySet());
        Assert.assertEquals("WebTransactions/Uri", terms.prefix);

    }
}
