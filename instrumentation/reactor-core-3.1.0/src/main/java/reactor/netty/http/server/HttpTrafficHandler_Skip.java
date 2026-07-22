/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.http.server;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// prevents this module from applying when reactor-netty 0.8.0 is present
@SkipIfPresent(originalName = "reactor.netty.http.server.HttpTrafficHandler")
class HttpTrafficHandler_Skip {
}
