/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http2;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

/*
 * This class is weaved to prevent this instrumentation module from applying to
 * 4.1.x versions prior to io.netty:netty-all:4.1.16.Final, as the HTTP/2
 * APIs were too unstable in those earlier versions.
 */
@Weave(type = MatchType.ExactClass, originalName = "io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec")
public class Http2StreamFrameToHttpObjectCodec_Instrumentation {

}
