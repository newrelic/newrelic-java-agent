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

    public static final boolean HTTP_ATTR_LEGACY;
    public static final boolean HTTP_ATTR_STANDARD;

    static {
        String attrMode = NewRelic.getAgent().getConfig().getValue("attributes.http_attribute_mode", "both");
        // legacy is only disabled when standard is selected.
        HTTP_ATTR_LEGACY = !"standard".equalsIgnoreCase(attrMode);
        // standard is only disabled when legacy is selected.
        HTTP_ATTR_STANDARD = !"legacy".equalsIgnoreCase(attrMode);
    }
    private GrpcConfig() {
    }

}
