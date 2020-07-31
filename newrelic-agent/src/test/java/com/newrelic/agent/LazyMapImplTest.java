/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

public class LazyMapImplTest {

    /**
     * Regression for JAVA-932.
     */
    @Test
    public void testBasic() {
        MapMaker factory = new MapMaker().initialCapacity(8).concurrencyLevel(4);
        Set<Long> testSet = Sets.newSetFromMap(new LazyMapImpl<Long, Boolean>(factory));
        Assert.assertNotNull(testSet);

        Long[] actuals = new Long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L };
        List<Long> actualsList = Arrays.asList(actuals);
        testSet.addAll(actualsList);

        for (Long actualBoxedLong : actualsList) {
            Assert.assertTrue(testSet.contains(actualBoxedLong));
        }
        for (long iteratedUnboxedLong = 1; iteratedUnboxedLong < 10; ++iteratedUnboxedLong) {
            Assert.assertTrue(testSet.contains(iteratedUnboxedLong));
        }

        int n = testSet.size();
        Assert.assertEquals(n, 9);
        testSet.remove(1L);
        Assert.assertTrue(testSet.size() == n - 1);

        if (!testSet.removeAll(actualsList)) {
            Assert.fail("remove all didn't do anything");
        }
        Assert.assertTrue(testSet.isEmpty());
    }
}
