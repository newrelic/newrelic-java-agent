/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package netty4116;

import com.agent.instrumentation.netty4116.NettyUtil;
import io.grpc.netty.GrpcHttp2HeadersUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPipeline_Instrumentation;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NettyUtilTest {

    private static ChannelHandlerContext_Instrumentation ctxWithHandler(ChannelHandler handler) {
        return new ChannelHandlerContext_Instrumentation() {
            @Override
            public ChannelPipeline_Instrumentation pipeline() {
                return null;
            }

            @Override
            public ChannelHandler handler() {
                return handler;
            }
        };
    }

    @Test
    public void testIsRequestHeaders_withMethod() {
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.method("GET");

        Assert.assertTrue(NettyUtil.isRequestHeaders(headers));
    }

    @Test
    public void testIsRequestHeaders_withoutMethod() {
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.status("200");

        Assert.assertFalse(NettyUtil.isRequestHeaders(headers));
    }

    @Test
    public void testIsRequestHeaders_throwingHeadersReturnsFalse() {
        GrpcHttp2HeadersUtils.GrpcHttp2ResponseHeaders headers = new GrpcHttp2HeadersUtils.GrpcHttp2ResponseHeaders();

        Assert.assertFalse(NettyUtil.isRequestHeaders(headers));
    }

    private static class GrpcHttp2OutboundHeadersLike extends DefaultHttp2Headers {
        private final CharSequence authority;

        GrpcHttp2OutboundHeadersLike(CharSequence authority) {
            this.authority = authority;
        }

        @Override
        public CharSequence method() {
            throw new UnsupportedOperationException("method() is not available on this outbound representation");
        }

        @Override
        public CharSequence authority() {
            return authority;
        }
    }

    @Test
    public void testIsRequestHeaders_grpcNettyOutboundRequest() {
        GrpcHttp2OutboundHeadersLike headers = new GrpcHttp2OutboundHeadersLike("localhost:8980");

        Assert.assertTrue(NettyUtil.isRequestHeaders(headers));
    }

    @Test
    public void testIsRequestHeaders_grpcNettyOutboundResponseOrTrailers() {
        GrpcHttp2OutboundHeadersLike headers = new GrpcHttp2OutboundHeadersLike(null);

        Assert.assertFalse(NettyUtil.isRequestHeaders(headers));
    }

    @Test
    public void testIsServerConnection_server() {
        Http2Connection connection = mock(Http2Connection.class);
        when(connection.isServer()).thenReturn(true);
        Http2ConnectionHandler handler = mock(Http2ConnectionHandler.class);
        when(handler.connection()).thenReturn(connection);

        Assert.assertTrue(NettyUtil.isServerConnection(ctxWithHandler(handler)));
    }

    @Test
    public void testIsServerConnection_client() {
        Http2Connection connection = mock(Http2Connection.class);
        when(connection.isServer()).thenReturn(false);
        Http2ConnectionHandler handler = mock(Http2ConnectionHandler.class);
        when(handler.connection()).thenReturn(connection);

        Assert.assertFalse(NettyUtil.isServerConnection(ctxWithHandler(handler)));
    }

    @Test
    public void testIsServerConnection_notAnHttp2ConnectionHandler() {
        ChannelHandler handler = mock(ChannelHandler.class);
        Assert.assertFalse(NettyUtil.isServerConnection(ctxWithHandler(handler)));
    }

    @Test
    public void testIsServerConnection_nullHandler() {
        Assert.assertFalse(NettyUtil.isServerConnection(ctxWithHandler(null)));
    }

    @Test
    public void testIsServerConnection_throwingHandlerReturnsFalse() {
        ChannelHandlerContext_Instrumentation ctx = new ChannelHandlerContext_Instrumentation() {
            @Override
            public ChannelPipeline_Instrumentation pipeline() {
                return null;
            }

            @Override
            public ChannelHandler handler() {
                throw new RuntimeException("boom");
            }
        };

        Assert.assertFalse(NettyUtil.isServerConnection(ctx));
    }
}
