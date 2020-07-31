/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

public abstract class AbstractCommand implements Command {
    private final String commandName;

    public AbstractCommand(String commandName) {
        super();
        this.commandName = commandName;
    }

    @Override
    public final String getName() {
        return commandName;
    }

}
