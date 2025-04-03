package io.micronaut.http.server.netty.handler;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.micronaut.http.server.netty.body.ByteBody;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

@Weave(type = MatchType.Interface, originalName = "io.micronaut.http.server.netty.handler.RequestHandler")
public abstract class RequestHandler_Instrumentation {

	@Trace
	public void accept(ChannelHandlerContext ctx, HttpRequest request, ByteBody body, OutboundAccess outboundAccess) {
		
		if(request != null) {
			String uri = request.uri();
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("URI", uri != null ? uri : "null");
			HttpMethod method = request.method();
			String methodName = null;
			if(method != null) {
				methodName = method.name();
			}
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("Method", methodName != null ? methodName : "null");
			if(uri != null && methodName != null) {
				NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Micronaut-Netty", methodName + " - " + uri);
			}
		}
		
		Weaver.callOriginal();
	}

}
