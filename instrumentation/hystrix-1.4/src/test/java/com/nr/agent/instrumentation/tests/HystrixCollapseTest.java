/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tests;

import java.util.concurrent.Future;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.commands.CollapseCommand;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.netflix.hystrix", "com.nr.agent.instrumentation.command" })
public class HystrixCollapseTest {

    @Test
    public void testCollapse() throws Exception {
        runCommand();
        HystrixTestUtils.verifyCollapseMetrics(CollapseCommand.class.getName(), HystrixCollapseTest.class.getName());

    }

    @Trace(dispatcher = true)
    private String[] runCommand() throws Exception {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        String[] results = new String[4];
        try {
            Future<String> f1 = new CollapseCommand(1).queue();
            Future<String> f2 = new CollapseCommand(2).queue();
            Future<String> f3 = new CollapseCommand(3).queue();
            Future<String> f4 = new CollapseCommand(4).queue();

            results[0] = f1.get();
            results[1] = f2.get();
            results[2] = f3.get();
            results[3] = f4.get();
        } finally {
            context.shutdown();
        }
        return results;
    }
}
