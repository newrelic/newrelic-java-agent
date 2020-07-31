/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class NormalizerFactoryTest {

    @Test
    public void normalizeWithTransactionSegmentRules() {

        List<TransactionSegmentTerms> transactionSegmentTermRules = ImmutableList.of(
                new TransactionSegmentTerms("WebTransaction/Custom", ImmutableSet.of("account", "product", "list")),
                new TransactionSegmentTerms("OtherTransaction/Job", ImmutableSet.of("keep", "this")));
        Normalizer normalizer = NormalizerFactory.createTransactionSegmentNormalizer(transactionSegmentTermRules);

        Assert.assertEquals("WebTransaction/Uri/dude/test/man",
                normalizer.normalize("WebTransaction/Uri/dude/test/man"));

        Assert.assertEquals("WebTransaction/Custom/*", normalizer.normalize("WebTransaction/Custom/dude/test/man"));

        Assert.assertEquals("WebTransaction/Custom/account/*/product/list",
                normalizer.normalize("WebTransaction/Custom/account/test/product/list"));

        Assert.assertEquals("OtherTransaction/Job/*",
                normalizer.normalize("OtherTransaction/Job/account/test/product/list"));
        Assert.assertEquals("OtherTransaction/Job/keep", normalizer.normalize("OtherTransaction/Job/keep"));
        Assert.assertEquals("OtherTransaction/Job/keep/*",
                normalizer.normalize("OtherTransaction/Job/keep/one/two/three"));
        Assert.assertEquals("OtherTransaction/Job/*/keep",
                normalizer.normalize("OtherTransaction/Job/one/two/three/keep"));
    }
}
