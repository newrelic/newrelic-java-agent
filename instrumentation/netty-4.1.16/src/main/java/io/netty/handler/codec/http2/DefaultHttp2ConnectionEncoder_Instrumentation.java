//package io.netty.handler.codec.http2;
//
//import com.agent.instrumentation.netty4116.NettyUtil;
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.Weaver;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelHandlerContext_Instrumentation;
//import io.netty.channel.ChannelPromise;
//
//@Weave(type = MatchType.ExactClass, originalName = "io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder")
//public class DefaultHttp2ConnectionEncoder_Instrumentation {
//
//    private ChannelFuture writeHeaders0(final ChannelHandlerContext_Instrumentation ctx, final int streamId,
//            final Http2Headers headers, final boolean hasPriority,
//            final int streamDependency, final short weight,
//            final boolean exclusive, final int padding,
//            final boolean endOfStream, ChannelPromise promise) {
//
//        if (headers != null) {
//            boolean expired = NettyUtil.processResponse(headers, ctx.pipeline().token);
//            if (expired) {
//                ctx.pipeline().token = null;
//            }
//        }
//
//
//
////        try {
////            Http2Stream stream = connection.stream(streamId);
////            if (stream == null) {
////                try {
////                    // We don't create the stream in a `halfClosed` state because if this is an initial
////                    // HEADERS frame we don't want the connection state to signify that the HEADERS have
////                    // been sent until after they have been encoded and placed in the outbound buffer.
////                    // Therefore, we let the `LifeCycleManager` will take care of transitioning the state
////                    // as appropriate.
////                    stream = connection.local().createStream(streamId, /*endOfStream*/ false);
////                } catch (Http2Exception cause) {
////                    if (connection.remote().mayHaveCreatedStream(streamId)) {
////                        promise.tryFailure(new IllegalStateException("Stream no longer exists: " + streamId, cause));
////                        return promise;
////                    }
////                    throw cause;
////                }
////            } else {
////                switch (stream.state()) {
////                    case RESERVED_LOCAL:
////                        stream.open(endOfStream);
////                        break;
////                    case OPEN:
////                    case HALF_CLOSED_REMOTE:
////                        // Allowed sending headers in these states.
////                        break;
////                    default:
////                        throw new IllegalStateException("Stream " + stream.id() + " in unexpected state " +
////                                stream.state());
////                }
////            }
////
////            // Trailing headers must go through flow control if there are other frames queued in flow control
////            // for this stream.
////            Http2RemoteFlowController flowController = flowController();
////            if (!endOfStream || !flowController.hasFlowControlled(stream)) {
////                // The behavior here should mirror that in FlowControlledHeaders
////
////                promise = promise.unvoid();
////                boolean isInformational = validateHeadersSentState(stream, headers, connection.isServer(), endOfStream);
////
////                ChannelFuture future = sendHeaders(frameWriter, ctx, streamId, headers, hasPriority, streamDependency,
////                        weight, exclusive, padding, endOfStream, promise);
////
////                // Writing headers may fail during the encode state if they violate HPACK limits.
////                Throwable failureCause = future.cause();
////                if (failureCause == null) {
////                    // Synchronously set the headersSent flag to ensure that we do not subsequently write
////                    // other headers containing pseudo-header fields.
////                    //
////                    // This just sets internal stream state which is used elsewhere in the codec and doesn't
////                    // necessarily mean the write will complete successfully.
////                    stream.headersSent(isInformational);
////
////                    if (!future.isSuccess()) {
////                        // Either the future is not done or failed in the meantime.
////                        notifyLifecycleManagerOnError(future, ctx);
////                    }
////                } else {
////                    lifecycleManager.onError(ctx, true, failureCause);
////                }
////
////                if (endOfStream) {
////                    // Must handle calling onError before calling closeStreamLocal, otherwise the error handler will
////                    // incorrectly think the stream no longer exists and so may not send RST_STREAM or perform similar
////                    // appropriate action.
////                    lifecycleManager.closeStreamLocal(stream, future);
////                }
////
////                return future;
////            } else {
////                // Pass headers to the flow-controller so it can maintain their sequence relative to DATA frames.
////                flowController.addFlowControlled(stream,
////                        new DefaultHttp2ConnectionEncoder.FlowControlledHeaders(stream, headers, hasPriority, streamDependency,
////                                weight, exclusive, padding, true, promise));
////                return promise;
////            }
////        } catch (Throwable t) {
////            lifecycleManager.onError(ctx, true, t);
////            promise.tryFailure(t);
////            return promise;
////        }
//
//        return Weaver.callOriginal();
//    }
//
//}
