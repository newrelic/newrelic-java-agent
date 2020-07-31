/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tests;

import static rx.Observable.zip;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.nr.agent.instrumentation.commands.IntegerEmittingObservable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import rx.Observable;
import rx.functions.Action1;

import com.netflix.hystrix.HystrixObservableCommand;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.commands.FailureObservableCommand;
import com.nr.agent.instrumentation.commands.HelloWorldObservable;
import com.nr.agent.instrumentation.commands.HelloWorldOneObservable;
import com.nr.agent.instrumentation.commands.TimeoutObservableCommand;
import rx.functions.Func2;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.netflix.hystrix", "com.nr.agent.instrumentation.command" })
public class HystrixObservableTest {

    @Test
    public void testFailureExecute() throws Exception {
        FailureObservableCommand command = new FailureObservableCommand();
        String result = (String) runCommand(command, null, HystrixTestUtils.CallType.EXECUTE);
        Assert.assertEquals("We failed and went to fallback.", result);
        HystrixTestUtils.verifyHystrixObservableMetrics(FailureObservableCommand.class.getName(),
                HystrixTestUtils.CallType.EXECUTE, true, 1, HystrixObservableTest.class.getName());
    }

    @Test
    public void testFailureQueue() throws Exception {
        FailureObservableCommand command = new FailureObservableCommand();
        String result = (String) runCommand(command, null, HystrixTestUtils.CallType.QUEUE);
        Assert.assertEquals("We failed and went to fallback.", result);
        HystrixTestUtils.verifyHystrixObservableMetrics(FailureObservableCommand.class.getName(),
                HystrixTestUtils.CallType.QUEUE, true, 1, HystrixObservableTest.class.getName());
    }

    @Test
    public void testFailureObserve() throws Exception {
        FailureObservableCommand command = new FailureObservableCommand();
        String result = (String) runCommand(command, null, HystrixTestUtils.CallType.OBSERVE);
        Assert.assertEquals("We failed and went to fallback.", result);
        HystrixTestUtils.verifyHystrixObservableMetrics(FailureObservableCommand.class.getName(),
                HystrixTestUtils.CallType.OBSERVE, true, 1, HystrixObservableTest.class.getName());
    }

    @Test
    public void testFailureToObservable() throws Exception {
        FailureObservableCommand command = new FailureObservableCommand();
        String result = (String) runCommand(command, null, HystrixTestUtils.CallType.TO_OBSERVABLE);
        Assert.assertEquals("We failed and went to fallback.", result);
        HystrixTestUtils.verifyHystrixObservableMetrics(FailureObservableCommand.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE, true, 1, HystrixObservableTest.class.getName());
    }

    @Test
    public void testTimeoutExecute() throws Exception {
        TimeoutObservableCommand command = new TimeoutObservableCommand();
        String result = (String) runCommand(command, null, HystrixTestUtils.CallType.EXECUTE);
        Assert.assertEquals("Success - Command Timed Out - YAY!", result);
        HystrixTestUtils.verifyHystrixObservableMetrics(TimeoutObservableCommand.class.getName(),
                HystrixTestUtils.CallType.EXECUTE, true, 1, HystrixObservableTest.class.getName());
    }

    @Test
    public void testTimeoutObserve() throws Exception {
        TimeoutObservableCommand command = new TimeoutObservableCommand();
        String result = (String) runCommand(command, null, HystrixTestUtils.CallType.OBSERVE);
        Assert.assertEquals("Success - Command Timed Out - YAY!", result);
        HystrixTestUtils.verifyHystrixObservableMetrics(TimeoutObservableCommand.class.getName(),
                HystrixTestUtils.CallType.OBSERVE, true, 1, HystrixObservableTest.class.getName());
    }

    @Test
    public void testTimeoutToObservable() throws Exception {
        TimeoutObservableCommand command = new TimeoutObservableCommand();
        String result = (String) runCommand(command, null, HystrixTestUtils.CallType.TO_OBSERVABLE);
        Assert.assertEquals("Success - Command Timed Out - YAY!", result);
        HystrixTestUtils.verifyHystrixObservableMetrics(TimeoutObservableCommand.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE, true, 1, HystrixObservableTest.class.getName());
    }

    // not exactly an execute, but the closest we can get with an observable
    @Test
    public void testHelloWorldObserableExecute() throws Exception {
        HelloWorldOneObservable command = new HelloWorldOneObservable("Bob");
        String output = (String) runCommand(command, null, HystrixTestUtils.CallType.EXECUTE);
        Assert.assertEquals("Hello Bob!", output);
        HystrixTestUtils.verifyHystrixObservableMetrics(HelloWorldOneObservable.class.getName(),
                HystrixTestUtils.CallType.EXECUTE, false, 1, HystrixObservableTest.class.getName());
    }

    @Test
    public void testHelloWorldObserableQueue() throws Exception {
        HelloWorldOneObservable command = new HelloWorldOneObservable("Sue");
        String output = (String) runCommand(command, null, HystrixTestUtils.CallType.QUEUE);
        Assert.assertEquals("Hello Sue!", output);
        HystrixTestUtils.verifyHystrixObservableMetrics(HelloWorldOneObservable.class.getName(),
                HystrixTestUtils.CallType.QUEUE, false, 1, HystrixObservableTest.class.getName());

    }

