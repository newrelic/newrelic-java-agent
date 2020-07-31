/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.core.CoreService;

import java.util.Collections;
import java.util.Map;

public class ShutdownCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "shutdown";

    private final CoreService coreService;

    public ShutdownCommand(CoreService coreService) {
        super(COMMAND_NAME);
        this.coreService = coreService;
    }

    @Override
    public Map process(IRPMService rpmService, Map arguments) throws CommandException {
        Agent.LOG.info("ShutdownCommand is shutting down the Agent");
        coreService.shutdownAsync();
        return Collections.EMPTY_MAP;
    }

}
