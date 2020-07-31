/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.IRPMService;

public class PingCommand implements Command {
    public static final String COMMAND_NAME = "ping";

    @Override
    public Map process(IRPMService rpmService, Map arguments) throws CommandException {
        return Collections.EMPTY_MAP;
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

}
