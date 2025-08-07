package com.newrelic.instrumentation.kotlin.coroutines_19;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;

public class NRFunction1SuspendWrapper<P1, R> implements Function1<P1, R> {
	
	private Function1<P1, R> delegate = null;
	private static boolean isTransformed = false;
	
	public NRFunction1SuspendWrapper(Function1<P1,R> d) {
		delegate = d;
		if(!isTransformed) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			isTransformed = true;
		}
	}

	@Override
	public R invoke(P1 p1) {
		if(p1 instanceof Continuation) {
			Continuation<?> cont = (Continuation<?>)p1;

			String cont_string = Utils.getContinuationString(cont);
			if(cont_string != null) {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Block","SuspendFunction",cont_string);
			} else {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Block","SuspendFunction","UnknownSource");

			}
		}
		return delegate != null ? delegate.invoke(p1) : null;
	}

}
