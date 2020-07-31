/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import org.junit.Assert;
import org.junit.Test;

/**
 * We only test the regular expression matching here. The visitor itself is much more work to test, and it's covered
 * indirectly by other tests.
 */
public class GeneratedClassDetectorTest {

    @Test
    public void patternTest() {
        // Real examples
        Assert.assertTrue(GeneratedClassDetector.isGenerated("javax/jms/MessageListener$$$view74"));

        // Synthetic examples
        Assert.assertTrue(GeneratedClassDetector.isGenerated("package/foo/bar/EnhancerByCGLIB$7a34cd2365f7"));
        Assert.assertTrue(GeneratedClassDetector.isGenerated("package/foo/bar/cglib$proxy_7a34cd2365f7"));
        Assert.assertTrue(GeneratedClassDetector.isGenerated("package/foo/bar/EnhancerByCgLiB$7a34cd2365f7"));
        Assert.assertTrue(GeneratedClassDetector.isGenerated("package/foo/bar/CGLib$proxy_7a34cd2365f7"));
        Assert.assertTrue(GeneratedClassDetector.isGenerated("package/foo/bar/CGLib$proxy_7a34cd2365f7"));

        Assert.assertFalse(GeneratedClassDetector.isGenerated("javax/jms/MessageListener$view74"));
        Assert.assertFalse(GeneratedClassDetector.isGenerated("java/lang/System"));
        Assert.assertFalse(GeneratedClassDetector.isGenerated("java/util/concurrent/atomic/AtomicCafe"));
    }
}
