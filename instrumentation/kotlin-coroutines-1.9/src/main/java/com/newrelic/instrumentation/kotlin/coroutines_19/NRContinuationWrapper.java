package com.newrelic.instrumentation.kotlin.coroutines_19;

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
		String contString = Utils.getContinuationString(delegate);
		if(contString != null && !contString.isEmpty()) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","ContinuationWrapper","resumeWith",contString);
		} else if(name != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","ContinuationWrapper","resumeWith",name);
		} else {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","ContinuationWrapper","resumeWith",p0.getClass().getName());
		}
		Token t = Utils.getToken(getContext());
		if(t != null) {
			t.link();
		}
		if(delegate != null) {
			delegate.resumeWith(p0);
		}
	}

}
