/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.AbstractCommand;
import com.newrelic.agent.commands.CommandException;
import com.newrelic.agent.util.TimeConversion;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StartProfilerCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "start_profiler";

    private static final String DISABLED_MESSAGE = "The profiler service is disabled";
    private static final String DURATION = "duration";
    private static final String SAMPLE_PERIOD = "sample_period";
    private static final String PROFILE_ID = "profile_id";
    private static final String ONLY_RUNNABLE_THREADS = "only_runnable_threads";
    private static final String ONLY_REQUEST_THREADS = "only_request_threads";
    private static final String PROFILE_AGENT_CODE = "profile_agent_code";
    private static final String PROFILER_FORMAT = "profiler_format";
    private static final String PROFILE_INSTRUMENTATION = "profile_instrumentation";

    private static final boolean DEFAULT_ONLY_RUNNABLE_THREADS = false;
    private static final boolean DEFAULT_ONLY_REQUEST_THREADS = false;

    private final ProfilerControl profilerControl;

    public StartProfilerCommand(ProfilerControl profilerControl) {
        super(COMMAND_NAME);
        this.profilerControl = profilerControl;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<?, ?> process(IRPMService rpmService, Map arguments) throws CommandException {
        if (profilerControl.isEnabled()) {
            return processEnabled(rpmService, arguments);
        }
        return processDisabled(rpmService, arguments);
    }

    public Map<?, ?> processEnabled(IRPMService rpmService, Map<?, ?> arguments) throws CommandException {
        ProfilerParameters parameters = createProfilerParameters(arguments);
        profilerControl.startProfiler(parameters);
        return Collections.EMPTY_MAP;
    }

    public Map<?, ?> processDisabled(IRPMService rpmService, Map<?, ?> arguments) throws CommandException {
        Agent.LOG.info(DISABLED_MESSAGE);
        Map<String, String> result = new HashMap<>();
        result.put("error", DISABLED_MESSAGE);
        return result;
    }

    private ProfilerParameters createProfilerParameters(Map<?, ?> arguments) throws CommandException {
        Map<?,?> argCopy = new HashMap<>(arguments);
        long profileId = getProfileId(arguments);

        double samplePeriod = getSamplePeriod(arguments);
        double duration = getDuration(arguments);
        if (samplePeriod > duration) {
            String msg = MessageFormat.format("{0} > {1} in start_profiler command: {2} > {3}", SAMPLE_PERIOD,
                    DURATION, samplePeriod, duration);
            throw new CommandException(msg);
        }
        long samplePeriodInMillis = TimeConversion.convertSecondsToMillis(samplePeriod);
        long durationInMillis = TimeConversion.convertSecondsToMillis(duration);

        boolean onlyRunnableThreads = getOnlyRunnableThreads(arguments);
        boolean onlyRequestThreads = getOnlyRequestThreads(arguments);
        boolean profileAgentCode = getProfileAgentCode(arguments);

        // Profiler v2 commands
        String profilerFormat = (String)arguments.remove(PROFILER_FORMAT);
        Boolean profileInstrumentation = (Boolean)arguments.remove(PROFILE_INSTRUMENTATION);

        if (arguments.size() > 0) {
            String msg = MessageFormat.format("Unexpected arguments in start_profiler command: {0}",
                    arguments.keySet().toString());
            Agent.LOG.warning(msg);
        }

        return new ProfilerParameters(profileId, samplePeriodInMillis, durationInMillis, onlyRunnableThreads,
                onlyRequestThreads, profileAgentCode, null, null).setProfilerFormat(profilerFormat).setProfileInstrumentation(profileInstrumentation).setParameterMap(argCopy);
    }

    private long getProfileId(Map<?, ?> arguments) throws CommandException {
        Object profileId = arguments.remove(PROFILE_ID);
        if (profileId instanceof Number) {
            return ((Number) profileId).longValue();
        } else if (profileId == null) {
            String msg = MessageFormat.format("Missing {0} in start_profiler command", PROFILE_ID);
            throw new CommandException(msg);
        } else {
            String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}", PROFILE_ID, profileId);
            throw new CommandException(msg);
        }
    }

    private double getSamplePeriod(Map<?, ?> arguments) throws CommandException {
        Object samplePeriod = arguments.remove(SAMPLE_PERIOD);
        if (samplePeriod instanceof Number) {
            return ((Number) samplePeriod).doubleValue();
        } else if (samplePeriod == null) {
            String msg = MessageFormat.format("Missing {0} in start_profiler command", SAMPLE_PERIOD);
            throw new CommandException(msg);
        } else {
            String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}", SAMPLE_PERIOD, samplePeriod);
            throw new CommandException(msg);
        }
    }

    private double getDuration(Map<?, ?> arguments) throws CommandException {
        Object duration = arguments.remove(DURATION);
        if (duration instanceof Number) {
            return ((Number) duration).doubleValue();
        } else if (duration == null) {
            String msg = MessageFormat.format("Missing {0} in start_profiler command", DURATION);
            throw new CommandException(msg);
        } else {
            String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}", DURATION, duration);
            throw new CommandException(msg);
        }
    }

    private boolean getOnlyRunnableThreads(Map<?, ?> arguments) throws CommandException {
        Object onlyRunnableThreads = arguments.remove(ONLY_RUNNABLE_THREADS);
        if (onlyRunnableThreads instanceof Boolean) {
            return (Boolean) onlyRunnableThreads;
        } else if (onlyRunnableThreads == null) {
            return DEFAULT_ONLY_RUNNABLE_THREADS;
        } else {
            String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}", ONLY_RUNNABLE_THREADS,
                    onlyRunnableThreads);
            throw new CommandException(msg);
        }
    }

    private boolean getOnlyRequestThreads(Map<?, ?> arguments) throws CommandException {
        Object onlyRequestThreads = arguments.remove(ONLY_REQUEST_THREADS);
        if (onlyRequestThreads instanceof Boolean) {
            return (Boolean) onlyRequestThreads;
        } else if (onlyRequestThreads == null) {
            return DEFAULT_ONLY_REQUEST_THREADS;
        } else {
            String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}", ONLY_REQUEST_THREADS,
                    onlyRequestThreads);
            throw new CommandException(msg);
        }
    }

    private boolean getProfileAgentCode(Map<?, ?> arguments) throws CommandException {
        Object profileAgentCode = arguments.remove(PROFILE_AGENT_CODE);
        if (profileAgentCode instanceof Boolean) {
            return (Boolean) profileAgentCode;
        } else if (profileAgentCode == null) {
            return Agent.isDebugEnabled();
        } else {
            String msg = MessageFormat.format("Invalid {0} in start_profiler command: {1}", PROFILE_AGENT_CODE,
                    profileAgentCode);
            throw new CommandException(msg);
        }
    }

}
