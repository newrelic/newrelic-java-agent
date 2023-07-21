/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.profile.ThreadType;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.threads.BasicThreadInfo;
import com.newrelic.agent.threads.ThreadNameNormalizer;
import com.newrelic.agent.trace.TransactionGuidFactory;
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
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfileTest {

    private static MockServiceManager serviceManager;
    private static ThreadNameNormalizer threadNameNormalizer;

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

        threadNameNormalizer = new ThreadNameNormalizer(agentConfig, threadService);
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

        profile.addStackTrace(getMockedThreadInfo(0, methodCaller), true, ThreadType.BasicThreadType.OTHER);
        profile.addStackTrace(getMockedThreadInfo(0, method, methodCaller), true, ThreadType.BasicThreadType.OTHER);

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
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, false, null, null)
                .setProfilerFormat("v2");
        ThreadMXBean threadMXBean = mock(ThreadMXBean.class);
        when(threadMXBean.getThreadCpuTime(0)).thenReturn(10L);
        IProfile profile = new Profile(parameters, TransactionGuidFactory.generate16CharGuid(), threadNameNormalizer, threadMXBean);

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        StackTraceElement methodTwice = new StackTraceElement("com.acme.explosive", "detonate", "explosive.java", 123);
        StackTraceElement methodCaller = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java", 123);
        StackTraceElement methodCallerToo = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java",
                321);

        // profile.addStackTrace(0, false,methodCaller);
        ThreadInfo threadInfo1 = getMockedThreadInfo(0, methodOnce, methodCaller);
        profile.addStackTrace(threadInfo1, true, ThreadType.BasicThreadType.OTHER);
        ThreadInfo threadInfo2 = getMockedThreadInfo(0, methodTwice);
        profile.addStackTrace(threadInfo2, true, ThreadType.BasicThreadType.OTHER);
        ThreadInfo threadInfo3 = getMockedThreadInfo(0, methodTwice, methodCaller);
        profile.addStackTrace(threadInfo3, true, ThreadType.BasicThreadType.OTHER);

        String normalizedThread1Name = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(threadInfo1));

        ProfileSegment methodCallerSegment = profile.getProfileTree(normalizedThread1Name).getSegmentForMethod(
                methodCaller);
        Assert.assertEquals(2, methodCallerSegment.getCallCount(methodCallerSegment.getMethod()));
        ProfileSegment methodCallerTooSegment = profile.getProfileTree(normalizedThread1Name).getSegmentForMethod(
                methodCallerToo);
        Assert.assertNull(methodCallerTooSegment);
        Map<ProfiledMethod, ProfileSegment> methodCallerChilden = methodCallerSegment.getChildMap();
        Assert.assertEquals(2, methodCallerChilden.size());

        ProfiledMethod methodOnceProfiledMethod = profile.getProfiledMethodFactory().getProfiledMethod(methodOnce);
        Assert.assertEquals(1, methodCallerChilden.get(methodOnceProfiledMethod).getCallCount(methodOnceProfiledMethod));
        ProfiledMethod methodTwiceProfiledMethod = profile.getProfiledMethodFactory().getProfiledMethod(methodTwice);
        Assert.assertEquals(1, methodCallerChilden.get(methodTwiceProfiledMethod).getCallCount(methodTwiceProfiledMethod));
    }

    @Test
    public void oneFrameStackIgnored() {
        IProfile profile = getProfile();

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        ThreadInfo threadInfo = getMockedThreadInfo(0, methodOnce);
        profile.addStackTrace(threadInfo, true, ThreadType.BasicThreadType.OTHER);

        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(threadInfo));

        ProfileSegment methodOnceSegment = profile.getProfileTree(normalizedThreadName).getSegmentForMethod(
                methodOnce);
        Assert.assertNull(methodOnceSegment);
    }

    @Test
    public void testGetCallSiteCount() {
        IProfile profile = getProfile();

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        StackTraceElement methodTwice = new StackTraceElement("com.acme.explosive", "detonate", "explosive.java", 123);
        StackTraceElement methodCaller = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java", 123);

        ThreadInfo threadInfo1 = getMockedThreadInfo(0, methodOnce);
        profile.addStackTrace(threadInfo1, true, ThreadType.BasicThreadType.OTHER);
        ThreadInfo threadInfo2 = getMockedThreadInfo(0, methodOnce, methodCaller);
        profile.addStackTrace(threadInfo2, true, ThreadType.BasicThreadType.OTHER);
        ThreadInfo threadInfo3 = getMockedThreadInfo(0, methodTwice);
        profile.addStackTrace(threadInfo3, true, ThreadType.BasicThreadType.OTHER);
        ThreadInfo threadInfo4 = getMockedThreadInfo(0, methodTwice, methodCaller);
        profile.addStackTrace(threadInfo4, true, ThreadType.BasicThreadType.OTHER);
        
        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(threadInfo1));

        Assert.assertEquals(0, profile.getSampleCount());
        Assert.assertEquals(3, profile.getProfileTree(normalizedThreadName).getCallSiteCount());
    }

    @Test
    public void testGetMethodCount() {
        IProfile profile = getProfile();

        StackTraceElement methodOnce = new StackTraceElement("com.acme.rocket", "launch", "rocket.java", 123);
        StackTraceElement methodTwice = new StackTraceElement("com.acme.explosive", "detonate", "explosive.java", 123);
        StackTraceElement methodCaller = new StackTraceElement("com.wileecoyote.cartoon", "show", "cartoon.java", 123);

        ThreadInfo threadInfo1 = getMockedThreadInfo(0, methodOnce, methodCaller);
        profile.addStackTrace(threadInfo1, true, ThreadType.BasicThreadType.OTHER);
        profile.addStackTrace(getMockedThreadInfo(0, methodTwice), true, ThreadType.BasicThreadType.OTHER);
        profile.addStackTrace(getMockedThreadInfo(0, methodTwice, methodCaller), true, ThreadType.BasicThreadType.OTHER);

        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(threadInfo1));

        Assert.assertEquals(3, profile.getProfileTree(normalizedThreadName).getMethodCount());
    }

    @Test
    public void testAddStackTrace() {
        IProfile profile = getProfile();

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        ThreadInfo threadInfo1 = getMockedThreadInfo(0, stackTrace);
        profile.addStackTrace(threadInfo1, true, ThreadType.BasicThreadType.OTHER);

        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(threadInfo1));

        Assert.assertEquals(stackTrace.length, profile.getProfileTree(normalizedThreadName).getCallSiteCount());
    }

    @Test
    public void veryLargeProfile() throws IOException {
        IProfile profile = getProfile();
        profile.start();

        ProfileTree tree = profile.getProfileTree("ThreadGroupName");
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
                
                methods.add(profile.getProfiledMethodFactory().getProfiledMethod(stack));
                count++;
            }
        }

        int expected = rootCount * Profile.MAX_STACK_DEPTH;
        tree = profile.getProfileTree("ThreadGroupName");
        Assert.assertEquals(expected, tree.getCallSiteCount());

        profile.end();
        Assert.assertEquals(Math.min(Profile.MAX_STACK_SIZE, expected), tree.getCallSiteCount());
    }

    @Test
    public void truncateForCollectorLimit() throws IOException {
        IProfile profile = getProfile();
        profile.start();

        ProfileTree tree = profile.getProfileTree("ThreadGroupName");

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
                methods.add(profile.getProfiledMethodFactory().getProfiledMethod(stack));
                count++;
            }
        }

        int expected = rootCount * Profile.MAX_STACK_DEPTH;
        tree = profile.getProfileTree("ThreadGroupName");
        Assert.assertEquals(expected, tree.getCallSiteCount());

        profile.end();
        Assert.assertEquals(Math.min(Profile.MAX_STACK_SIZE, expected), tree.getCallSiteCount());

        StringWriter writer = new StringWriter();
        profile.writeJSONString(writer);
        Assert.assertTrue(writer.getBuffer().length() <= Profile.MAX_ENCODED_BYTES);
    }

    @Test
    public void truncateForCollectorLimitSimpleCompression() throws IOException {
        turnOnSimpleCompression();

        try {
            IProfile profile = getProfile();
            profile.start();

            ProfileTree tree = profile.getProfileTree("ThreadGroupName");

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
                    methods.add(profile.getProfiledMethodFactory().getProfiledMethod(stack));
                    count++;
                }
            }

            int expected = rootCount * Profile.MAX_STACK_DEPTH;
            tree = profile.getProfileTree("ThreadGroupName");
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

        ProfiledMethod method1 = profile.getProfiledMethodFactory().getProfiledMethod(stack1);
        ProfiledMethod method2 = profile.getProfiledMethodFactory().getProfiledMethod(stack2);
        ProfiledMethod method3 = profile.getProfiledMethodFactory().getProfiledMethod(stack3);

        StackTraceElement[] stack_123 = new StackTraceElement[] { stack1, stack2, stack3 };
        StackTraceElement[] stack_124 = new StackTraceElement[] { stack1, stack2, stack4 };
        StackTraceElement[] stack_125 = new StackTraceElement[] { stack1, stack2, stack5 };
        StackTraceElement[] stack_12567 = new StackTraceElement[] { stack1, stack2, stack5, stack6, stack7 };

        // 6 hits on 1, 6 hits on 2, 3 hits on 3, 1 hit on 4, 2 hits on 5, 1 hits on 6, 1 hit on 7
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_124), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_125), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_12567), true);

        ProfileTree tree = profile.getProfileTree("ThreadGroupName");
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

        ProfiledMethod method1 = profile.getProfiledMethodFactory().getProfiledMethod(stack1);
        ProfiledMethod method2 = profile.getProfiledMethodFactory().getProfiledMethod(stack2);
        ProfiledMethod method5 = profile.getProfiledMethodFactory().getProfiledMethod(stack5);
        ProfiledMethod method6 = profile.getProfiledMethodFactory().getProfiledMethod(stack6);

        StackTraceElement[] stack_1 = new StackTraceElement[] { stack1 };
        StackTraceElement[] stack_12 = new StackTraceElement[] { stack1, stack2 };
        StackTraceElement[] stack_123 = new StackTraceElement[] { stack1, stack2, stack3 };
        StackTraceElement[] stack_1234 = new StackTraceElement[] { stack1, stack2, stack3, stack4 };
        StackTraceElement[] stack_5 = new StackTraceElement[] { stack5 };
        StackTraceElement[] stack_56 = new StackTraceElement[] { stack5, stack6 };
        StackTraceElement[] stack_567 = new StackTraceElement[] { stack5, stack6, stack7 };
        StackTraceElement[] stack_5678 = new StackTraceElement[] { stack5, stack6, stack7, stack8 };

        // 4 hits on 1, 3 hits on 2, 2 hits on 3, 1 hit on 4
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_1), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_12), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_123), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_1234), true);

        // 4 hits on 4, 3 hits on 5, 2 hits on 6, 1 hit on 7
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_5), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_56), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_567), true);
        profile.getProfileTree("ThreadGroupName").addStackTrace(Arrays.asList(stack_5678), true);

        ProfileTree tree = profile.getProfileTree("ThreadGroupName");
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
        ThreadInfo threadInfo = getMockedThreadInfo(0, stackTrace);
        profile.addStackTrace(threadInfo, true, ThreadType.BasicThreadType.OTHER);

        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(threadInfo));
        
        Assert.assertEquals(stackTrace.length, profile.getProfileTree(normalizedThreadName).getCallSiteCount());

        profile.addStackTrace(getMockedThreadInfo(0, stackTrace), true, ThreadType.BasicThreadType.OTHER);

        Assert.assertEquals(stackTrace.length, profile.getProfileTree(normalizedThreadName).getCallSiteCount());

        stackTrace = Thread.currentThread().getStackTrace();
        profile.addStackTrace(getMockedThreadInfo(0, stackTrace), true, ThreadType.BasicThreadType.OTHER);

        Assert.assertEquals(stackTrace.length, profile.getProfileTree(normalizedThreadName).getCallSiteCount(), 2);

        Assert.assertEquals(1, profile.getProfileTree(normalizedThreadName).getRootCount());
    }

    private IProfile treeTestProfile() {
        IProfile profile = getProfile();
        final StackTraceElement[] stack = new StackTraceElement[] { createSimpleStackTrace("c"),
                createSimpleStackTrace("b"), createSimpleStackTrace("a") };
        profile.addStackTrace(getMockedThreadInfo(0, stack), true, ThreadType.BasicThreadType.OTHER);
        profile.addStackTrace(getMockedThreadInfo(0, stack), true, ThreadType.BasicThreadType.OTHER);
        profile.addStackTrace(getMockedThreadInfo(0, stack), true, ThreadType.BasicThreadType.OTHER);
        profile.addStackTrace(getMockedThreadInfo(0, createSimpleStackTrace("d"), createSimpleStackTrace("c"),
                createSimpleStackTrace("b"), createSimpleStackTrace("a")), true, ThreadType.BasicThreadType.OTHER);
        return profile;
    }

    @Test
    public void testTopDownTree() {
        IProfile profile = treeTestProfile();

        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(getMockedThreadInfo(0)));

        verifyTree(profile.getProfileTree(normalizedThreadName), new HashMap<String, Integer>() {

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
        ThreadInfo threadInfo = mock(ThreadInfo.class);
        when(threadInfo.getThreadId()).thenReturn(12L);
        when(threadInfo.getThreadName()).thenReturn("name");

        ThreadMXBean threadMXBean = mock(ThreadMXBean.class);
        when(threadMXBean.getThreadCpuTime(0)).thenReturn(10L);
        when(threadMXBean.getThreadInfo(anyLong(), eq(0))).thenReturn(threadInfo);

        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, true, null, null)
                .setProfilerFormat("v2");
        return new Profile(parameters, TransactionGuidFactory.generate16CharGuid(), threadNameNormalizer, threadMXBean);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteCompressedData() throws IOException, ParseException {
        IProfile profile = createTestProfile();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        profile.getProfileTree("ThreadGroupName").writeJSONString(writer);
        writer.close();

        JSONParser parser = new JSONParser();
        Object parse = parser.parse(out.toString());
        Assert.assertTrue(parse instanceof List);

        List profileSegments = (List) parse;
        Assert.assertEquals(profile.getProfileTree("ThreadGroupName").getRootCount() + 1,
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
        Map trees = (Map) ((Map) parsedData).get("threads");
        Assert.assertEquals(1, trees.size());

        List segments = (List) trees.values().iterator().next();

        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(getMockedThreadInfo(0)));
        
        Assert.assertEquals(profile.getProfileTree(normalizedThreadName).getRootCount() + 1,
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

    private ThreadInfo getMockedThreadInfo(long threadId, StackTraceElement... stackTraceElements) {
        ThreadInfo threadInfo = mock(ThreadInfo.class);

        when(threadInfo.getThreadId()).thenReturn(threadId);
        when(threadInfo.getThreadName()).thenReturn("Thread-" + threadId);
        when(threadInfo.getStackTrace()).thenReturn(stackTraceElements);

        return threadInfo;
    }
}
