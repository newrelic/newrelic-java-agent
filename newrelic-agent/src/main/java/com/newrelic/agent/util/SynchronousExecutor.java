/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.concurrent.Executor;

/**
 * An executor that synchronously runs tasks.
 */
public class SynchronousExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
        command.run();
    }

}
