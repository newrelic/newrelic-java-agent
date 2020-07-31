/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import java.lang.management.ThreadInfo;
import java.util.Set;

import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.profile.ThreadType;
import com.newrelic.agent.util.StringMap;

public interface IProfile extends ProfileData {
    
    static final String CLASSES_KEY = "classes";
    static final String METHODS_KEY = "methods";    
    static final String STRING_MAP_KEY = "string_map";
    static final String AGENT_THREAD_NAMES_KEY = "agent_thread_names";
    static final String INSTRUMENTATION_KEY = "instrumentation";
    static final String THREADS_KEY = "threads";
    static final String VERSION_KEY = "version";
    static final String PROFILE_ARGUMENTS_KEY = "profile_arguments";

    void start();

    void end();

    void beforeSampling();
    
    void addStackTrace(ThreadInfo threadInfo, boolean isRunnable, ThreadType type);

    ProfilerParameters getProfilerParameters();

    int getSampleCount();

    Long getProfileId();

    ProfileTree getProfileTree(String normalizedThreadName);
    
    StringMap getStringMap();
    
    ProfiledMethodFactory getProfiledMethodFactory();

    Set<Long> getThreadIds();

    /*
     * For testing
     */
    int trimBy(int count);

    long getStartTimeMillis();

    long getEndTimeMillis();

    void markInstrumentedMethods();
    
    TransactionProfileSession getTransactionProfileSession();

}