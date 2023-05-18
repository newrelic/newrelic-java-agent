/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class ProfilerParameters implements JSONStreamAware {

    private final Long profileId;
    private final Long samplePeriodInMillis;
    private final Long durationInMillis;
    private final boolean onlyRunnableThreads;
    private final boolean onlyRequestThreads;
    private final boolean profileAgentCode;
    private final String keyTransaction;
    private final String appName;
    private String profilerFormat;
    private boolean profileInstrumentation = true;
    private Map<?,?> parameterMap = new HashMap<Object, Object>();

    public ProfilerParameters(Long profileId, long samplePeriodInMillis, long durationInMillis,
            boolean onlyRunnableThreads, boolean onlyRequestThreads, boolean profileAgentCode, String keyTransaction,
            String appName) {
        this.profileId = profileId;
        this.samplePeriodInMillis = samplePeriodInMillis;
        this.durationInMillis = durationInMillis;
        this.onlyRunnableThreads = onlyRunnableThreads;
        this.onlyRequestThreads = onlyRequestThreads;
        this.profileAgentCode = profileAgentCode;
        this.keyTransaction = keyTransaction;
        this.appName = appName;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getSamplePeriodInMillis() {
        return samplePeriodInMillis;
    }

    public Long getDurationInMillis() {
        return durationInMillis;
    }

    public boolean isRunnablesOnly() {
        return onlyRunnableThreads;
    }

    public boolean isOnlyRequestThreads() {
        return onlyRequestThreads;
    }

    public boolean isProfileAgentThreads() {
        return profileAgentCode;
    }

    public boolean isProfileInstrumentation() {
        return profileInstrumentation;
    }
    
    public ProfilerParameters setProfileInstrumentation(Boolean includeTransactions) {
        if (includeTransactions != null) {
            this.profileInstrumentation = includeTransactions;
        }
        return this;
    }

    public String getKeyTransaction() {
        return keyTransaction;
    }

    public String getAppName() {
        return appName;
    }

    public String getProfilerFormat() {
        return profilerFormat;
    }

    public ProfilerParameters setProfilerFormat(String profileFormat) {
        this.profilerFormat = profileFormat == null ? null : profileFormat.trim();
        return this;
    }

    public ProfilerParameters setParameterMap(Map<?, ?> parameterMap) {
        this.parameterMap = parameterMap;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = profileId == null ? result : (prime * result) + profileId.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ProfilerParameters other = (ProfilerParameters) obj;
        return profileId.longValue() == other.profileId.longValue();
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject.writeJSONString(parameterMap, out);
    }

}
