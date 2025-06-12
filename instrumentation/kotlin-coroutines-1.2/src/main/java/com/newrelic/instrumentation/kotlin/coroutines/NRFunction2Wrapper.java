package com.newrelic.instrumentation.kotlin.coroutines;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;

public class NRFunction2Wrapper<P1, P2, R> implements Function2<P1, P2, R> {
	
	private Function2<P1, P2, R> delegate = null;
	private String name = null;
	private static boolean isTransformed = false;
	
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
	@Trace(async=true, excludeFromTransactionTrace = true)
	public R invoke(P1 p1, P2 p2) {
		
		boolean isUndispatched = p1.getClass().getName().equals("kotlinx.coroutines.UndispatchedCoroutine")  || p2.getClass().getName().equals("kotlinx.coroutines.UndispatchedCoroutine");
		if (!isUndispatched) {
			String nameStr = null;
			boolean linked = false;
			if (p1 instanceof CoroutineContext) {
				CoroutineContext ctx = (CoroutineContext) p1;
				nameStr = Utils.getCoroutineName(ctx);
				Token token = Utils.getToken(ctx);
				if (token != null) {
					token.link();
					linked = true;
				}
			}
			if (p1 instanceof CoroutineScope) {
				CoroutineScope scope = (CoroutineScope) p1;
				nameStr = Utils.getCoroutineName(scope.getCoroutineContext());
				if (!linked) {
					Token token = Utils.getToken(scope.getCoroutineContext());
					if (token != null) {
						token.link();
						linked = true;
					}
				}
			}
			if (p2 instanceof Continuation) {
				Continuation continuation = (Continuation) p2;
				if (nameStr == null)
					nameStr = Utils.getCoroutineName(continuation.getContext(), continuation);
				if (nameStr == null || nameStr.equals(Utils.CREATEMETHOD1) || nameStr.equals(Utils.CREATEMETHOD2))
					nameStr = name;

				if (!linked) {
					Token token = Utils.getToken(continuation.getContext());
					if (token != null) {
						token.link();
						linked = true;
					}
				}

				if (!Utils.ignoreContinuation(name)
						&& !Utils.ignoreContinuation(continuation.getClass(), continuation.getContext())) {
					NRContinuationWrapper wrapper = new NRContinuationWrapper(continuation, nameStr);
					p2 = (P2) wrapper;
				}
			}
			if (nameStr == null) {
				nameStr = name;
			}
			if (nameStr != null) {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom", "WrappedSuspend", nameStr);
			} 
		}
		if(delegate != null) {
			return delegate.invoke(p1, p2);
		}
		return null;
	}
	
	public void markUndispatched() {
		
	}

}
