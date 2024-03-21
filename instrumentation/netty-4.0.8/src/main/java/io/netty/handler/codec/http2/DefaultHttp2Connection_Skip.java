package io.netty.handler.codec.http2;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/*
 * Stops this instrumentation from applying to versions
 * io.netty:netty-all:4.1.0.Beta4 and above that support HTTP/2
 */
@SkipIfPresent(originalName = "io.netty.handler.codec.http2.DefaultHttp2Connection")
public class DefaultHttp2Connection_Skip {
}
