# Netty

In addition to supporting HTTP/1, this instrumentation module adds support for
HTTP/2 starting with Netty version `io.netty:netty-all:4.1.16.Final`.

At the time this instrumentation was added the most recent version of Netty was
`io.netty:netty-all:4.1.107.Final` and virtually all the HTTP/2 APIs in the
project are annotated as unstable.

## HTTP/2

HTTP/2 works in a fundamentally different manner than HTTP/1.

> At the core of all performance enhancements of HTTP/2 is the new binary framing layer, which dictates how the HTTP messages are encapsulated and transferred
> between the client and server.
>
>The "layer" refers to a design choice to introduce a new optimized encoding mechanism between the socket interface and the higher HTTP API exposed to our
> applications: the HTTP semantics, such as verbs, methods, and headers, are unaffected, but the way they are encoded while in transit is different.
>
>In short, HTTP/2 breaks down the HTTP protocol communication into an exchange of binary-encoded frames, which are then mapped to messages that belong to a
> particular stream, all of which are multiplexed within a single TCP connection.
>
>* Stream: A bidirectional flow of bytes within an established connection, which may carry one or more messages.
>* Message: A complete sequence of frames that map to a logical request or response message.
>* Frame: The smallest unit of communication in HTTP/2, each containing a frame header, which at a minimum identifies the stream to which the frame belongs.
>
>The relation of these terms can be summarized as follows:
>* All communication is performed over a single TCP connection that can carry any number of bidirectional streams.
>* Each stream has a unique identifier and optional priority information that is used to carry bidirectional messages.
>* Each message is a logical HTTP message, such as a request, or response, which consists of one or more frames.
>* The frame is the smallest unit of communication that carries a specific type of data—e.g., HTTP headers, message payload, and so on. Frames from different
   streams may be interleaved and then reassembled via the embedded stream identifier in the header of each frame.

## HTTP/2 Instrumentation

To instrument HTTP/2, instrumentation points are weaved where the `io.netty.handler.codec.http2.Http2Headers` are available. The `Http2Headers` are wrapped and used to collect info about the  request and response.

### Request

`io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder$FrameReadListener` is weaved to start a transaction for HTTP/2 requests and process the requests headers. 

When `FrameReadListener.onHeadersRead` is invoked on the `Http2Headers` request headers, `NettyDispatcher.channelRead` is called which stores
a `token` in the Netty context pipeline, starts a transaction, and sets/wraps the request headers on the transaction.

### Response

`io.netty.handler.codec.http2.Http2FrameWriter` is weaved to process the HTTP/2 response headers.

When `Http2FrameWriter.writeHeaders` is invoked on the `Http2Headers` response headers, `NettyUtil.processResponse` is called which uses the
`token` in the Netty context pipeline to get the transaction, sets/wraps the response on the transaction, and expires the `token`.
