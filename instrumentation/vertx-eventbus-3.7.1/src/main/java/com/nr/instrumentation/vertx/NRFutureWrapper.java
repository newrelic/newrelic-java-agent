package com.nr.instrumentation.vertx;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import io.vertx.core.Future;
import io.vertx.core.Handler;

public class NRFutureWrapper<T> implements Handler<Future<T>> {
	
	private Handler<Future<T>> delegate = null;
	
	private Token token = null;
	
	public NRFutureWrapper(Handler<Future<T>> h,Token t) {
		delegate = h;
		token = t;
	}

	@Override
	@Trace(async=true)
	public void handle(Future<T> event) {
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","FutureHandler","handle"});
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		delegate.handle(event);
	}

}
