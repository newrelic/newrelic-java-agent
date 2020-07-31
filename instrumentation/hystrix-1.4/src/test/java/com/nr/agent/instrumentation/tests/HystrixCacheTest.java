/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.commands.CommandUsingRequestCache;
import com.nr.agent.instrumentation.tests.HystrixTestUtils.CallType;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.netflix.hystrix", "com.nr.agent.instrumentation.command" })
public class HystrixCacheTest {

    @Test
    public void testCache() {
        runCommand();
        HystrixTestUtils.verifyHystrixCacheMetrics(CommandUsingRequestCache.class.getName(), CallType.EXECUTE, 5, 2,
                HystrixCacheTest.class.getName());
    }

    @Trace(dispatcher = true)
    private void runCommand() {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            CommandUsingRequestCache command2a = new CommandUsingRequestCache(2);
            CommandUsingRequestCache command2b = new CommandUsingRequestCache(2);
            CommandUsingRequestCache command2c = new CommandUsingRequestCache(2);
            CommandUsingRequestCache command8a = new CommandUsingRequestCache(8);
            CommandUsingRequestCache command9a = new CommandUsingRequestCache(9);

            Assert.assertTrue(command2a.execute());
            Assert.assertTrue(command2b.execute());
            Assert.assertTrue(command2b.isResponseFromCache());
            Assert.assertTrue(command2c.execute());
            Assert.assertTrue(command2c.isResponseFromCache());
            Assert.assertTrue(command8a.execute());
            Assert.assertFalse(command8a.isResponseFromCache());
            Assert.assertFalse(command9a.execute());
            Assert.assertFalse(command9a.isResponseFromCache());
        } finally {
            context.shutdown();
        }
    }
}
