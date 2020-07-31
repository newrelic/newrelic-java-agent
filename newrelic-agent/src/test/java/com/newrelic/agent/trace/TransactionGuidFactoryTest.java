/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class TransactionGuidFactoryTest {

    @Test
    public void testGetGuid() {
        String guidString = TransactionGuidFactory.generate16CharGuid();

        Assert.assertTrue(guidString.length() > 0);
        for (int i = 0; i < guidString.length(); i++) {
            char c = guidString.charAt(i);
            // verify the string is a hexidecimal value
            Assert.assertTrue((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
        }
    }

    /**
     * Generate a bunch of strings and verify they're all different. Of course, this test is nondeterministic. But it's
     * the same nondeterminism we're subjecting our customers to - some of our code, such as the CAT code, assumes the
     * transactions GUIDs are unique. So if we ever really see a failure here, we should consider changing the algorithm
     * to generate unique IDs that are more unique (e.g. real GUIDs).
     */
    @Test
    public void testGuidUnique() {
        final Set<String> someGuids = new HashSet<>();

        for (int i = 0; i < 1000; ++i) {
            String guidString = TransactionGuidFactory.generate16CharGuid();
            if (someGuids.contains(guidString)) {
                Assert.fail("Non-unique identifier: " + guidString);
            }
            someGuids.add(guidString);
        }
    }
}
