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

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.commands.CommandCallsCommand;
import com.nr.agent.instrumentation.commands.SleepDecrementCommand;
import com.nr.agent.instrumentation.tests.HystrixTestUtils.CallType;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.netflix.hystrix", "com.nr.agent.instrumentation.command" })
public class HystrixMultipleTest {

    @Test
    public void testMultipleCommandsExecute() throws Exception {
        int count = 3;
        runCommand(HystrixTestUtils.CallType.EXECUTE, count);
        HystrixTestUtils.verifyHystrixMetrics(SleepDecrementCommand.class.getName(), HystrixTestUtils.CallType.EXECUTE,
                false, count, HystrixMultipleTest.class.getName());
    }

    @Test
    public void testMultipleCommandsQueue() throws Exception {
        int count = 4;
        runCommand(HystrixTestUtils.CallType.QUEUE, count);
        HystrixTestUtils.verifyHystrixMetrics(SleepDecrementCommand.class.getName(), HystrixTestUtils.CallType.QUEUE,
                false, count, HystrixMultipleTest.class.getName());
    }

    @Test
    public void testMultipleCommandsToObservable() throws Exception {
        int count = 2;
        runCommand(HystrixTestUtils.CallType.TO_OBSERVABLE, count);
        HystrixTestUtils.verifyHystrixMetrics(SleepDecrementCommand.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE, false, count, HystrixMultipleTest.class.getName());
    }

    @Trace(dispatcher = true)
    private void runCommand(HystrixTestUtils.CallType type, int count) throws Exception {
        Integer result = count;
        SleepDecrementCommand command;
        while (result > 0) {
            command = new SleepDecrementCommand(result);
            int previous = result;
            if (type == CallType.EXECUTE || type == CallType.QUEUE) {
                result = (Integer) HystrixTestUtils.runCommand(command, type);
            } else {
                result = Integer.valueOf((String) HystrixTestUtils.runCommand(command, type));

            }
            Assert.assertEquals(previous - 1, result.intValue());
        }
    }

    @Test
    public void testCommandCallCommandExecute() throws Exception {
        int count = 3;
        CommandCallsCommand command = new CommandCallsCommand(count);
        HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.EXECUTE);
        HystrixTestUtils.verifyHystrixMetrics(CommandCallsCommand.class.getName(), HystrixTestUtils.CallType.EXECUTE,
                false, count, HystrixTestUtils.class.getName());
    }

    @Test
    public void testCommandCallCommandQueue() throws Exception {
        int count = 5;
        CommandCallsCommand command = new CommandCallsCommand(count);
        HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.QUEUE);
        HystrixTestUtils.verifyHystrixMetrics(CommandCallsCommand.class.getName(), HystrixTestUtils.CallType.QUEUE,
                false, count, HystrixTestUtils.class.getName());
    }

    @Test
    public void testCommandCallCommandToObservable() throws Exception {
        int count = 3;
        CommandCallsCommand command = new CommandCallsCommand(count);
        HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.TO_OBSERVABLE);
        HystrixTestUtils.verifyHystrixMetrics(CommandCallsCommand.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE, false, count, HystrixTestUtils.class.getName());
    }

}
