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

To instrument HTTP/2 the `io.netty.handler.codec.http2.Http2FrameCodec` class is weaved and new wrappers are used to handle working
with HTTP/2 frames instead of request/response objects.

### Request

When `Http2FrameCodec.onHttp2Frame` is invoked on a `Http2HeadersFrame` representing the request headers, `NettyDispatcher.channelRead` is called which stores
a `token` in the context pipeline, starts a transaction, and sets/wraps the request on the transaction.

### Response

When `Http2FrameCodec.write` is invoked on a `Http2HeadersFrame` representing the response headers, `NettyUtil.processResponse` is called which uses the
`token` in the context pipeline to get the transaction, sets/wraps the response on the transaction, and expires the `token`.
