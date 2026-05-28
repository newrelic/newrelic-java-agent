package io.netty.channel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.agent.instrumentation.netty4116.Http2RequestHeaderWrapper;
import com.agent.instrumentation.netty4116.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

public class NRNettyChannelHandler extends ChannelInboundHandlerAdapter_Instrumentation {

	public static final String NR_CHANNEL_HANDLER = "newrelic.channel.handler";
	public static final AtomicBoolean instrumented = new AtomicBoolean(false);

	public NRNettyChannelHandler() {
		if(!instrumented.get()) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			instrumented.set(true);
		}
	}

	@Trace(dispatcher = true)
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		ChannelPipeline pipeline = ctx.pipeline();
		if (pipeline instanceof DefaultChannelPipeline_Instrumentation) {
			DefaultChannelPipeline_Instrumentation dPipeline = (DefaultChannelPipeline_Instrumentation)pipeline;

			if(dPipeline.nettyToken == null) {
				dPipeline.nettyToken = NewRelic.getAgent().getTransaction().getToken();
			}
		}
		Transaction tx = NewRelic.getAgent().getTransaction();
		if (tx != null) {
			tx.convertToWebTransaction();
			if(msg instanceof HttpRequest) {
				tx.setWebRequest(new RequestWrapper((HttpRequest) msg));
			} else if(msg instanceof Http2HeadersFrame) {
				tx.setWebRequest(new Http2RequestHeaderWrapper((Http2Headers) msg));
			}
		} 
		TracedMethod tracer = NewRelic.getAgent().getTracedMethod();
		if (tracer == null) {
			NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to dispatch netty tx. No tracer."); // it happens.
		} else {
			tracer.setMetricName("NettyUpstreamDispatcher");
			NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.REQUEST_URI, true,
					"NettyDispatcher", "NettyDispatcher");
		}
		ctx.fireChannelRead(msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		if(ctx.pipeline().get(NR_CHANNEL_HANDLER) != null) {
			ctx.pipeline().remove(NR_CHANNEL_HANDLER);
		}
		super.channelReadComplete(ctx);
	}


}
