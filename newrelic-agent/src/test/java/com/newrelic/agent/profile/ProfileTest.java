/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.newrelic.agent.Duration;
import com.newrelic.agent.MockDispatcher;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProfileTest {
    
    private static MockServiceManager serviceManager;

    @BeforeClass
    public static void beforeClass() throws Exception {

        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        Map<String, Object> map = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(map);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

    }

    @Test
    public void testAdd() {
        createTestProfile();
    }

    @Test
    public void testAskForBogusThread() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(66666);
        Assert.assertNull(threadInfo);
    }

    private IProfile createTestProfile() {
        StackTraceElement method = new StackTraceElement("com.acme.explosive", "detonate", "explosive.java", 123);
        StackTraceElement methodCaller = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java", 123);

        IProfile profile = getProfile();

        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodCaller);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, method, methodCaller);

        return profile;
    }

    /*
     * @Test public void testGetCallCounts() { Profile profile = new Profile(50);
     * 
     * Assert.assertEquals(0, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCounts().size());
     * 
     * StackTraceElement stackElement = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
     * profile.add(new ProfileSegment(stackElement, (ProfileSegment)null), null);
     * 
     * Map<ProfiledMethod,Integer> callCounts =
     * profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCounts(); Integer count =
     * (Integer)callCounts.get(new ProfiledMethod(stackElement)); Assert.assertEquals(1, count.longValue()); }
     */

    @Test
    public void testGetCallCount() {
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, false, null, null);
        IProfile profile = new Profile(parameters);

        Assert.assertEquals(0, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCount(null));

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        StackTraceElement methodTwice = new StackTraceElement("com.acme.explosive", "detonate", "explosive.java", 123);
        StackTraceElement methodCaller = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java", 123);
        StackTraceElement methodCallerToo = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java",
                321);

        Assert.assertEquals(0, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCount(methodCaller));

        // profile.addStackTrace(0, false,methodCaller);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodOnce, methodCaller);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodTwice);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodTwice, methodCaller);

        Assert.assertEquals(2, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCount(methodCaller));
        Assert.assertEquals(0, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCount(methodCallerToo));
        Assert.assertEquals(1, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCount(methodOnce));
        Assert.assertEquals(1, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallCount(methodTwice));
    }

    @Test
    public void oneFrameStackIgnored() {
        IProfile profile = getProfile();

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodOnce);
        Assert.assertEquals(0, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallSiteCount());
    }

    @Test
    public void testGetCallSiteCount() {
        IProfile profile = getProfile();

        Assert.assertEquals(0, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallSiteCount());

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        StackTraceElement methodTwice = new StackTraceElement("com.acme.explosive", "detonate", "explosive.java", 123);
        StackTraceElement methodCaller = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java", 123);

        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodCaller);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodOnce, methodCaller);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodTwice);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodTwice, methodCaller);

        Assert.assertEquals(0, profile.getSampleCount());
        Assert.assertEquals(3, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallSiteCount());
    }

    @Test
    public void testGetMethodCount() {
        IProfile profile = getProfile();

        Assert.assertEquals(0, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getMethodCount());

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        StackTraceElement methodTwice = new StackTraceElement("com.acme.explosive", "detonate", "explosive.java", 123);
        StackTraceElement methodCaller = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java", 123);

        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodOnce, methodCaller);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodTwice);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, methodTwice, methodCaller);

        Assert.assertEquals(3, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getMethodCount());
    }

    @Test
    public void testAddStackTrace() {
        IProfile profile = getProfile();

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, stackTrace);

        Assert.assertEquals(stackTrace.length,
                profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallSiteCount());
    }

    @Test
    public void veryLargeProfile() throws IOException {
        IProfile profile = getProfile();
        profile.start();

        ProfileTree tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);
        List<ProfiledMethod> methods = new ArrayList<>();
        List<StackTraceElement> stacks = new ArrayList<>();
        int rootCount = ((Profile.MAX_STACK_SIZE / 10) + Profile.MAX_STACK_SIZE) / 300;
        int count = 0;
        for (int i = 0; i < rootCount; i++) {
            stacks.clear();
            for (int j = 0; j < Profile.MAX_STACK_DEPTH; j++) {
                StackTraceElement stack = new StackTraceElement("Class" + count, "Method" + count, "file.java", count);
                stacks.add(stack);
                tree.addStackTrace(stacks, true);
                methods.add(ProfiledMethod.newProfiledMethod(stack));
                count++;
            }
        }

        int expected = rootCount * Profile.MAX_STACK_DEPTH;
        tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);
        Assert.assertEquals(expected, tree.getCallSiteCount());

        profile.end();
        Assert.assertEquals(Math.min(Profile.MAX_STACK_SIZE, expected), tree.getCallSiteCount());

    }

    @Test
    public void truncateForCollectorLimit() throws IOException {
        IProfile profile = getProfile();
        profile.start();

        ProfileTree tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);

        int longStringSize = 300;
        StringBuilder longStringBuilder = new StringBuilder(longStringSize);
        for(int j=0; j<longStringSize; j++) {
            longStringBuilder.append(j);
        }
        String longString = longStringBuilder.toString();

        List<ProfiledMethod> methods = new ArrayList<>();
        List<StackTraceElement> stacks = new ArrayList<>();
        int rootCount = ((Profile.MAX_STACK_SIZE / 10) + Profile.MAX_STACK_SIZE) / 300;
        int count = 0;
        for (int i = 0; i < rootCount; i++) {
            stacks.clear();
            for (int j = 0; j < Profile.MAX_STACK_DEPTH; j++) {
                StackTraceElement stack = new StackTraceElement(longString, longString, longString, count);
                stacks.add(stack);
                tree.addStackTrace(stacks, true);
                methods.add(ProfiledMethod.newProfiledMethod(stack));
                count++;
            }
        }

        int expected = rootCount * Profile.MAX_STACK_DEPTH;
        tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);
        Assert.assertEquals(expected, tree.getCallSiteCount());

        profile.end();
        Assert.assertEquals(Math.min(Profile.MAX_STACK_SIZE, expected), tree.getCallSiteCount());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        profile.writeJSONString(writer);
        Assert.assertTrue(out.toByteArray().length <= Profile.MAX_ENCODED_BYTES);
    }

    @Test
    public void truncateForCollectorLimitSimpleCompression() throws IOException {
        turnOnSimpleCompression();
        
        try {
            IProfile profile = getProfile();
            profile.start();

            ProfileTree tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);

            int longStringSize = 300;
            StringBuilder longStringBuilder = new StringBuilder(longStringSize);
            for (int j = 0; j < longStringSize; j++) {
                longStringBuilder.append(j);
            }
            String longString = longStringBuilder.toString();

            List<ProfiledMethod> methods = new ArrayList<>();
            List<StackTraceElement> stacks = new ArrayList<>();
            int rootCount = ((Profile.MAX_STACK_SIZE / 10) + Profile.MAX_STACK_SIZE) / 300;
            int count = 0;
            for (int i = 0; i < rootCount; i++) {
                stacks.clear();
                for (int j = 0; j < Profile.MAX_STACK_DEPTH; j++) {
                    StackTraceElement stack = new StackTraceElement(longString, longString, longString, count);
                    stacks.add(stack);
                    tree.addStackTrace(stacks, true);
                    methods.add(ProfiledMethod.newProfiledMethod(stack));
                    count++;
                }
            }

            int expected = rootCount * Profile.MAX_STACK_DEPTH;
            tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);
            Assert.assertEquals(expected, tree.getCallSiteCount());

            profile.end();
            Assert.assertEquals(Math.min(Profile.MAX_STACK_SIZE, expected), tree.getCallSiteCount());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // If using simple compression the full payload will be deflated
            OutputStreamWriter writer = new OutputStreamWriter(new DeflaterOutputStream(out,
                    new Deflater(Deflater.DEFAULT_COMPRESSION)));
            
            profile.writeJSONString(writer);
            Assert.assertTrue(out.toByteArray().length <= Profile.MAX_ENCODED_BYTES);
        } finally {
            turnOffSimpleCompression();
        }
    }

    @Test
    public void trimSingleRoot() {
        IProfile profile = getProfile();

        StackTraceElement stack1 = new StackTraceElement("Class1", "Method1", "Class1.java", 1);
        StackTraceElement stack2 = new StackTraceElement("Class2", "Method2", "Class2.java", 2);
        StackTraceElement stack3 = new StackTraceElement("Class3", "Method3", "Class3.java", 3);
        StackTraceElement stack4 = new StackTraceElement("Class4", "Method4", "Class4.java", 4);
        StackTraceElement stack5 = new StackTraceElement("Class5", "Method5", "Class5.java", 5);
        StackTraceElement stack6 = new StackTraceElement("Class6", "Method6", "Class6.java", 6);
        StackTraceElement stack7 = new StackTraceElement("Class7", "Method7", "Class7.java", 7);

        ProfiledMethod method1 = ProfiledMethod.newProfiledMethod(stack1);
        ProfiledMethod method2 = ProfiledMethod.newProfiledMethod(stack2);
        ProfiledMethod method3 = ProfiledMethod.newProfiledMethod(stack3);

        StackTraceElement[] stack_123 = new StackTraceElement[] { stack1, stack2, stack3 };
        StackTraceElement[] stack_124 = new StackTraceElement[] { stack1, stack2, stack4 };
        StackTraceElement[] stack_125 = new StackTraceElement[] { stack1, stack2, stack5 };
        StackTraceElement[] stack_12567 = new StackTraceElement[] { stack1, stack2, stack5, stack6, stack7 };

        // 6 hits on 1, 6 hits on 2, 3 hits on 3, 1 hit on 4, 2 hits on 5, 1 hits on 6, 1 hit on 7
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_124), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_125), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_12567), true);

        ProfileTree tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);
        Assert.assertEquals(7, tree.getCallSiteCount());

        Assert.assertEquals(4, profile.trimBy(4));
        Assert.assertEquals(3, tree.getCallSiteCount());

        List<ProfiledMethod> expectedMethods = new ArrayList<>();
        expectedMethods.add(method1);
        expectedMethods.add(method2);
        expectedMethods.add(method3);
        List<ProfiledMethod> actualMethods = new ArrayList<>(tree.getCallSiteCount());
        for (ProfileSegment seg : tree.getRootSegments()) {
            actualMethods.addAll(seg.getMethods());
        }
        Assert.assertEquals(expectedMethods.size(), actualMethods.size());
        Assert.assertTrue(actualMethods.containsAll(expectedMethods));

    }

    @Test
    public void trimMultiRoots() {
        IProfile profile = getProfile();

        StackTraceElement stack1 = new StackTraceElement("Class1", "Method1", "Class1.java", 1);
        StackTraceElement stack2 = new StackTraceElement("Class2", "Method2", "Class2.java", 2);
        StackTraceElement stack3 = new StackTraceElement("Class3", "Method3", "Class3.java", 3);
        StackTraceElement stack4 = new StackTraceElement("Class4", "Method4", "Class4.java", 4);
        StackTraceElement stack5 = new StackTraceElement("Class5", "Method5", "Class5.java", 5);
        StackTraceElement stack6 = new StackTraceElement("Class6", "Method6", "Class6.java", 6);
        StackTraceElement stack7 = new StackTraceElement("Class7", "Method7", "Class7.java", 7);
        StackTraceElement stack8 = new StackTraceElement("Class8", "Method8", "Class8.java", 7);

        ProfiledMethod method1 = ProfiledMethod.newProfiledMethod(stack1);
        ProfiledMethod method2 = ProfiledMethod.newProfiledMethod(stack2);
        ProfiledMethod method5 = ProfiledMethod.newProfiledMethod(stack5);
        ProfiledMethod method6 = ProfiledMethod.newProfiledMethod(stack6);

        StackTraceElement[] stack_1 = new StackTraceElement[] { stack1 };
        StackTraceElement[] stack_12 = new StackTraceElement[] { stack1, stack2 };
        StackTraceElement[] stack_123 = new StackTraceElement[] { stack1, stack2, stack3 };
        StackTraceElement[] stack_1234 = new StackTraceElement[] { stack1, stack2, stack3, stack4 };
        StackTraceElement[] stack_5 = new StackTraceElement[] { stack5 };
        StackTraceElement[] stack_56 = new StackTraceElement[] { stack5, stack6 };
        StackTraceElement[] stack_567 = new StackTraceElement[] { stack5, stack6, stack7 };
        StackTraceElement[] stack_5678 = new StackTraceElement[] { stack5, stack6, stack7, stack8 };

        // 4 hits on 1, 3 hits on 2, 2 hits on 3, 1 hit on 4
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_1), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_12), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_1234), true);

        // 4 hits on 4, 3 hits on 5, 2 hits on 6, 1 hit on 7
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_5), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_56), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_567), true);
        profile.getProfileTree(ThreadType.BasicThreadType.OTHER).addStackTrace(Arrays.asList(stack_5678), true);

        ProfileTree tree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);
        Assert.assertEquals(8, tree.getCallSiteCount());

        Assert.assertEquals(4, profile.trimBy(4));
        Assert.assertEquals(4, tree.getCallSiteCount());

        List<ProfiledMethod> expectedMethods = new ArrayList<>();
        expectedMethods.add(method1);
        expectedMethods.add(method2);
        expectedMethods.add(method5);
        expectedMethods.add(method6);
        List<ProfiledMethod> actualMethods = new ArrayList<>(tree.getCallSiteCount());
        for (ProfileSegment seg : tree.getRootSegments()) {
            actualMethods.addAll(seg.getMethods());
        }
        Assert.assertEquals(expectedMethods.size(), actualMethods.size());
        Assert.assertTrue(actualMethods.containsAll(expectedMethods));

    }

    @Test
    public void testAddStackTraces() {
        IProfile profile = getProfile();

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, stackTrace);
        Assert.assertEquals(stackTrace.length,
                profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallSiteCount());

        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, stackTrace);

        Assert.assertEquals(stackTrace.length,
                profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallSiteCount());

        stackTrace = Thread.currentThread().getStackTrace();
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, stackTrace);

        Assert.assertEquals(stackTrace.length,
                profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getCallSiteCount(), 2);

        Assert.assertEquals(1, profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getRootCount());
    }

    @Test
    public void testAddBackgroundThreadStackTraces() {
        ProfilerParameters parameters = new ProfilerParameters(-1L, 0L, 0L, false, false, false, "name", null);
        KeyTransactionProfile keyTxProfile = new KeyTransactionProfile(new Profile(parameters));
        IProfile profile = keyTxProfile.getDelegate();
        StackTraceElement[] stack = new StackTraceElement[] { createSimpleStackTrace("c"), createSimpleStackTrace("b"),
                createSimpleStackTrace("a") };
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.BACKGROUND, stack);
        Assert.assertEquals(profile.getProfileTree(ThreadType.BasicThreadType.BACKGROUND).getCallSiteCount(), 3);
    }

    @Test
    public void testAsyncStackTracesMultipleThreads() throws Exception {
        ExecutorService singleThreadExecutor1 = Executors.newSingleThreadExecutor();
        ExecutorService singleThreadExecutor2 = Executors.newSingleThreadExecutor();
        Future<Long> getThreadId1 = singleThreadExecutor1.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return Thread.currentThread().getId();
            }
        });
        Future<Long> getThreadId2 = singleThreadExecutor2.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return Thread.currentThread().getId();
            }
        });
        final Long threadId1 = getThreadId1.get(10, TimeUnit.SECONDS);
        final Long threadId2 = getThreadId2.get(10, TimeUnit.SECONDS);

        ProfilerParameters parameters = new ProfilerParameters(-1L, 0L, 0L, false, false, false, "keyTransaction",
                "asyncStackTraces");
        final KeyTransactionProfile profile = new KeyTransactionProfile(new Profile(parameters));
        profile.start();
        StackTraceElement[] keyTransactionTrace1 = new StackTraceElement[] {
                createSimpleStackTrace("keyTransaction1"),
                createSimpleStackTrace("keyTransaction1"),
                createSimpleStackTrace("keyTransaction1")};

        final long startTime1 = System.nanoTime();
        profile.addStackTrace(threadId1, true, ThreadType.BasicThreadType.OTHER, keyTransactionTrace1);
        final long endTime1 = System.nanoTime();

        StackTraceElement[] keyTransactionTrace2 = new StackTraceElement[] {
                createSimpleStackTrace("keyTransaction2"),
                createSimpleStackTrace("keyTransaction2"),
                createSimpleStackTrace("keyTransaction2")};

        final long startTime2 = System.nanoTime();
        profile.addStackTrace(threadId2, true, ThreadType.BasicThreadType.OTHER, keyTransactionTrace2);
        final long endTime2 = System.nanoTime();

        Future<?> transactionFinished = singleThreadExecutor2.submit(new Runnable() {
            @Override
            public void run() {
                Multimap<Long, Duration> threadIdToDuration = ArrayListMultimap.create();
                threadIdToDuration.put(threadId1, new Duration(startTime1, endTime1));
                threadIdToDuration.put(threadId2, new Duration(startTime2, endTime2));

                profile.dispatcherTransactionFinished(generateTransactionData(threadIdToDuration, startTime1, endTime2,
                        "asyncStackTraces"), new TransactionStats());
            }
        });
        transactionFinished.get(10, TimeUnit.SECONDS);

        profile.end();
        ProfileTree requestProfileTree = profile.getProfileTree(ThreadType.BasicThreadType.OTHER);
        Collection<ProfileSegment> rootSegments = requestProfileTree.getRootSegments();
        assertNotNull(rootSegments);
        assertEquals(2, rootSegments.size());
    }

    private TransactionData generateTransactionData(Multimap<Long, Duration> threadIdToDuration, long startTime,
            long endTime, String appName) {
        MockDispatcher dispatcher = new MockDispatcher();
        dispatcher.setWebTransaction(true);
        MockDispatcherTracer rootTracer = new MockDispatcherTracer();
        rootTracer.setDurationInMilliseconds(endTime - startTime);
        rootTracer.setStartTime(startTime);
        rootTracer.setEndTime(endTime);
        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(appName);

        return new TransactionDataTestBuilder(appName, agentConfig, rootTracer)
                .setDispatcher(rootTracer)
                .setFrontendMetricName("keyTransaction")
                .setThreadIdToDuration(threadIdToDuration)
                .build();
    }

    private IProfile treeTestProfile() {
        IProfile profile = getProfile();
        final StackTraceElement[] stack = new StackTraceElement[] { createSimpleStackTrace("c"),
                createSimpleStackTrace("b"), createSimpleStackTrace("a") };
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, stack);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, stack);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, stack);
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, createSimpleStackTrace("d"), createSimpleStackTrace("c"), createSimpleStackTrace("b"),
                createSimpleStackTrace("a"));
        return profile;
    }

    @Test
    public void testTopDownTree() {
        IProfile profile = treeTestProfile();

        verifyTree(profile.getProfileTree(ThreadType.BasicThreadType.OTHER), new HashMap<String, Integer>() {

            private static final long serialVersionUID = 1L;

            {
                put("a", 4);
            }
        });
    }

    private void verifyTree(ProfileTree tree, Map<String, Integer> expected) {
        Assert.assertEquals(expected.size(), tree.getRootCount());

        for (ProfileSegment segment : tree.getRootSegments()) {
            Assert.assertEquals(expected.get(segment.getMethod().getClassName()),
                    (Integer) segment.getRunnableCallCount());
        }
    }

    private IProfile getProfile() {
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, true, null, null);
        return new Profile(parameters);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteCompressedData() throws IOException, ParseException {
        IProfile profile = createTestProfile();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        profile.getProfileTree(ThreadType.BasicThreadType.BasicThreadType.OTHER).writeJSONString(writer);
        writer.close();

        JSONParser parser = new JSONParser();
        Object parse = parser.parse(out.toString());
        Assert.assertTrue(parse instanceof List);

        List profileSegments = (List) parse;
        Assert.assertEquals(profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getRootCount() + 1,
                profileSegments.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testJson() throws Exception {
        IProfile profile = createTestProfile();

        verifyProfile(profile, false);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleCompressionJson() throws Exception {
        turnOnSimpleCompression();

        try {
            IProfile profile = createTestProfile();

            verifyProfile(profile, true);
        } finally {
            turnOffSimpleCompression();
        }
    }

    private void verifyProfile(IProfile profile, boolean simpleCompression) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        if (simpleCompression) {
            // If using simple compression the full payload will be deflated
            writer = new OutputStreamWriter(new DeflaterOutputStream(out, new Deflater(Deflater.DEFAULT_COMPRESSION)));
        }

        profile.writeJSONString(writer);

        writer.close();
        byte[] profileOutput = out.toByteArray();

        // Verify that data is less than the collector max of 1MB
        Assert.assertTrue(profileOutput.length < 1000000);

        JSONParser parser = new JSONParser();
        Object parse;

        // After verifying data size, inflate the data to parse the json
        if (simpleCompression) {
            InflaterInputStream inStream = new InflaterInputStream(new ByteArrayInputStream(profileOutput));
            String jsonOutput = CharStreams.toString(new InputStreamReader(inStream, StandardCharsets.UTF_8));
            parse = parser.parse(jsonOutput);
        } else {
            parse = parser.parse(new String(profileOutput, StandardCharsets.UTF_8));
        }
        Assert.assertTrue(parse instanceof List);

        List data = (List) parse;

        Assert.assertEquals(profile.getProfileId(), data.get(0));
        Assert.assertEquals(profile.getStartTimeMillis(), data.get(1));
        Assert.assertEquals(profile.getEndTimeMillis(), data.get(2));

        parser = new JSONParser();
        Object parsedData;
        if (simpleCompression) {
            parsedData = data.get(4);
        } else {
            String rawData = (String) data.get(4);
            byte[] compressedData = Base64.getDecoder().decode(rawData);

            InflaterInputStream inStream = new InflaterInputStream(new ByteArrayInputStream(compressedData));
            rawData = CharStreams.toString(new InputStreamReader(inStream, StandardCharsets.UTF_8));
            parsedData = parser.parse(rawData);
        }

        Assert.assertTrue(parse instanceof List);
        Map trees = (Map) parsedData;
        Assert.assertEquals(1, trees.size());

        List segments = (List) trees.values().iterator().next();
        Assert.assertEquals(profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getRootCount() + 1,
                segments.size());
    }

    private StackTraceElement createSimpleStackTrace(String name) {
        return new StackTraceElement(name, name, name, 1);
    }

    private void turnOnSimpleCompression() {
        Map<String, Object> map = ImmutableMap.<String, Object>of(AgentConfigImpl.SIMPLE_COMPRESSION_PROPERTY, true);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(map);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);
    }

    private void turnOffSimpleCompression() {
        Map<String, Object> map = ImmutableMap.<String, Object>of(AgentConfigImpl.SIMPLE_COMPRESSION_PROPERTY, false);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(map);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);
    }
}
