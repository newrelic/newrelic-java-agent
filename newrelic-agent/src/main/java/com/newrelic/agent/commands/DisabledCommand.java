/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class DisabledCommand extends AbstractCommand {

    private final String errorMessage;

    public DisabledCommand(String name) {
        this(name, MessageFormat.format("Command \"{0}\" is disabled", name));
    }

    public DisabledCommand(String name, String errorMessage) {
        super(name);
        this.errorMessage = errorMessage;
    }

    @Override
    public Map process(IRPMService rpmService, Map arguments) throws CommandException {
        Agent.LOG.log(Level.INFO, errorMessage);
        Map<String, String> map = new HashMap<>();
        map.put("error", errorMessage);

        return map;
    }

}
