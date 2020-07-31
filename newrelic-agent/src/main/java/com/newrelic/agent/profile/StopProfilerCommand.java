/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.AbstractCommand;
import com.newrelic.agent.commands.CommandException;

public class StopProfilerCommand extends AbstractCommand {
    public static final String COMMAND_NAME = "stop_profiler";
    private final ProfilerControl profilerControl;

    public StopProfilerCommand(ProfilerControl profilerControl) {
        super(COMMAND_NAME);
        this.profilerControl = profilerControl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map process(IRPMService rpmService, Map arguments) throws CommandException {
        if (arguments.size() != 2) {
            throw new CommandException("The stop_profiler command expected 2 arguments");
        }
        Object report = arguments.get("report_data"), profileId = arguments.get("profile_id");

        if (!(profileId instanceof Number)) {
            throw new CommandException("The start_profiler command encountered an invalid profile id: " + profileId);
        }

        if (!(report instanceof Boolean)) {
            throw new CommandException("The start_profiler command encountered an invalid report_data parameter: "
                    + report);
        }

        profilerControl.stopProfiler(((Number) profileId).longValue(), (Boolean) report);
        return Collections.EMPTY_MAP;
    }

}
