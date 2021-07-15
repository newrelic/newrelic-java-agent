package com.nr.instrumentation.vertx;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class NRCompletionWrapper<T> implements Handler<AsyncResult<T>> {

	private Segment segment = null;
	private Handler<AsyncResult<T>> delegate = null;
	public Token token = null;
	
	public NRCompletionWrapper(Handler<AsyncResult<T>> d,Token t, Segment s) {
		delegate = d;
		token = t;
		segment = s;
	}
	
	
	@Override
	@Trace(async=true)
	public void handle(AsyncResult<T> event) {
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","CompletionHandler","handle"});
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		if(delegate != null) {
			delegate.handle(event);
		}
		if(segment != null) {
			segment.end();
		}
	}

}
