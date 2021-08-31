package com.newrelic.instrumentation.kotlin.coroutines;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;

public class NRContinuationWrapper<T> implements Continuation<T> {
	
	private Continuation<T> delegate = null;
	private String name = null;
	private static boolean isTransformed = false;
	
	public NRContinuationWrapper(Continuation<T> d, String n) {
		delegate = d;
		name = n;
		if(!isTransformed) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			isTransformed = true;
		}
	}

	@Override
	public CoroutineContext getContext() {
		return delegate.getContext();
	}

	@Override
	@Trace(async=true)
	public void resumeWith(Object p0) {
		
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","ContinuationWrapper","resumeWith",name != null ? name : Utils.getCoroutineName(getContext(), delegate));
		Token token = Utils.getToken(getContext());
		if(token != null) {
			token.link();
		}
		delegate.resumeWith(p0);
	}

}
