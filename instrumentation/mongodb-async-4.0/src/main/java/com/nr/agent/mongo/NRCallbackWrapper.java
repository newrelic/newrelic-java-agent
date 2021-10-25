package com.nr.agent.mongo;

import com.mongodb.internal.async.SingleResultCallback;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class NRCallbackWrapper<T> implements SingleResultCallback<T> {
	
	private SingleResultCallback<T> delegate = null;
	private static boolean isTransformed = false;
	public Token token = null;
	public Segment segment = null;
	public DatastoreParameters params = null;
	
	public NRCallbackWrapper(SingleResultCallback<T> d) {
		delegate = d;
		if(!isTransformed) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			isTransformed = true;
		}
	}

	@Override
	@Trace(async=true)
	public void onResult(T result, Throwable t) {
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		if(segment != null) {
			if(params != null) {
				segment.reportAsExternal(params);
			}
			segment.end();
		} else if(params != null ){
			NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
		}
		delegate.onResult(result, t);
	}

}
