/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.CommandException;

public class StartProfilerCommandTest {

    private StartProfilerCommand startProfilerCommand;
    private ProfilerParameters profilerParameters;

    @Before
    public void setup() {
        ProfilerControl profilerControl = new ProfilerControl() {

            @Override
            public void startProfiler(ProfilerParameters parameters) {
                profilerParameters = parameters;
            }

            @Override
            public int stopProfiler(Long profileId, boolean report) {
                return 0;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

        };
        startProfilerCommand = new StartProfilerCommand(profilerControl);
    }

    @Test(expected = CommandException.class)
    public void noArguments() throws CommandException {
        IRPMService rpmService = null;
        startProfilerCommand.process(rpmService, Collections.EMPTY_MAP);
    }

    @Test(expected = CommandException.class)
    public void missingSamplePeriod() throws CommandException {
        IRPMService rpmService = null;
        Map<String, Object> args = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("profile_id", 1L);
                put("duration", 1);
            }
        };
        startProfilerCommand.process(rpmService, args);
    }

    @Test(expected = CommandException.class)
    public void badSamplePeriod() throws CommandException {
        IRPMService rpmService = null;
        Map<String, Object> args = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("profile_id", 1L);
                put("duration", 120.0f);
                put("sample_period", "dog");
            }
        };
        startProfilerCommand.process(rpmService, args);
    }

    @Test(expected = CommandException.class)
    public void samplePeriodGreaterThanDuration() throws CommandException {
        IRPMService rpmService = null;
        Map<String, Object> args = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("profile_id", 1L);
                put("duration", 1000L);
                put("sample_period", 2000L);
            }
        };
        startProfilerCommand.process(rpmService, args);
    }

    @Test
    public void validRequiredArgs() throws CommandException {
        IRPMService rpmService = null;
        final long profileId = 1L;
        final long duration = 60000L;
        final long samplePeriod = 500L;
        Map<String, Object> args = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("profile_id", profileId);
                put("sample_period", samplePeriod);
                put("duration", duration);
            }
        };
        startProfilerCommand.process(rpmService, args);

        Assert.assertEquals(profileId, profilerParameters.getProfileId().longValue());
        Assert.assertEquals(duration * 1000L, profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals(samplePeriod * 1000L, profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals(false, profilerParameters.isRunnablesOnly());
        Assert.assertEquals(false, profilerParameters.isOnlyRequestThreads());
        Assert.assertEquals(false, profilerParameters.isProfileAgentThreads());
        Assert.assertNull(profilerParameters.getKeyTransaction());
    }

    @Test
    public void validArgsThreadProfilerV2NullDefaults() throws CommandException {
        IRPMService rpmService = null;
        final long profileId = 1L;
        final long duration = 60000L;
        final long samplePeriod = 500L;
        final boolean onlyRunnableThreads = true;
        final boolean onlyRequestThreads = true;
        final String profilerFormat = "v2";
        final Object profileAgentCode = null;
        Map<String, Object> args = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("profile_id", profileId);
                put("sample_period", samplePeriod);
                put("duration", duration);
                put("only_runnable_threads", onlyRunnableThreads);
                put("only_request_threads", onlyRequestThreads);
                put("profiler_format", profilerFormat);
                put("profile_agent_code", profileAgentCode);
            }
        };
        startProfilerCommand.process(rpmService, args);

        Assert.assertEquals(duration * 1000L, profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals(samplePeriod * 1000L, profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals(profileId, profilerParameters.getProfileId().longValue());
        Assert.assertEquals(onlyRunnableThreads, profilerParameters.isRunnablesOnly());
        Assert.assertEquals(onlyRequestThreads, profilerParameters.isOnlyRequestThreads());
        Assert.assertEquals(false, profilerParameters.isProfileAgentThreads());
        Assert.assertEquals(profilerFormat, profilerParameters.getProfilerFormat());
        Assert.assertNull(profilerParameters.getKeyTransaction());
    }

    @Test
    public void validArgs() throws CommandException {
        IRPMService rpmService = null;
        final long profileId = 1L;
        final long duration = 60000L;
        final long samplePeriod = 500L;
        final boolean onlyRunnableThreads = true;
        final boolean onlyRequestThreads = true;
        final boolean profileAgentCode = true;
        Map<String, Object> args = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("profile_id", profileId);
                put("sample_period", samplePeriod);
                put("duration", duration);
                put("only_runnable_threads", onlyRunnableThreads);
                put("only_request_threads", onlyRequestThreads);
                put("profile_agent_code", profileAgentCode);
            }
        };
        startProfilerCommand.process(rpmService, args);

        Assert.assertEquals(duration * 1000L, profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals(samplePeriod * 1000L, profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals(profileId, profilerParameters.getProfileId().longValue());
        Assert.assertEquals(onlyRunnableThreads, profilerParameters.isRunnablesOnly());
        Assert.assertEquals(onlyRequestThreads, profilerParameters.isOnlyRequestThreads());
        Assert.assertEquals(profileAgentCode, profilerParameters.isProfileAgentThreads());
        Assert.assertNull(profilerParameters.getKeyTransaction());
    }

    @Test
    public void validArgsNullDefault() throws CommandException {
        IRPMService rpmService = null;
        final long profileId = 1L;
        final long duration = 60000L;
        final long samplePeriod = 500L;
        final boolean onlyRunnableThreads = true;
        final boolean onlyRequestThreads = true;
        final Object profileAgentCode = null;
        Map<String, Object> args = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("profile_id", profileId);
                put("sample_period", samplePeriod);
                put("duration", duration);
                put("only_runnable_threads", onlyRunnableThreads);
                put("only_request_threads", onlyRequestThreads);
                put("profile_agent_code", profileAgentCode);
            }
        };
        startProfilerCommand.process(rpmService, args);

        Assert.assertEquals(duration * 1000L, profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals(samplePeriod * 1000L, profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals(profileId, profilerParameters.getProfileId().longValue());
        Assert.assertEquals(onlyRunnableThreads, profilerParameters.isRunnablesOnly());
        Assert.assertEquals(onlyRequestThreads, profilerParameters.isOnlyRequestThreads());
        Assert.assertEquals(false, profilerParameters.isProfileAgentThreads());
        Assert.assertNull(profilerParameters.getKeyTransaction());
    }
}
