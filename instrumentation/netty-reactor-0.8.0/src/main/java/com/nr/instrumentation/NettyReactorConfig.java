/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.api.agent.NewRelic;

public class NettyReactorConfig {
    public static final boolean errorsEnabled = NewRelic.getAgent().getConfig()
            .getValue("reactor-netty.errors.enabled", false);

    private NettyReactorConfig() {
    }
}
