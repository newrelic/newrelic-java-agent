package com.newrelic.instrumentation.kotlin.coroutines;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

import kotlin.jvm.functions.Function1;

public class NRFunction1Wrapper<P1,R> implements Function1<P1, R> {
	
	private Function1<P1, R> delegate = null;
	private String name = null;
	private static boolean isTransformed = false;
	
	public NRFunction1Wrapper(Function1<P1, R> d, String n) {
		delegate = d;
		name = n;
		if(!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@Override
	@Trace(dispatcher=true)
	public R invoke(P1 p1) {
		if(name != null) NewRelic.getAgent().getTracedMethod().setMetricName("Custom","WrappedSuspend",name);
		if(delegate != null) {
			return delegate.invoke(p1);
		}
		return null;
	}

}
