/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import org.junit.Assert;
import org.junit.Test;

public class FixedSizeSetTest {
    @Test
    public void testLimit() {
        FixedSizeSet<String> set = new FixedSizeSet<>(20);

        for (int i = 0; i < 100; i++) {
            set.add(Integer.toString(i));
        }

        Assert.assertEquals(20, set.size());
    }
}