    @Test
    public void testHelloWorldObserableObserve() throws Exception {
        HelloWorldObservable command = new HelloWorldObservable("Jane");
        String output = (String) runCommand(command, null, HystrixTestUtils.CallType.OBSERVE);
        Assert.assertEquals("Hello Jane!", output);
        HystrixTestUtils.verifyHystrixObservableMetrics(HelloWorldObservable.class.getName(),
                HystrixTestUtils.CallType.OBSERVE, false, 1, HystrixObservableTest.class.getName());

    }

    @Test
    public void testHelloWorldObserableToObservable() throws Exception {
        HelloWorldObservable command = new HelloWorldObservable("Sue");
        String output = (String) runCommand(command, null, HystrixTestUtils.CallType.TO_OBSERVABLE);
        Assert.assertEquals("Hello Sue!", output);
        HystrixTestUtils.verifyHystrixObservableMetrics(HelloWorldObservable.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE, false, 1, HystrixObservableTest.class.getName());

    }

    @Test
    public void testHelloWorldObservableToObservableSwitchIfEmpty() throws Exception {
        HelloWorldObservable command = new HelloWorldObservable("Sue");
        HelloWorldObservable optionalCommand = new HelloWorldObservable("Joe");
        String output = (String) runCommand(command, optionalCommand, HystrixTestUtils.CallType.TO_OBSERVABLE_SWITCH_IF_EMPTY);
        Assert.assertEquals("Hello Sue!", output);
        HystrixTestUtils.verifyHystrixObservableMetrics(HelloWorldObservable.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE_SWITCH_IF_EMPTY, false, 2, HystrixObservableTest.class.getName());
    }

    @Test
    public void testHelloWorldObservableToObservableZip() throws Exception {
        IntegerEmittingObservable leftCommand = new IntegerEmittingObservable(1, 2, 3, 4, 5);
        IntegerEmittingObservable rightCommand = new IntegerEmittingObservable(6, 7);
        Integer sum = (Integer) runCommand(leftCommand, rightCommand, HystrixTestUtils.CallType.TO_OBSERVABLE_ZIP);
        Assert.assertEquals(16, (int) sum);
        HystrixTestUtils.verifyHystrixObservableMetrics(IntegerEmittingObservable.class.getName(),
                HystrixTestUtils.CallType.TO_OBSERVABLE, false, 2, HystrixObservableTest.class.getName());
    }

    @Trace(dispatcher = true)
    public Object runCommand(HystrixObservableCommand command,
            HystrixObservableCommand optionalCommand, HystrixTestUtils.CallType type) throws Exception {
        switch (type) {
        case EXECUTE:
            // this is the closest thing to execute
            return command.toObservable().toBlocking().toFuture().get();
        case QUEUE:
            // this is the closest thing to queue
            Future<Object> fs = command.toObservable().toBlocking().toFuture();
            Thread.sleep(1);
            return fs.get();
        case TO_OBSERVABLE:
            Observable<Object> observe = command.toObservable();
            final StringBuilder sb = new StringBuilder();
            observe.subscribe(new Action1<Object>() {

                @Override
                public void call(Object v) {
                    sb.append(v);
                }
            });
            // wait for the result
            int value = 0;
            while (sb.length() == 0 && value < 200) {
                Thread.sleep(10);
                value += 10;
            }
            return sb.toString();
        case TO_OBSERVABLE_SWITCH_IF_EMPTY:
            Observable<Object> observable1 = command.toObservable();
            Observable<Object> observable2 = optionalCommand.toObservable();

            final StringBuilder sb2 = new StringBuilder();
            observable1.switchIfEmpty(observable2).subscribe(new Action1<Object>() {
                @Override
                public void call(Object v) {
                    sb2.append(v);
                }
            });

            // wait for the result
            int count2 = 0;
            while (sb2.length() == 0 && count2 < 200) {
                Thread.sleep(10);
                count2 += 10;
            }
            return sb2.toString();
        case TO_OBSERVABLE_ZIP:
            Observable<Integer> observableZip1 = command.toObservable();
            Observable<Integer> observableZip2 = optionalCommand.toObservable();

            final AtomicInteger sum = new AtomicInteger(0);
            zip(observableZip1, observableZip2, new Func2<Integer, Integer, Integer>() {
                @Override
                public Integer call(Integer left, Integer right) {
                    int value = left + right;
                    sum.addAndGet(value);
                    return value;
                }
            }).single().subscribe();

            // wait for the result
            int count3 = 0;
            while (sum.get() < 16 && count3 < 200) {
                Thread.sleep(10);
                count3 += 10;
            }
            return sum.get();
        case OBSERVE:
            Observable<Object> obs = command.observe();
            final StringBuilder sb1 = new StringBuilder();
            obs.subscribe(new Action1<Object>() {

                @Override
                public void call(Object v) {
                    sb1.append(v);
                }

            });
            int count = 0;
            while (sb1.length() == 0 && count < 500) {
                Thread.sleep(10);
                count += 10;
            }
            return sb1.toString();
        }
        return "";
    }
}
