package com.newrelic.instrumentation.kotlin.coroutines_17;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;

/*
 * Used to wrap a suspend function that was used as an input as a parameter to the
 * Coroutine functions runBlocking, async, invoke, launch and withContext
 */
public class NRFunction2SuspendWrapper<S, T, R> implements Function2<S, T, R> {

	private Function2<S, T, R> delegate = null;
	private static boolean isTransformed = false;

	public NRFunction2SuspendWrapper(Function2<S, T, R> d) {
		delegate = d;
		if(!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@Override
	public R invoke(S s, T t) {
		
		if(t instanceof Continuation) {
			Continuation<?> cont = (Continuation<?>)t;

			String cont_string = Utils.getContinuationString(cont);
			if(cont_string != null) {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Block","SuspendFunction",cont_string);
			} else {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Block","SuspendFunction","UnknownSource");

			}
		}
		if(s instanceof CoroutineScope) {
			CoroutineScope scope = (CoroutineScope)s;
			CoroutineContext ctx = scope.getCoroutineContext();
			Token token = Utils.getToken(ctx);
			if(token != null) {
				token.link();
			}
			
		}
		if(delegate != null) {
			return delegate.invoke(s, t);
		}
		return null;
	}

}
