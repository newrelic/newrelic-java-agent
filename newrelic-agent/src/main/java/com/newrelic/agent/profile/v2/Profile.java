/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.profile.ThreadType;
import com.newrelic.agent.profile.method.MethodInfoFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.threads.BasicThreadInfo;
import com.newrelic.agent.threads.ThreadNameNormalizer;
import com.newrelic.agent.transport.DataSenderWriter;
import com.newrelic.agent.util.StackTraces;
import com.newrelic.agent.util.StringMap;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.zip.Deflater;

/**
 * Execution profile over a time period
 */
public class Profile implements IProfile {

    public static final int MAX_STACK_DEPTH = 300;
    /*
     * Since the Collector has a 1000000 byte limit on the Content-Length in the HttpServletRequest, restrict the size
     * of the profile to 60000 stack elements (~750000 bytes).
     */
    public static final int MAX_STACK_SIZE = 60000;

    /**
     * Collector has a 1000000 byte limit on the Content-Length in the HttpServletRequest
     */
    public static final int MAX_ENCODED_BYTES = 1000000;

    /**
     * The maximum size of the JSON payload itself (excluding data) is 114 bytes (4 Longs + 4 Integers +
     * Transaction GUID). This means that we need to take the collector max (1MB) and subtract our payload size to
     * figure out how much data we can include before potentially going over the limit.
     */
    public static final int MAX_ENCODED_DATA_BYTES = MAX_ENCODED_BYTES - 144;

    /**
     * The amount to trim a stack size by if as long as its encoded size is greater than MAX_ENCODED_BYTES.
     */
    public static final int STACK_TRIM = 10000;
    private static final int JSON_VERSION = 2;

    private long startTimeMillis = 0;
    private long endTimeMillis = 0;
    private int sampleCount = 0;
    private int totalThreadCount = 0;
    private int runnableThreadCount = 0;
    private final ThreadMXBean threadMXBean;

    /**
     * Cache for thread CPU times. Initialized in constructor after threadMXBean is assigned.
     */
    private final Map<Long, Long> startThreadCpuTimes;
    private final Function<Long, Long> startThreadCpuTimeLoader;

    private final ProfilerParameters profilerParameters;

    private final Map<Long, ProfileTree> threadIdToProfileTrees = new HashMap<>();

    /**
     * Cache for profile trees. Initialized in constructor after threadMXBean is assigned.
     */
    private final Map<String, ProfileTree> profileTrees;
    private final Function<String, ProfileTree> profileTreeLoader;
    
    private final StringMap stringMap = new Murmur3StringMap();
    private final ProfiledMethodFactory profiledMethodFactory;
    private final ThreadNameNormalizer threadNameNormalizer;
    private final TransactionProfileSession transactionProfileSession;
    private final String sessionId;

    public Profile(ProfilerParameters parameters, String sessionId, ThreadNameNormalizer threadNameNormalizer) {
        this(parameters, sessionId, threadNameNormalizer, ManagementFactory.getThreadMXBean());
    }

    // Package-private for testing
    Profile(ProfilerParameters parameters, String sessionId, ThreadNameNormalizer threadNameNormalizer, ThreadMXBean threadMXBean) {
        this.profilerParameters = parameters;
        this.sessionId = sessionId;
        this.threadNameNormalizer = threadNameNormalizer;

        // Initialize threadMXBean FIRST before creating loaders that reference it
        this.threadMXBean = threadMXBean;

        // THEN create loaders that safely reference threadMXBean
        this.startThreadCpuTimeLoader = threadId -> this.threadMXBean.getThreadCpuTime(threadId);
        this.profileTreeLoader = createCacheLoader(true);

        // THEN create caches using the factory
        this.startThreadCpuTimes = AgentBridge.collectionFactory.createCacheWithInitialCapacity(16);
        this.profileTrees = AgentBridge.collectionFactory.createCacheWithInitialCapacity(16);

        profiledMethodFactory = new ProfiledMethodFactory(this);
        if (parameters.isProfileInstrumentation()) {
            transactionProfileSession = new TransactionProfileSessionImpl(this, threadNameNormalizer);
        } else {
            transactionProfileSession = TransactionProfileSessionImpl.NO_OP_TRANSACTION_PROFILE_SESSION;
        }
    }
    
