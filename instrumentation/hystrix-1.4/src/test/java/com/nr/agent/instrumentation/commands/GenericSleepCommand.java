/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.commands;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class GenericSleepCommand extends HystrixCommand<String> {

    private long timeToSleep;

    public GenericSleepCommand(long sleepTime) {
        super(HystrixCommandGroupKey.Factory.asKey("Sleeping"));
        timeToSleep = sleepTime;
    }

    @Override
    protected String run() throws Exception {
        Thread.sleep(timeToSleep);
        return "GenericSleepCommand: Slept for " + timeToSleep;
    }

}