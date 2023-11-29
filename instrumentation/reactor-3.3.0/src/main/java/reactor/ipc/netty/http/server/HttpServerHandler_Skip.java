/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.ipc.netty.http.server;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// prevents this module from applying when reactor-netty 0.7.0 is present
@SkipIfPresent(originalName = "reactor.ipc.netty.http.server.HttpServerHandler")
class HttpServerHandler_Skip {
}