    Function<String, ProfileTree> createCacheLoader(final boolean reportCpu) {
        return key -> new ProfileTree(Profile.this, reportCpu);
    }

    private Map<Long, Long> getThreadCpuTimes() {        
        if (!(threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled())) {
            return Collections.emptyMap();
        }

        HashMap<Long, Long> cpuTimes = new HashMap<>();
        for (long id : threadMXBean.getAllThreadIds()) {
            cpuTimes.put(id, threadMXBean.getThreadCpuTime(id));
        }
        return cpuTimes;
    }

    @Override
    public ProfileTree getProfileTree(String normalizedThreadName) {
        return profileTrees.computeIfAbsent(normalizedThreadName, profileTreeLoader);
    }

    /**
     * Subclasses may override.
     */
    @Override
    public void start() {
        startTimeMillis = System.currentTimeMillis();


        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if (!threadMXBean.isThreadCpuTimeSupported()) {
            Agent.LOG.info("Profile unable to record CPU time: Thread CPU time measurement is not supported");
        } else if (!threadMXBean.isThreadCpuTimeEnabled()) {
            Agent.LOG.info("Profile unable to record CPU time: Thread CPU time measurement is not enabled");
        } else {
            startThreadCpuTimes.putAll(getThreadCpuTimes());
        }
    }

    /**
     * Subclasses may override.
     */
    @Override
    public void end() {
        endTimeMillis = System.currentTimeMillis();

        Map<Long, Long> endThreadCpuTimes = getThreadCpuTimes();

        for (Entry<Long, Long> entry : endThreadCpuTimes.entrySet()) {
            Long startTime = startThreadCpuTimes.get(entry.getKey());
            if (startTime == null) {
                startTime = 0l;
            }
            long cpuTime = TimeUnit.MILLISECONDS.convert(entry.getValue() - startTime, TimeUnit.NANOSECONDS);

            ProfileTree tree = threadIdToProfileTrees.get(entry.getKey());
            if (null != tree) {
                tree.incrementCpuTime(cpuTime);
            } else {
                // maybe log at finest?  We are excluding stuff...
            }
        }

        int stackCount = getCallSiteCount();
        String msg = MessageFormat.format("Profile size is {0} stack elements", stackCount);
        Agent.LOG.info(msg);
        if (stackCount > MAX_STACK_SIZE) {
            Agent.LOG.info(MessageFormat.format("Trimmed profile size by {0} stack elements", trim(stackCount
                    - MAX_STACK_SIZE, stackCount)));
        }
    }
    
    

    @Override
    public Set<Long> getThreadIds() {
        return threadIdToProfileTrees.keySet();
    }

