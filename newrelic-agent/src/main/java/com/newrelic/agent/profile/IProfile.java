/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

public interface IProfile extends ProfileData {

    void start();

    void end();

    void beforeSampling();

    void addStackTrace(long threadId, boolean runnable, ThreadType type, StackTraceElement... stackTrace);

    ProfilerParameters getProfilerParameters();

    int getSampleCount();

    Long getProfileId();

    ProfileTree getProfileTree(ThreadType threadType);

    /*
     * For testing
     */
    int trimBy(int count);

    long getStartTimeMillis();

    long getEndTimeMillis();

    void markInstrumentedMethods();

}