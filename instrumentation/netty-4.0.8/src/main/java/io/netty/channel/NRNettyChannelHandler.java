package io.netty.channel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.agent.instrumentation.netty40.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;

import io.netty.handler.codec.http.HttpRequest;

public class NRNettyChannelHandler extends ChannelInboundHandlerAdapter {

    public static final String NR_CHANNEL_HANDLER = "newrelic.channel.handler";
    public static final AtomicBoolean instrumented = new AtomicBoolean(false);

    public NRNettyChannelHandler() {
        if(!instrumented.get()) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
            instrumented.set(true);
        }
    }

    @Trace(dispatcher = true, excludeFromTransactionTrace = true)
    public void channelRead(ChannelHandlerContext ctx, Object msg) {


        ChannelPipeline pipeline = ctx.pipeline();
        if (pipeline instanceof DefaultChannelPipeline) {
            DefaultChannelPipeline dPipeline = (DefaultChannelPipeline)pipeline;

            if(dPipeline.nettyToken == null) {
                dPipeline.nettyToken = NewRelic.getAgent().getTransaction().getToken();
            }
        }
        Transaction tx = NewRelic.getAgent().getTransaction();
        if (tx != null) {
            tx.convertToWebTransaction();
            if(msg instanceof HttpRequest) {
                tx.setWebRequest(new RequestWrapper((HttpRequest) msg));
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
