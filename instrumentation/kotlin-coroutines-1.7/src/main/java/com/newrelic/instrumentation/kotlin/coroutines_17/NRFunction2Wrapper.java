package com.newrelic.instrumentation.kotlin.coroutines_17;

import com.newrelic.agent.bridge.AgentBridge;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.SuspendFunction;
import kotlin.jvm.functions.Function2;

public class NRFunction2Wrapper<P1, P2, R> implements Function2<P1, P2, R> {

	private Function2<P1, P2, R> delegate = null;
	private static boolean isTransformed = false;

	public NRFunction2Wrapper(Function2<P1, P2, R> d) {
		delegate = d;
		if(!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public R invoke(P1 p1, P2 p2) {
		
		if(p2 instanceof Continuation && !(p2 instanceof SuspendFunction)) {
			// wrap if needed
			String cont_string = Utils.getContinuationString((Continuation)p2);
			if(!(p2 instanceof NRContinuationWrapper) && !Utils.ignoreContinuation(cont_string)) {
				NRContinuationWrapper wrapper = new NRContinuationWrapper<>((Continuation)p2, cont_string);
				p2 = (P2) wrapper;
			}
		}
		if(delegate != null) {
			return delegate.invoke(p1, p2);
		}
		return null;
	}

}
