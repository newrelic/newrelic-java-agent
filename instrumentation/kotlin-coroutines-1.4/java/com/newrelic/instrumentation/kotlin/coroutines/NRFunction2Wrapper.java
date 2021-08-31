package com.newrelic.instrumentation.kotlin.coroutines;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;

public class NRFunction2Wrapper<P1, P2, R> implements Function2<P1, P2, R> {
	
	private Function2<P1, P2, R> delegate = null;
	private String name = null;
	private static boolean isTransformed = false;
	public Token token = null;
	
	public NRFunction2Wrapper(Function2<P1, P2, R> d,String n) {
		delegate = d;
		name = n;
		if(!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Trace(async=true)
	public R invoke(P1 p1, P2 p2) {
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		String nameStr = null;
		if(p2 instanceof Continuation) {
			Continuation continuation = (Continuation)p2;
			
			if (!Utils.ignoreContinuation(continuation.getClass(), continuation.getContext())) {
				NRContinuationWrapper wrapper = new NRContinuationWrapper(continuation, name);
				p2 = (P2) wrapper;
			}
		}
		if(nameStr == null) {
			nameStr = name;
		}
		if(nameStr != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","WrappedSuspend",nameStr);
		}
		if(delegate != null) {
			return delegate.invoke(p1, p2);
		}
		return null;
	}

}
