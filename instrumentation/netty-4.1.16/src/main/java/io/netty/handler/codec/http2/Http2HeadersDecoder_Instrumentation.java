package io.netty.handler.codec.http2;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.bootstrap.NettyDispatcher;
import io.netty.buffer.ByteBuf;

// TODO delete this class, just doing logging
@Weave(type = MatchType.Interface, originalName = "io.netty.handler.codec.http2.Http2HeadersDecoder")
public class Http2HeadersDecoder_Instrumentation {

    public Http2Headers decodeHeaders(int streamId, ByteBuf headerBlock) {
        Http2Headers http2Headers = Weaver.callOriginal();

        NettyDispatcher.debug2(http2Headers, this.getClass().getName(), "decodeHeaders");

        return http2Headers;
    }

}
