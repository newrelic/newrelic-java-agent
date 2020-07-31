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

public class RestartCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "restart";

    public RestartCommand() {
        super(COMMAND_NAME);
    }

    @Override
    public Map process(final IRPMService rpmService, Map arguments) throws CommandException {
        rpmService.reconnect();
        return Collections.EMPTY_MAP;
    }

}
