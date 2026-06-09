package com.newrelic.instrumentation.kotlin.coroutines_17;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import com.newrelic.api.agent.Trace;
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
	private String name = null;
	private String type = null;
	private static boolean isTransformed = false;

	public NRFunction2SuspendWrapper(String nameToUse, String typeToUse, Function2<S, T, R> d) {
		delegate = d;
		this.name = nameToUse;
		this.type = typeToUse;
		if (!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@Override
	@Trace
	public R invoke(S s, T t) {
		if (name != null) {
			if (type != null) {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom", "Kotlin-Coroutines", "Block", type, name);
			} else {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom", "Kotlin-Coroutines", "Block", "UnknownType", name);
			}
		} else {
			String generatedName = getName(s,t);
			if (type != null) {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom", "Kotlin-Coroutines", "Block", type, generatedName);
			} else {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom", "Kotlin-Coroutines", "Block", "UnknownType", generatedName);
			}
		}
		if (delegate != null) {
			return delegate.invoke(s, t);
		}
		return null;
	}

	private String getName(S s, T t) {
		if (s instanceof CoroutineScope) {
			CoroutineScope scope = (CoroutineScope) s;
			CoroutineContext context = scope.getCoroutineContext();
			String coroutineName = Utils.getCoroutineName(context);
			if (coroutineName != null) {
				return coroutineName;
			}
		}

		if (t instanceof Continuation) {
			Continuation<?> cont = (Continuation<?>) t;

			String cont_string = Utils.getContinuationString(cont);
			if (cont_string != null) {
				return cont_string;
			}
		}
		return "UnknownSource";
	}

}
