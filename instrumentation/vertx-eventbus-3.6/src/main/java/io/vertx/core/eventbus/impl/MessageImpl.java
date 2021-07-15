package io.vertx.core.eventbus.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.MessageHeaders;
import com.nr.instrumentation.vertx.NRWrappedReplyHandler;

@Weave(type=MatchType.BaseClass)
public abstract class MessageImpl<U, V> {

	public abstract String address();
	
	public abstract String replyAddress();
		
	public abstract MultiMap headers();
	
	@Trace(dispatcher=true)
	public <R> void reply(Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
		if(NRWrappedReplyHandler.class.isInstance(replyHandler)) {
			NRWrappedReplyHandler<R> wrapper = (NRWrappedReplyHandler<R>)replyHandler;
			Token token = wrapper.getToken();
			if(token != null) {
				token.link();
			}
		}
		MultiMap headers = options.getHeaders();
		if(headers == null) {
			headers = new CaseInsensitiveHeaders();
		}
		MessageHeaders msgHeaders = new MessageHeaders(headers);
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(msgHeaders);
		options.setHeaders(msgHeaders.getMultimap());
		String replyAddress = replyAddress();
		if(replyAddress != null && !replyAddress.isEmpty()) {
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("ReplyAddress", replyAddress());
		}
		String address = address();
		if(address != null && !address.isEmpty()) {
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("address", address);
		}
		Weaver.callOriginal();
	}
	
}
