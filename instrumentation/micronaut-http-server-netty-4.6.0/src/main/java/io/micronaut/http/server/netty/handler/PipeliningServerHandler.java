package io.micronaut.http.server.netty.handler;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext;

@Weave
public abstract class PipeliningServerHandler {

	@Trace(dispatcher = true)
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		Weaver.callOriginal();
	}
	
	@Weave
	private static class MessageInboundHandler  {
		
		@Trace
		void read(Object message) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","InboundHander","MessageInboundHandler","read");
			Weaver.callOriginal();
		}
		
	}
	
	@Weave
	private static class DecompressingInboundHandler  {
		
		@Trace
		void read(Object message) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","InboundHander","DecompressingInboundHandler","read");
			Weaver.callOriginal();
		}
		
	}
	
	@Weave
	private static class OptimisticBufferingInboundHandler {
		
		@Trace
		void read(Object message) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","InboundHander","OptimisticBufferingInboundHandler","read");
			Weaver.callOriginal();
		}
		
	}

	@Weave
	private static class DroppingInboundHandler {
		
		@Trace
		void read(Object message) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","InboundHander","DroppingInboundHandler","read");
			Weaver.callOriginal();
		}
		
	}

	@Weave
	private static class StreamingInboundHandler {
		
		@Trace
		void read(Object message) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","InboundHander","StreamingInboundHandler","read");
			Weaver.callOriginal();
		}
		
	}
	
	@Weave
	private static class ContinueOutboundHandler {
		
		@Trace
		void writeSome() {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","OutboundHander","ContinueOutboundHandler","writeSome");
			Weaver.callOriginal();
		}
	}

	@Weave
	private static class FullOutboundHandler {
		
		@Trace
		void writeSome() {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","OutboundHander","FullOutboundHandler","writeSome");
			Weaver.callOriginal();
		}
	}

	@Weave
	private static class StreamingOutboundHandler {
		
		@Trace
		void writeSome() {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Micronaut","HTTP","Netty","OutboundHander","StreamingOutboundHandler","writeSome");
			Weaver.callOriginal();
		}
	}
	
	@Weave
	 public static class OutboundAccessImpl {
		 
	 }
	
}
