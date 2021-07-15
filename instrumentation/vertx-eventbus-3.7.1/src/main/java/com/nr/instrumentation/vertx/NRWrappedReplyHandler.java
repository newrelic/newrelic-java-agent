package com.nr.instrumentation.vertx;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

public class NRWrappedReplyHandler<T> implements Handler<AsyncResult<Message<T>>> {
	
	private Token token = null;
	private Segment segment = null;
	private Handler<AsyncResult<Message<T>>> delegate = null;
	
	public Token getToken() {
		return token;
	}

	public NRWrappedReplyHandler(Token t, Segment s, Handler<AsyncResult<Message<T>>> d) {
		token = t;
		segment = s;
		delegate = d;
	}



	@Override
	@Trace(async=true)
	public void handle(AsyncResult<Message<T>> event) {
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		if(segment != null) {
			segment.end();
			segment = null;
		}
		delegate.handle(event);
	}

}
