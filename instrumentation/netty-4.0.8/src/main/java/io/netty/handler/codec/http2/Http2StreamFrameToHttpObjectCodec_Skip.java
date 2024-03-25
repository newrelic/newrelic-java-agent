/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http2;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/*
 * Stops this instrumentation from applying when the netty-4.1.16 module applies
 */
@SkipIfPresent(originalName = "io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec")
public class Http2StreamFrameToHttpObjectCodec_Skip {
}