    /**
     * Use the loaded classes to mark all of the {@link ProfiledMethod}s which are instrumented using our method
     * annotations.
     * 
     * @see InstrumentedClass
     * @see InstrumentedMethod
     */
    @Override
    public void markInstrumentedMethods() {
        try {
            doMarkInstrumentedMethods();
        } catch (Throwable ex) {
            String msg = MessageFormat.format("Error marking instrumented methods {0}", ex);
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, ex);
            } else {
                Agent.LOG.finer(msg);
            }
        }
    }

    private void doMarkInstrumentedMethods() {
        MethodInfoFactory methodInfoFactory = new MethodInfoFactory();
        profiledMethodFactory.setMethodDetails(methodInfoFactory);
    }

    /**
     * For testing.
     */
    @Override
    public int trimBy(int limit) {
        return trim(limit, getCallSiteCount());
    }

    /**
     * Reduce the size of the profile by removing segments with the lowest call count (first priority) and highest depth
     * in the tree (second priority).
     * 
     * @param limit the maximum number of segments to remove
     */
    private int trim(int limit, int stackCount) {
        ProfileSegmentSort[] segments = getSortedSegments(stackCount);
        int count = 0;
        for (ProfileSegmentSort segment : segments) {
            if (count >= limit) {
                break;
            }
            segment.remove();
            count++;
        }
        return count;
    }

    /**
     * Get a sorted array of all segments in the profile.
     */
    private ProfileSegmentSort[] getSortedSegments(int stackCount) {
        ProfileSegmentSort[] segments = new ProfileSegmentSort[stackCount];
        int index = 0;
        for (ProfileTree profileTree : profileTrees.values()) {
            for (ProfileSegment rootSegment : profileTree.getRootSegments()) {
                index = addSegment(rootSegment, null, 1, segments, index);
            }
        }
        Arrays.sort(segments);
        return segments;
    }

    private int addSegment(ProfileSegment segment, ProfileSegment parent, int depth, ProfileSegmentSort[] segments,
            int index) {
        ProfileSegmentSort segSort = new ProfileSegmentSort(segment, parent, depth);
        segments[index++] = segSort;
        for (ProfileSegment child : segment.getChildren()) {
            index = addSegment(child, segment, ++depth, segments, index);
        }
        return index;
    }

    /**
     * Get the number of distinct method invocation nodes in the profile.
     */
    private int getCallSiteCount() {
        int count = 0;
        for (ProfileTree profileTree : profileTrees.values()) {
            count += profileTree.getCallSiteCount();
        }
        return count;
    }

    @Override
    public StringMap getStringMap() {
        return stringMap;
    }

    @Override
    public ProfiledMethodFactory getProfiledMethodFactory() {
        return profiledMethodFactory;
    }

    @Override
    public Long getProfileId() {
        return profilerParameters.getProfileId();
    }

    @Override
    public ProfilerParameters getProfilerParameters() {
        return profilerParameters;
    }

    @Override
    public void beforeSampling() {
        sampleCount++;
    }

    @Override
    public int getSampleCount() {
        return sampleCount;
    }

    @Override
    public final long getStartTimeMillis() {
        return startTimeMillis;
    }

    @Override
    public final long getEndTimeMillis() {
        return endTimeMillis;
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        // xraySessionId is a deprecated Array member, but it must be passed
        // to retain sequencing.
        final Object xraySessionId = null;

        JSONArray.writeJSONString(Arrays.asList(profilerParameters.getProfileId(), startTimeMillis, endTimeMillis,
                    sampleCount, getData(out), totalThreadCount, runnableThreadCount,
                    xraySessionId, sessionId, Integer.toString(JSON_VERSION)), out);
        
    }
    
    private Map<String, Object> getJsonMap() {
        Map<String, Object> data = new HashMap<>();
        data.put(PROFILE_ARGUMENTS_KEY, this.profilerParameters);
        data.put(VERSION_KEY, JSON_VERSION);
        data.put(THREADS_KEY, profileTrees);
        data.put(INSTRUMENTATION_KEY, transactionProfileSession);
        data.put(AGENT_THREAD_NAMES_KEY, getAgentThreadNames());
        data.put(METHODS_KEY, profiledMethodFactory.getMethods());
        data.put(CLASSES_KEY, profiledMethodFactory.getClasses());
        data.put(STRING_MAP_KEY, stringMap.getStringMap());
        return data;
    }

    private Object getData(Writer out) {
        Map<String, Object> data = getJsonMap();

        Object result = DataSenderWriter.getJsonifiedOptionallyCompressedEncodedString(data, out, Deflater.BEST_SPEED,
                MAX_ENCODED_DATA_BYTES);

        // trim if necessary until encoded/compressed size is under the collector's threshold
        int maxStack = MAX_STACK_SIZE;
        while (result == null && maxStack > 0) {
            maxStack -= STACK_TRIM;
            int stackCount = getCallSiteCount();
            trim(stackCount - maxStack, stackCount);
            result = DataSenderWriter.getJsonifiedOptionallyCompressedEncodedString(data, out,
                    Deflater.BEST_SPEED, MAX_ENCODED_DATA_BYTES);
        }

        if (result != null && DataSenderWriter.isCompressingWriter(out)) {
            String msg = MessageFormat.format("Profile v2 serialized size = {0} bytes", result.toString().length());
            Agent.LOG.info(msg);
        }
        return result;
    }

    private Collection<Object> getAgentThreadNames() {
        Set<Long> agentThreadIds = ServiceFactory.getThreadService().getAgentThreadIds();
        Set<Object> names = new HashSet<>();
        
        for (long id : agentThreadIds) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(id, 0);
            names.add(stringMap.addString(threadNameNormalizer.getNormalizedThreadName(
                    new BasicThreadInfo(threadInfo))));
        }
        
        return new ArrayList<>(names);
    }

    private void incrementThreadCounts(boolean runnable) {
        totalThreadCount++;
        if (runnable) {
            runnableThreadCount++;
        }
    }

    private boolean shouldScrubStack(ThreadType type) {
        if (ThreadType.BasicThreadType.AGENT.equals(type)) {
            return false;
        }
        if (profilerParameters.isProfileAgentThreads()) {
            return false;
        }
        return true;
    }

    /**
     * Subclasses may override.
     */
    @Override
    public void addStackTrace(ThreadInfo threadInfo, boolean runnable, ThreadType type) {
        addStackTrace(new BasicThreadInfo(threadInfo), threadInfo.getStackTrace(),  runnable, type);
    }

    void addStackTrace(BasicThreadInfo threadInfo, StackTraceElement[] stackTrace, boolean runnable, ThreadType type) {
        if (stackTrace.length < 2) {
            return;
        }

        // make sure this thread is in our start time cache (auto-load if not present)
        startThreadCpuTimes.computeIfAbsent(threadInfo.getId(), startThreadCpuTimeLoader);

        incrementThreadCounts(runnable);

        List<StackTraceElement> stackTraceList;
        if (shouldScrubStack(type)) {
            stackTraceList = StackTraces.scrubAndTruncate(Arrays.asList(stackTrace), 0);
        } else {
            stackTraceList = Arrays.asList(stackTrace);
        }
        List<StackTraceElement> result = new ArrayList<>(stackTraceList);

        // the stack traces we get start with the leaves, not the roots. flip them
        Collections.reverse(result);

        String normalizedThreadName = threadNameNormalizer.getNormalizedThreadName(threadInfo);
        ProfileTree profileTree = getProfileTree(normalizedThreadName);
        threadIdToProfileTrees.put(threadInfo.getId(), profileTree);
        profileTree.addStackTrace(result, runnable);
    }

    /**
     * A class to sort profile segments in order of lowest runnable call count (first) and highest depth in the stack
     * (second).
     * 
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    private static class ProfileSegmentSort implements Comparable<ProfileSegmentSort> {

        private final ProfileSegment segment;
        private final ProfileSegment parent;
        private final int depth;

        private ProfileSegmentSort(ProfileSegment segment, ProfileSegment parent, int depth) {
            super();
            this.segment = segment;
            this.parent = parent;
            this.depth = depth;
        }

        void remove() {
            if (parent != null) {
                parent.removeChild(segment.getMethod());
            }
        }

        @Override
        public String toString() {
            return segment.toString();
        }

        @Override
        public int compareTo(ProfileSegmentSort other) {
            int thisCount = segment.getRunnableCallCount();
            int otherCount = other.segment.getRunnableCallCount();
            if (thisCount == otherCount) {
                return (depth > other.depth ? -1 : (depth == other.depth ? 0 : 1));
            }
            return thisCount > otherCount ? 1 : -1;
        }
    }

    @Override
    public TransactionProfileSession getTransactionProfileSession() {
        return transactionProfileSession;
    }

}
