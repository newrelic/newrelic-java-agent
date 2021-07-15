package io.vertx.core.eventbus.impl;

import io.vertx.core.Context;
import io.vertx.core.spi.metrics.EventBusMetrics;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class HandlerHolder<T> {

	@NewField
	public Token token = null;
	
	private final HandlerRegistration<T> handler = Weaver.callOriginal();

	@SuppressWarnings("rawtypes")
	public HandlerHolder(EventBusMetrics metrics, HandlerRegistration<T> handler, boolean replyHandler, boolean localOnly,Context context) {
		
	}
	
	@Trace(async=true)
	public HandlerRegistration<T> getHandler() {
		HandlerRegistration<T> hr = Weaver.callOriginal();
		if(token != null) {
			hr.token = token;
			token = null;
		}
		return hr;
	}
	
	@Trace(async=true)
	public boolean isReplyHandler() {
		if(token != null) {
			handler.token = token;
			token = null;
		}
		return Weaver.callOriginal();
	}
	
	public synchronized boolean isRemoved() {
		boolean b = Weaver.callOriginal();
		if(b && token != null) {
			token.expire();
			token = null;
		}
		return b;
	}
}
