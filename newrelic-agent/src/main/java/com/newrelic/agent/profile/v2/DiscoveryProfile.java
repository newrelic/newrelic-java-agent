/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.newrelic.agent.bridge.AgentBridge;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import com.newrelic.agent.threads.BasicThreadInfo;
import com.newrelic.agent.threads.ThreadNameNormalizer;

/**
 * This thing tracks stack traces for invocations of leaf tracers that occur outside
 * of transactions to help identify uninstrumented transactions.
 */
public class DiscoveryProfile implements JSONStreamAware {
    
    /**
     * Thread name (string map key) to a discovery profile tree.
     */
    private final Map<Object, ProfileTree> discoveryProfileTrees;
    private final Function<Object, ProfileTree> discoveryProfileTreeLoader;
    private final ThreadNameNormalizer threadNameNormalizer;
    private final Profile profile;
    
    public DiscoveryProfile(Profile profile, ThreadNameNormalizer threadNameNormalizer) {
        this.threadNameNormalizer = threadNameNormalizer;
        this.profile = profile;
        this.discoveryProfileTreeLoader = threadNameKey -> new ProfileTree(this.profile, false);
        discoveryProfileTrees = AgentBridge.collectionFactory.createCacheWithInitialCapacity(16);
    }

    public void noticeStartTracer(int signatureId) {
        
        String threadName = threadNameNormalizer.getNormalizedThreadName(new BasicThreadInfo(Thread.currentThread()));
        Object key = profile.getStringMap().addString(threadName);

        discoveryProfileTrees.computeIfAbsent(key, discoveryProfileTreeLoader).addStackTrace(getScrubbedCurrentThreadStackTrace(), true);
    }
    
    static List<StackTraceElement> getScrubbedCurrentThreadStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<StackTraceElement> stackTraceList = getScrubbedStackTrace(Arrays.asList(stackTrace));
        Collections.reverse(stackTraceList);
        
        return stackTraceList;
    }

    static List<StackTraceElement> getScrubbedStackTrace(Collection<StackTraceElement> stackTrace) {
        List<StackTraceElement> list = new ArrayList<>(stackTrace.size());
        
        boolean nonAgent = false;
        for (StackTraceElement e : stackTrace) {
            if (nonAgent || 
                    (isNotAgent(e) &&
                            isNotThreadGetStackTrace(e))) {
                nonAgent = true;
                list.add(e);
            }
        }
        
        return list;
    }

    private static boolean isNotAgent(StackTraceElement e) {
        return !e.getClassName().startsWith("com.newrelic.agent");
    }

    private static boolean isNotThreadGetStackTrace(StackTraceElement e) {
        return !("java.lang.Thread".equals(e.getClassName()) &&
                "getStackTrace".equals(e.getMethodName()));
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject.writeJSONString(discoveryProfileTrees, out);
    }

    
}
