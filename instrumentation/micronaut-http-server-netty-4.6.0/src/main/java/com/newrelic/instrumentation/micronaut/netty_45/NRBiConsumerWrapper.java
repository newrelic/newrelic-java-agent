package com.newrelic.instrumentation.micronaut.netty_45;

import java.util.function.BiConsumer;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class NRBiConsumerWrapper<R> implements BiConsumer<R, Throwable> {

	BiConsumer<R, Throwable> delegate = null;
	private Token token = null;
	private static boolean isTransformed = false;
	
	public NRBiConsumerWrapper(BiConsumer<R, Throwable> d, Token t) {
		delegate = d;
		token = t;
		if(!isTransformed) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			isTransformed = true;
		}
	}
	
	@Override
	@Trace(async = true)
	public void accept(R t, Throwable u) {
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		if(u != null) {
			NewRelic.noticeError(u);
		}
		if(delegate != null) {
			delegate.accept(t, u);
		}
	}

}
