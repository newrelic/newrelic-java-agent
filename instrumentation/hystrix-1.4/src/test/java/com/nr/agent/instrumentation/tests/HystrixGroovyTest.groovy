/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext
import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.nr.agent.instrumentation.commands.HelloWorldCommand
import com.nr.agent.instrumentation.commands.NonGenericSleepCommand
import com.nr.agent.instrumentation.tests.HystrixTestUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test exists solely to reproduce an issue in the weaver where a base class match that uses generics
 * (HystrixCommand) that tries to weave a second level class without generics throws exceptions in Groovy.
 * 
 * For an example see the {@link NonGenericSleepCommand}
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = [ "com.netflix.hystrix", "com.nr.agent.instrumentation.command" ])
public class HystrixGroovyTest {

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
    public void testNonGenericSleepExecute() throws Exception {
        NonGenericSleepCommand command = new NonGenericSleepCommand(1);
        HystrixTestUtils.CallType type = HystrixTestUtils.CallType.EXECUTE;
        String commandName = NonGenericSleepCommand.class.getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("GenericSleepCommand: Slept for 1", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
        HystrixTestUtils.verifyEvent(HystrixTestUtils.class.getName(), HystrixTestUtils.TimeEvaluation.TOTAL_TIME_GREATER);
    }

    @Test
    public void testNonGenericSleepQueue() throws Exception {
        NonGenericSleepCommand command = new NonGenericSleepCommand(1);
        HystrixTestUtils.CallType type = HystrixTestUtils.CallType.QUEUE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("GenericSleepCommand: Slept for 1", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    @Test
    public void testNonGenericSleepToObservable() throws Exception {
        NonGenericSleepCommand command = new NonGenericSleepCommand(1);
        HystrixTestUtils.CallType type = HystrixTestUtils.CallType.TO_OBSERVABLE;
        String commandName = command.getClass().getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("GenericSleepCommand: Slept for 1", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
    }

    /*
     * In the failure case, this is the test that fails with a NullPointerException in com.sun.beans.TypeResolver
     */
    @Test
    public void testHelloWorldExecute() throws Exception {
        HystrixCommand<String> command = new HelloWorldCommand();
        HystrixTestUtils.CallType type = HystrixTestUtils.CallType.EXECUTE;
        String commandName = HelloWorldCommand.class.getName();
        String result = (String) HystrixTestUtils.runCommand(command, type);
        Assert.assertEquals("Hello World", result);
        HystrixTestUtils.verifyHystrixMetricsOneCommand(commandName, type, false);
        HystrixTestUtils.verifyOneHystrixTransactionTrace(commandName, type);
        HystrixTestUtils.verifyEvent(HystrixTestUtils.class.getName(), HystrixTestUtils.TimeEvaluation.TOTAL_TIME_GREATER);
    }
}