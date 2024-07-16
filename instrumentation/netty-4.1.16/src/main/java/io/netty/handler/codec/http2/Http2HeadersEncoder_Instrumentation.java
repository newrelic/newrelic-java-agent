package io.netty.handler.codec.http2;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.bootstrap.NettyDispatcher;
import io.netty.buffer.ByteBuf;

// TODO delete this class, just doing logging
@Weave(type = MatchType.Interface, originalName = "io.netty.handler.codec.http2.Http2HeadersEncoder")
public class Http2HeadersEncoder_Instrumentation {

    public void encodeHeaders(int streamId, Http2Headers headers, ByteBuf buffer) {

        NettyDispatcher.debug2(headers, this.getClass().getName(), "encodeHeaders");

        Weaver.callOriginal();
    }

}
