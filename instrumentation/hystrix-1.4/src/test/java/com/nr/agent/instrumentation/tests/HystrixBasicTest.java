/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.nr.agent.instrumentation.commands.CommandUsingSemaphoreIsolation;
import com.nr.agent.instrumentation.commands.CountCommand;
import com.nr.agent.instrumentation.commands.FailureNoFailureCommand;
import com.nr.agent.instrumentation.commands.FailureNoTimeoutCommand;
import com.nr.agent.instrumentation.commands.HelloWorldCommand;
import com.nr.agent.instrumentation.commands.SleepCommand;
import com.nr.agent.instrumentation.commands.TimeoutCommand;
import com.nr.agent.instrumentation.tests.HystrixTestUtils.CallType;
import com.nr.agent.instrumentation.tests.HystrixTestUtils.Failure;
import com.nr.agent.instrumentation.tests.HystrixTestUtils.TimeEvaluation;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.netflix.hystrix", "com.nr.agent.instrumentation.command" })
public class HystrixBasicTest {

    private HystrixRequestContext context;

    @Before
    public void init() {
        context = HystrixRequestContext.initializeContext();
    }

    @After
    public void shutdown() {
        context.shutdown();
    }

    @Test
    public void testHelloWorldExecute() throws Exception {
        HystrixCommand<String> command = new HelloWorldCommand();
        CallType type = CallType.EXECUTE;
        String commandName = HelloWorldCommand.class.getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("Hello World", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
        HystrixTestUtils.verifyEvent(HystrixTestUtils.class.getName(), TimeEvaluation.TOTAL_TIME_GREATER);
    }

    @Test
    public void testSleepExecute() throws Exception {
        SleepCommand command = new SleepCommand(1);
        CallType type = CallType.EXECUTE;
        String commandName = SleepCommand.class.getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("SleepCommand: Slept for 1", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
        HystrixTestUtils.verifyEvent(HystrixTestUtils.class.getName(), TimeEvaluation.TOTAL_TIME_GREATER);
    }

    @Test
    public void testCountExecute() throws Exception {
        CountCommand command = new CountCommand();
        CallType type = CallType.EXECUTE;
        String commandName = CountCommand.class.getName();
        Integer result = (Integer) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals(5, result.intValue());
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
        HystrixTestUtils.verifyEvent(HystrixTestUtils.class.getName(), TimeEvaluation.TOTAL_TIME_GREATER);

    }

    @Test
    public void testFailureExecute() throws Exception {
        FailureNoTimeoutCommand command = new FailureNoTimeoutCommand();
        CallType type = CallType.EXECUTE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.EXECUTE);
        Assert.assertEquals("Failure: Failurecommand.", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, true);
        HystrixTestUtils.verifyTransactionTraceFailure(commandName, type, Failure.EXCEPTION);
        HystrixTestUtils.verifyEvent(HystrixTestUtils.class.getName(), TimeEvaluation.TOTAL_TIME_GREATER);
    }

    @Test
    public void testExceptionNoFailureExecute() throws Exception {
        FailureNoFailureCommand command = new FailureNoFailureCommand();
        CallType type = CallType.EXECUTE;
        String commandName = command.getClass().getName();
        try {
            HystrixTestUtils.runCommand(command, type);
            Assert.fail("Exception should have been thrown");
        } catch (HystrixRuntimeException e) {
            // should have failed
        }
        HystrixTestUtils.verifyHystrixMetricsOneCommand(FailureNoFailureCommand.class.getName(), type, false);
        // while it throws an exception, the failback is not called because it is disabled and so the tt is the same as
        // the sucessful trace
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    @Ignore("Ignored temporarily due to flicker failure")
    public void testTimeoutExecute() throws Exception {
        TimeoutCommand timeoutCommand = new TimeoutCommand();
        CallType type = CallType.EXECUTE;
        String commandName = timeoutCommand.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(timeoutCommand, type);
        Assert.assertEquals(TimeoutCommand.SUCCESS, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, true);
        HystrixTestUtils.verifyTransactionTraceFailure(commandName, type, Failure.TIMEOUT);
    }

    @Test
    public void testHelloWorldQueue() throws Exception {
        HystrixCommand<String> helloWorldCommand = new HelloWorldCommand();
        CallType type = CallType.QUEUE;
        String commandName = helloWorldCommand.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(helloWorldCommand, type);
        Assert.assertEquals("Hello World", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    public void testSleepQueue() throws Exception {
        SleepCommand command = new SleepCommand(1);
        CallType type = CallType.QUEUE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("SleepCommand: Slept for 1", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    public void testCountQueue() throws Exception {
        CountCommand command = new CountCommand();
        CallType type = CallType.QUEUE;
        String commandName = command.getClass().getName();
        Integer result = (Integer) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals(5, result.intValue());
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    public void testFailureQueue() throws Exception {
        FailureNoTimeoutCommand command = new FailureNoTimeoutCommand();
        CallType type = CallType.QUEUE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("Failure: Failurecommand.", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, HystrixTestUtils.CallType.QUEUE, true);
        HystrixTestUtils.verifyTransactionTraceFailure(commandName, type, Failure.EXCEPTION);
    }

    @Test
    public void testTimeoutQueue() throws Exception {
        TimeoutCommand timeoutCommand = new TimeoutCommand();
        CallType type = CallType.QUEUE;
        String commandName = timeoutCommand.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(timeoutCommand, type);
        Assert.assertEquals(TimeoutCommand.SUCCESS, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, true);
        HystrixTestUtils.verifyTransactionTraceFailure(commandName, type, Failure.TIMEOUT);
    }

    @Test
    public void testHelloWorldToObservable() throws Exception {
        HystrixCommand<String> helloWorldCommand = new HelloWorldCommand();
        CallType type = CallType.TO_OBSERVABLE;
        String commandName = helloWorldCommand.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(helloWorldCommand, type);
        Assert.assertEquals("Hello World", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    public void testSleepToObservable() throws Exception {
        SleepCommand command = new SleepCommand(1);
        CallType type = CallType.TO_OBSERVABLE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("SleepCommand: Slept for 1", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    public void testCountToObservable() throws Exception {
        CountCommand command = new CountCommand();
        CallType type = CallType.TO_OBSERVABLE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("5", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    public void testFailureToObservable() throws Exception {
        FailureNoTimeoutCommand command = new FailureNoTimeoutCommand();
        CallType type = CallType.TO_OBSERVABLE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("Failure: Failurecommand.", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, true);
        HystrixTestUtils.verifyTransactionTraceFailure(commandName, type, Failure.EXCEPTION);
    }

    @Test
    public void testTimeoutToObservable() throws Exception {
        TimeoutCommand timeoutCommand = new TimeoutCommand();
        CallType type = CallType.TO_OBSERVABLE;
        String commandName = timeoutCommand.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(timeoutCommand, type);
        Assert.assertEquals(TimeoutCommand.SUCCESS, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, true);
        HystrixTestUtils.verifyTransactionTraceFailure(commandName, type, Failure.TIMEOUT);
    }

    @Test
    public void testCountObserve() throws Exception {
        CountCommand command = new CountCommand();
        String result = (String) HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.OBSERVE);
        Assert.assertEquals("5", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(CountCommand.class.getName(),
                HystrixTestUtils.CallType.OBSERVE, false);
    }

    @Test
    public void testFailureObserve() throws Exception {
        FailureNoTimeoutCommand command = new FailureNoTimeoutCommand();
        String result = (String) HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.OBSERVE);
        Assert.assertEquals("Failure: Failurecommand.", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(FailureNoTimeoutCommand.class.getName(),
                HystrixTestUtils.CallType.OBSERVE, true);
    }

    @Test
    public void testTimeoutObserve() throws Exception {
        TimeoutCommand timeoutCommand = new TimeoutCommand();
        String result = (String) HystrixTestUtils.runCommand(timeoutCommand, HystrixTestUtils.CallType.OBSERVE);
        Assert.assertEquals(TimeoutCommand.SUCCESS, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(TimeoutCommand.class.getName(),
                HystrixTestUtils.CallType.OBSERVE, true);
    }

    @Test
    public void testSemaphoreExecute() throws Exception {
        int id = 5;
        HystrixCommand<String> command = new CommandUsingSemaphoreIsolation(id);
        String result = (String) HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.EXECUTE);
        Assert.assertEquals("ValueFromHashMap_" + id, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(CommandUsingSemaphoreIsolation.class.getName(),
                HystrixTestUtils.CallType.EXECUTE, false);
    }

    @Test
    public void testSemaphoreQueue() throws Exception {
        int id = 5;
        HystrixCommand<String> command = new CommandUsingSemaphoreIsolation(id);
        String result = (String) HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.QUEUE);
        Assert.assertEquals("ValueFromHashMap_" + id, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(CommandUsingSemaphoreIsolation.class.getName(),
                HystrixTestUtils.CallType.QUEUE, false);
    }

    @Test
    public void testSemaphoreObserve() throws Exception {
        int id = 21;
        HystrixCommand<String> command = new CommandUsingSemaphoreIsolation(id);
        String result = (String) HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.OBSERVE);
        Assert.assertEquals("ValueFromHashMap_" + id, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(CommandUsingSemaphoreIsolation.class.getName(),
                HystrixTestUtils.CallType.OBSERVE, false);
    }

    @Test
    public void testSemaphoreToObservable() throws Exception {
        int id = 9;
        HystrixCommand<String> command = new CommandUsingSemaphoreIsolation(id);
        String result = (String) HystrixTestUtils.runCommand(command, HystrixTestUtils.CallType.TO_OBSERVABLE);
        Assert.assertEquals("ValueFromHashMap_" + id, result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(CommandUsingSemaphoreIsolation.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE, false);
    }

}
