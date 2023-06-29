/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.profile.RunnableThreadRules;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackTracesTest {
    @Test
    public void toStringList() {
        List<StackTraceElement> stack = generateStack(20);

        List<String> stringList = StackTraces.toStringList(stack);
        Assert.assertEquals(stack.size(), stringList.size());
    }

    @Test
    public void testDoNotScubOtherNewRelicCode() {
        StackTraceElement root = new StackTraceElement("myroot.node.Bar", "method", "file", 0);

        StackTraceElement keep1 = new StackTraceElement("com.newrelic.dirac.Bar", "method", "file", 0);
        StackTraceElement keep2 = new StackTraceElement("com.nr.dirac.Bar", "method", "file", 0);
        StackTraceElement keep3 = new StackTraceElement("com.newrelic.agentvalidator.AgentActionConsumer", "method",
                "file", 0);
        StackTraceElement keep4 = new StackTraceElement("com.newrelic.collector.Bar", "method", "file", 0);
        StackTraceElement keep5 = new StackTraceElement("com.newrelic.kafka.Bar", "method", "file", 0);
        StackTraceElement keep6 = new StackTraceElement("com.newrelic.servlet.Bar", "method", "file", 0);

        StackTraceElement remove1 = new StackTraceElement("com.newrelic.api.agent.weaver.Bar", "method", "file", 0);
        StackTraceElement remove2 = new StackTraceElement("com.newrelic.agent.application.Bar", "method", "file", 0);
        StackTraceElement remove3 = new StackTraceElement("com.newrelic.bootstrap.BootstrapAgent", "method", "file", 0);
        StackTraceElement remove4 = new StackTraceElement("com.newrelic.weave.Bar", "method", "file", 0);

        List<StackTraceElement> actual = StackTraces.scrub(Arrays.asList(root, remove1, keep1, keep2, keep3,
                keep4, keep5, keep6));
        Assert.assertEquals(6, actual.size());
        Assert.assertFalse("com.newrelic.api.agent. should not be kept", actual.contains(remove1));
        Assert.assertFalse(actual.contains(root));

        actual = StackTraces.scrub(Arrays.asList(root, remove2, keep1, keep2, keep3, keep4, keep5, keep6));
        Assert.assertEquals(6, actual.size());
        Assert.assertFalse("com.newrelic.agent.application. should not be kept", actual.contains(remove2));
        Assert.assertFalse(actual.contains(root));

        actual = StackTraces.scrub(Arrays.asList(root, remove3, keep1, keep2, keep3, keep4, keep5, keep6));
        Assert.assertEquals(6, actual.size());
        Assert.assertFalse("com.newrelic.bootstrap.BootstrapAgent should not be kept", actual.contains(remove2));
        Assert.assertFalse(actual.contains(root));

        actual = StackTraces.scrub(Arrays.asList(root, remove4, keep1, keep2, keep3, keep4, keep5, keep6));
        Assert.assertEquals(6, actual.size());
        Assert.assertFalse("com.newrelic.weave.Bar should not be kept", actual.contains(remove4));
        Assert.assertFalse(actual.contains(root));
    }

    @Test
    public void testScrubNothingToScrub() {
        List<StackTraceElement> stackTraces = Arrays.asList(new StackTraceElement("foo.Bar", "foo", "foo.class", 1));
        Assert.assertEquals(stackTraces.size(), StackTraces.scrubAndTruncate(stackTraces, 0).size());
    }

    @Test
    public void testScrub() {
        StackTraceElement firstElement = new StackTraceElement("foo.Bar", "foo", "foo.class", 1), lastElement = new StackTraceElement(
                "yoyo.Dude", "man", "Dude.class", 6);
        List<StackTraceElement> stackTraces = Arrays.asList(new StackTraceElement("java.logger.Log", "log", "", 66),
                new StackTraceElement("com.newrelic.agent.test.Test", "test", "", 66),
                new StackTraceElement("com.newrelic.weave.test.Test", "test", "", 66), firstElement,
                new StackTraceElement("foo.Bar", "foo", "foo.class", 6), new StackTraceElement("boo.Bar", "dude",
                        "boo.class", 6), lastElement);
        List<StackTraceElement> scrubbedList = StackTraces.scrubAndTruncate(stackTraces, 0);
        Assert.assertEquals(4, scrubbedList.size());
        Assert.assertEquals(firstElement, scrubbedList.get(0));
        Assert.assertEquals(lastElement, scrubbedList.get(3));
    }

    @Test
    public void testToStringListRemoveParent1() {
        // need configuration service set
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);
        // get stack traces
        List<StackTraceElement> parentTrace = generateStack(5, 15);
        List<StackTraceElement> childTrace = generateStack(0, 10);

        List<String> trimmed = StackTraces.toStringListRemoveParent(childTrace, parentTrace);
        Assert.assertEquals(5, trimmed.size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals("Test.method(file:" + i + ")", trimmed.get(i));
        }
    }

    @Test
    public void testToStringListRemoveParent2() {
        // need configuration service set
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);
        // get stack traces
        StackTraceElement[] parentTraceArray = Thread.currentThread().getStackTrace();
        List<StackTraceElement> parentTrace = changeComNewrelic(parentTraceArray);

        StackTraceElement[] childTraceArray = anotherMethod1();
        List<StackTraceElement> childTrace = changeComNewrelic(childTraceArray);
        List<StackTraceElement> childScrubbed = StackTraces.scrubAndTruncate(childTrace);

        List<String> trimmed = StackTraces.toStringListRemoveParent(childScrubbed, parentTrace);
        Assert.assertEquals(3, trimmed.size());

        Assert.assertTrue("0 failed " + trimmed.get(0), trimmed.get(0).startsWith(
                "hello.howdy.util.StackTracesTest.anotherMethod3("));
        Assert.assertTrue("1 failed" + trimmed.get(1), trimmed.get(1).startsWith(
                "hello.howdy.util.StackTracesTest.anotherMethod2("));
        Assert.assertTrue("2 failed" + trimmed.get(2), trimmed.get(2).startsWith(
                "hello.howdy.util.StackTracesTest.anotherMethod1("));

    }

    @Test
    public void test_getThreadStackTraceElements_andLast() {
        StackTraceElement[] elements = StackTraces.getThreadStackTraceElements(Thread.currentThread().getId());
        Assert.assertNotNull(elements);
        Assert.assertTrue(elements.length > 0);
        Collection<String> elementStrs = StackTraces.stackTracesToStrings(elements);
        Assert.assertNotNull(elementStrs);
        Assert.assertEquals(elements.length, elementStrs.size());

        // less than requested exists, no actual trim
        List elementList = StackTraces.last(elements, elements.length+1);
        Assert.assertEquals(elements.length, elementList.size());

        // actually trim down to size
        elementList = StackTraces.last(elements, 3);
        Assert.assertEquals(3, elementList.size());

        // no thread
        elements = StackTraces.getThreadStackTraceElements(Integer.MAX_VALUE);
        Assert.assertNull(elements);
        elementStrs = StackTraces.stackTracesToStrings(elements);
        Assert.assertNotNull(elementStrs);
        Assert.assertEquals(0, elementStrs.size());
    }

    @Test
    public void test_createStackTraceException() {
        Exception result = StackTraces.createStackTraceException("MyException");
        Assert.assertNotNull(result);
        Assert.assertEquals("MyException", result.getMessage());
        Assert.assertTrue(result.getStackTrace().length > 0);
    }

    private List<StackTraceElement> changeComNewrelic(StackTraceElement[] elements) {
        String currentClass;
        List<StackTraceElement> toReturn = new ArrayList<>(elements.length);
        for (StackTraceElement current : elements) {
            currentClass = current.getClassName();
            if (currentClass.startsWith("com.newrelic.agent")) {

                currentClass = currentClass.replaceAll("com.newrelic.agent", "hello.howdy");
                StackTraceElement theNewOne = new StackTraceElement(currentClass, current.getMethodName(),
                        current.getFileName(), current.getLineNumber());
                toReturn.add(theNewOne);
            } else if (currentClass.startsWith("java.lang.Thread")) {
                // do nothing
            } else {
                toReturn.add(current);
            }
        }
        return toReturn;
    }

    @Test
    public void isSame() {
        StackTraceElement one = new StackTraceElement("1", "2", null, 0);
        Assert.assertTrue(StackTraces.isSameClassAndMethod(one, one));
        StackTraceElement two = new StackTraceElement("1", "2", null, 0);
        Assert.assertTrue(StackTraces.isSameClassAndMethod(one, two));

        one = new StackTraceElement("hello", "running", null, 10);
        two = new StackTraceElement("hello", "running", null, 50);
        Assert.assertTrue(StackTraces.isSameClassAndMethod(one, two));

        one = new StackTraceElement("hello", "method1", null, 20);
        two = new StackTraceElement("hello", "method1", null, 30);
        Assert.assertTrue(StackTraces.isSameClassAndMethod(one, two));

        one = new StackTraceElement("hello1", "method1", null, 20);
        two = new StackTraceElement("hello", "method1", null, 30);
        Assert.assertFalse(StackTraces.isSameClassAndMethod(one, two));

        one = new StackTraceElement("hello", "method", null, 20);
        two = new StackTraceElement("hello", "method1", null, 30);
        Assert.assertFalse(StackTraces.isSameClassAndMethod(one, two));
    }

    private StackTraceElement[] anotherMethod1() {
        return anotherMethod2();
    }

    private StackTraceElement[] anotherMethod2() {
        return anotherMethod3();
    }

    private StackTraceElement[] anotherMethod3() {
        return Thread.currentThread().getStackTrace();
    }

    @Test
    public void testToStringListRemoveParentNullParent() {
        // need configuration service set
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);
        // get stack traces
        List<StackTraceElement> parentTrace = null;
        List<StackTraceElement> childTrace = generateStack(0, 10);

        List<String> trimmed = StackTraces.toStringListRemoveParent(childTrace, parentTrace);
        Assert.assertEquals(childTrace.size(), trimmed.size());
        for (int i = 0; i < childTrace.size(); i++) {
            Assert.assertEquals(childTrace.get(i).toString(), trimmed.get(i));
        }
    }

    @Test
    public void testToStringListRemoveParentScrubbedParent() {
        // need configuration service set
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        Map<String, Object> configMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);
        // get stack traces
        List<StackTraceElement> parentTrace = generateStack(0, 2, "com.newrelic.agent.Test");
        List<StackTraceElement> childTrace = generateStack(0, 10);

        List<String> trimmed = StackTraces.toStringListRemoveParent(childTrace, parentTrace);
        Assert.assertEquals(childTrace.size(), trimmed.size());
        for (int i = 0; i < childTrace.size(); i++) {
            Assert.assertEquals(childTrace.get(i).toString(), trimmed.get(i));
        }
    }

    @Test
    public void testScrubAgentHandle() {
        List<StackTraceElement> stackTraces = Arrays.asList(new StackTraceElement(Proxy.class.getName(),
                "getAgentHandle", "", 66));
        List<StackTraceElement> scrubbed = StackTraces.scrub(stackTraces);
        Assert.assertEquals(0, scrubbed.size());
    }

    @Test
    public void truncateStack() {

        List<StackTraceElement> stack = generateStack(20);

        List<StackTraceElement> truncatedStack = StackTraces.truncateStack(stack, 10);

        Assert.assertEquals(11, truncatedStack.size());

        Assert.assertEquals(stack.get(0), truncatedStack.get(0));
        Assert.assertEquals(stack.get(stack.size() - 1), truncatedStack.get(truncatedStack.size() - 1));
    }

    @Test
    public void getRootCause() {
        Throwable error = new SQLException("dude");
        Assert.assertEquals(error, StackTraces.getRootCause(error));

        Assert.assertEquals(error, StackTraces.getRootCause(new ServletException(error)));
    }

    @Test
    public void truncateStackUnderLimit() {

        List<StackTraceElement> stack = generateStack(10);

        List<StackTraceElement> truncatedStack = StackTraces.truncateStack(stack, 10);

        Assert.assertEquals(10, truncatedStack.size());
        Assert.assertEquals(stack, truncatedStack);
    }

    private List<StackTraceElement> generateStack(int size) {
        ArrayList<StackTraceElement> stack = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            stack.add(new StackTraceElement("Test", "method", "file", i));
        }
        return stack;
    }

    private List<StackTraceElement> generateStack(int start, int size) {
        return generateStack(start, size, "Test");
    }

    private List<StackTraceElement> generateStack(int start, int size, String className) {
        ArrayList<StackTraceElement> stack = new ArrayList<>();
        int end = size + start;
        for (int i = start; i < end; i++) {
            stack.add(new StackTraceElement(className, "method", "file", i));
        }
        return stack;
    }

    @Test
    public void testRunnable() {
        ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId(), 200);
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertTrue(runnableThreadRules.isRunnable(threadInfo));
    }

    @Test
    public void weblogicNTSocketMuxerNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("weblogic.socket.NTSocketMuxer",
                "getIoCompletionResult", "", -2)));
    }

    @Test
    public void isRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertTrue(runnableThreadRules.isRunnable(new StackTraceElement("com.dude.Test", "go", "", 4)));
        Assert.assertTrue(runnableThreadRules.isRunnable(new StackTraceElement("com.dude.Test", "go", "", -2)));
    }

    @Test
    public void emptyStackNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement[0]));
    }

    @Test
    public void stackIsRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        StackTraceElement[] elements = new StackTraceElement[2];
        elements[0] = new StackTraceElement("com.dude.Test", "go", "", 4);
        elements[1] = new StackTraceElement("org.dude.Test", "go", "", -3);
        Assert.assertTrue(runnableThreadRules.isRunnable(elements));
    }

    @Test
    public void objectWaitNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("java.lang.Object", "wait", "", -2)));
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("java.lang.Object", "wait", "", 4566)));
    }

    @Test
    public void javaIONotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("java.io.BufferedWriter", "write", "",
                -2)));
    }

    @Test
    public void socketReadNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("java.net.Socket", "read", "", -2)));
    }

    @Test
    public void nioSocketNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("sun.nio.NIOSocket", "read", "", -2)));
    }

    @Test
    public void jRocketSocketNativeIONotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("jrockit.net.SocketNativeIO",
                "readBytesPinned", "", -2)));
    }

    @Test
    public void waitForProcessExitNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("java.lang.UNIXProcess",
                "waitForProcessExit", "", -2)));
    }

    @Test
    public void unsafeParkNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("sun.misc.Unsafe", "park", "", -2)));
    }

    @Test
    public void socketAcceptNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("org.apache.tomcat.jni.Socket",
                "accept", "", -2)));
    }

    @Test
    public void fileDescriptorSyncNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("java.io.FileDescriptor", "sync", "",
                -2)));
    }

    @Test
    public void tomcatJniPollPollNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("org.apache.tomcat.jni.Poll", "poll",
                "", -2)));
    }

    @Test
    public void weblogicSocketPosixSocketMixerPollNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("weblogic.socket.PosixSocketMuxer",
                "poll", "", -2)));
    }

    @Test
    public void weblogicSocketNTSocketMuxeGetIoCompletionResultNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("weblogic.socket.NTSocketMuxer",
                "getIoCompletionResult", "", -2)));
    }

    @Test
    public void writeBytesNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("java.io.FileOutputStream",
                "writeBytes", "", -2)));
    }

    @Test
    public void nativeAcceptNotRunnable() {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        Assert.assertFalse(runnableThreadRules.isRunnable(new StackTraceElement("com.caucho.vfs.JniServerSocketImpl",
                "nativeAccept", "", -2)));
    }

}