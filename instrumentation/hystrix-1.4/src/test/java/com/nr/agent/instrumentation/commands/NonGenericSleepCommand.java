/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.commands;

public class NonGenericSleepCommand extends GenericSleepCommand {

    private long timeToSleep;

    public NonGenericSleepCommand(long sleepTime) {
        super(sleepTime);
        this.timeToSleep = sleepTime;
    }

    @Override
    protected String getFallback() {
        return "Failure: SleepCommand: " + timeToSleep;
    }
}
