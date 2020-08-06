/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

abstract class AbstractCliCommand {
    public abstract String getLongDescription();

    public Options getOptions() {
        return new Options();
    }

    public abstract void performCommand(CommandLine parsedOptions) throws Exception;
}
