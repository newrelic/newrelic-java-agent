/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import org.junit.Assert;
import org.junit.Test;

public class ClassMethodSignaturesTest {

    @Test
    public void testGrow() {
        ClassMethodSignatures sigs = new ClassMethodSignatures(10);
        for (int i = 0; i < 12; i++) {
            Assert.assertEquals(i, sigs.add(new ClassMethodSignature("one", Integer.toString(i), "")));
        }

        for (int i = 0; i < 12; i++) {
            Assert.assertEquals(new ClassMethodSignature("one", Integer.toString(i), ""), sigs.get(i));
        }
    }

}
