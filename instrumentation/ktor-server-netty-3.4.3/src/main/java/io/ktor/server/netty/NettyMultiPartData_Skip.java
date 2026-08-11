/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.server.netty;

import com.newrelic.api.agent.weaver.SkipIfPresent;

@SkipIfPresent(originalName = "io.ktor.server.netty.NettyMultiPartData")
public class NettyMultiPartData_Skip {
}
