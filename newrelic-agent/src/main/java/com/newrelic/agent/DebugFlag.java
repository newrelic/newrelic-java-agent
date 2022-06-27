/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.concurrent.atomic.AtomicBoolean;

public class DebugFlag {
    public static final AtomicBoolean tokenEnabled = new AtomicBoolean();

    // This replaces a previous check that duplicated code in several places. We need to check the debug flag in AgentJarHelper before the Agent class has been loaded.
    // This flag cannot be set via newrelic.yml (AgentConfigImpl) because a ServiceManager and ConfigService have not been initialized for the earliest checks
    // for the debug setting.
    public static final boolean DEBUG = Boolean.getBoolean("newrelic.debug") || Boolean.parseBoolean(System.getenv("NEWRELIC_DEBUG"));
            ;


}
