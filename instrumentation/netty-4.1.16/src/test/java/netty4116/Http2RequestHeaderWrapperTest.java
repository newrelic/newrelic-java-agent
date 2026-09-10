/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package netty4116;

import com.agent.instrumentation.netty4116.Http2RequestHeaderWrapper;
import io.grpc.netty.GrpcHttp2HeadersUtils;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;
import org.junit.Assert;
import org.junit.Test;

public class Http2RequestHeaderWrapperTest {

    @Test
    public void testGetHeader_grpcRequestHeaders() {
        GrpcHttp2HeadersUtils.GrpcHttp2RequestHeaders headers = new GrpcHttp2HeadersUtils.GrpcHttp2RequestHeaders();
        headers.add(new AsciiString("x-custom"), "custom-value");

        Http2RequestHeaderWrapper wrapper = new Http2RequestHeaderWrapper(headers);

        Assert.assertEquals("custom-value", wrapper.getHeader("x-custom"));
    }

    @Test
    public void testGetHeaderAndGetHeaders_grpcResponseHeaders() {
        GrpcHttp2HeadersUtils.GrpcHttp2ResponseHeaders headers = new GrpcHttp2HeadersUtils.GrpcHttp2ResponseHeaders();
        headers.add(new AsciiString("x-custom"), "custom-value");

        Http2RequestHeaderWrapper wrapper = new Http2RequestHeaderWrapper(headers);

        Assert.assertEquals("custom-value", wrapper.getHeader("x-custom"));
        Assert.assertEquals(1, wrapper.getHeaders("x-custom").size());
        Assert.assertEquals("custom-value", wrapper.getHeaders("x-custom").get(0));
    }

    @Test
    public void testGrpcResponseHeaders_methodAndPathReturnNull() {
        GrpcHttp2HeadersUtils.GrpcHttp2ResponseHeaders headers = new GrpcHttp2HeadersUtils.GrpcHttp2ResponseHeaders();

        Http2RequestHeaderWrapper wrapper = new Http2RequestHeaderWrapper(headers);

        Assert.assertNull(wrapper.getMethod());
        Assert.assertNull(wrapper.getRequestURI());
    }

    @Test
    public void testGetHeader_plainHttp2HeadersUnaffected() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.add("x-custom", "custom-value");

        Http2RequestHeaderWrapper wrapper = new Http2RequestHeaderWrapper(headers);

        Assert.assertEquals("custom-value", wrapper.getHeader("x-custom"));
    }

    @Test
    public void testGetHeader_grpcRequestHeadersAbsentHeaderReturnsNull() {
        GrpcHttp2HeadersUtils.GrpcHttp2RequestHeaders headers = new GrpcHttp2HeadersUtils.GrpcHttp2RequestHeaders();

        Http2RequestHeaderWrapper wrapper = new Http2RequestHeaderWrapper(headers);

        Assert.assertNull(wrapper.getHeader("x-missing"));
    }
}
