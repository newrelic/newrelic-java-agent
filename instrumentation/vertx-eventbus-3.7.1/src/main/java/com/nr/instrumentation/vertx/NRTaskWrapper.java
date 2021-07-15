package com.nr.instrumentation.vertx;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import io.vertx.core.Handler;

public class NRTaskWrapper<T> implements Handler<T> {
	
	private Handler<T> delegate = null;
	private Token token = null;
	private static boolean isTransformed = false;
	public String name = null;
	
	
	public NRTaskWrapper(Handler<T> d, Token t) {
		delegate = d;
		token = t;
		if(!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@Trace(async=true,excludeFromTransactionTrace=true)
	public void handle(T event) {
		if(name != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","TaskWrapper","handle",name});
		}
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		if(delegate != null) {
			delegate.handle(event);
		}
	}
	
	public void linkAndExpireToken() {
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
	}

	public Token getToken() {
		return token;
	}
}
