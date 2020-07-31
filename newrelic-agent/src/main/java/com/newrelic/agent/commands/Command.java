/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import java.util.Map;

import com.newrelic.agent.IRPMService;

/**
 * A command takes some action when {@link #process(IRPMService, Map)} is called.
 * 
 * Commands are registered with the {@link CommandParser}.
 */
public interface Command {
    /**
     * The name of this command.
     */
    String getName();

    /**
     * Executes this command. This is called from {@link CommandParser#beforeHarvest} if
     * it receives a command from the rpm service that matches this command.
     */
    Map process(IRPMService rpmService, Map arguments) throws CommandException;
}
