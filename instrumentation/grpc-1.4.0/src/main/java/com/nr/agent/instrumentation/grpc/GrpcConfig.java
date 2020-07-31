/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.grpc;

import com.newrelic.api.agent.NewRelic;

public class GrpcConfig {

    public static final boolean disributedTracingEnabled = NewRelic.getAgent().getConfig().getValue("grpc.distributed_tracing.enabled", true);

    public static final boolean errorsEnabled = NewRelic.getAgent().getConfig().getValue("grpc.errors.enabled", true);

    private GrpcConfig() {
    }

}
