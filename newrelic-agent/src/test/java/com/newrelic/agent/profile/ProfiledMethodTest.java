/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import org.junit.Assert;
import org.junit.Test;

public class ProfiledMethodTest {
    @Test
    public void testEquals() {
        ProfiledMethod one = ProfiledMethod.newProfiledMethod(new StackTraceElement("class", "method", null, 1));
        ProfiledMethod two = ProfiledMethod.newProfiledMethod(new StackTraceElement("class", "method", null, 1));

        Assert.assertTrue(one.equals(two));
    }

    @Test
    public void testLineNumberNotEquals() {
        ProfiledMethod one = ProfiledMethod.newProfiledMethod(new StackTraceElement("class", "method", null, 1));
        ProfiledMethod two = ProfiledMethod.newProfiledMethod(new StackTraceElement("class", "method", null, 2));

        Assert.assertFalse(one.equals(two));
    }

    @Test
    public void testClassNotEquals() {
        ProfiledMethod one = ProfiledMethod.newProfiledMethod(new StackTraceElement("class", "method", null, 1));
        ProfiledMethod two = ProfiledMethod.newProfiledMethod(new StackTraceElement("class2", "method", null, 1));

        Assert.assertFalse(one.equals(two));
    }

    @Test
    public void testMethodNotEquals() {
        ProfiledMethod one = ProfiledMethod.newProfiledMethod(new StackTraceElement("class", "method", null, 1));
        ProfiledMethod two = ProfiledMethod.newProfiledMethod(new StackTraceElement("class", "method2", null, 1));

        Assert.assertFalse(one.equals(two));
    }

}
