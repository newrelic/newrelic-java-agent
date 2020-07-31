/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import com.newrelic.test.marker.ZuluIncompatibleTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Running this test with java9 requires a system property of 'jdk.attach.allowAttachSelf=true' to be set.
 */
@Category({ ZuluIncompatibleTest.class })
public class WeaverAgentTest {
    @Test
    public void testInstrumentation() {
        Assert.assertNotNull(WeaverAgent.getInstrumentation());
    }
}
