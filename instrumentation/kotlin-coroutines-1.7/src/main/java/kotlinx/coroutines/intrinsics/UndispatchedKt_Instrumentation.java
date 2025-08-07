package kotlinx.coroutines.intrinsics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRContinuationWrapper;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRFunction1SuspendWrapper;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRFunction2SuspendWrapper;
import com.newrelic.instrumentation.kotlin.coroutines_17.Utils;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.SuspendFunction;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.internal.ScopeCoroutine;

@Weave(originalName = "kotlinx.coroutines.intrinsics.UndispatchedKt")
public class UndispatchedKt_Instrumentation {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static final <T> void startCoroutineUnintercepted(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> cont) {
		String continuationString = Utils.getContinuationString(cont);
		if(cont != null && !(cont instanceof SuspendFunction)) {
			if(!(cont instanceof NRContinuationWrapper) && !Utils.ignoreContinuation(continuationString)) {
				NRContinuationWrapper<? super T> wrapper = new NRContinuationWrapper<>(cont, continuationString);
				cont = wrapper;
			}
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function1");
		if(continuationString != null) {
			traced.addCustomAttribute("Continuation", continuationString);
		}
		if(!(f instanceof NRFunction1SuspendWrapper)) {
			NRFunction1SuspendWrapper wrapper = new NRFunction1SuspendWrapper<>(f);
			f = wrapper;
		}
		Weaver.callOriginal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static final <R, T> void startCoroutineUndispatched(Function2<? super R, ? super Continuation<? super T>, ? extends Object> f, R receiver, Continuation<? super T> cont) {
		String continuationString = Utils.getContinuationString(cont);
		if(cont != null && !(cont instanceof SuspendFunction)) {
			if(!(cont instanceof NRContinuationWrapper) && !Utils.ignoreContinuation(continuationString)) {
				NRContinuationWrapper<? super T> wrapper = new NRContinuationWrapper<>(cont, continuationString);
				cont = wrapper;
			}			
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		if(continuationString != null) {
			traced.addCustomAttribute("Continuation", continuationString);
		}
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2SuspendWrapper)) {
			NRFunction2SuspendWrapper wrapper = new NRFunction2SuspendWrapper<>(f);
			f = wrapper;
		}
		Weaver.callOriginal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static final <T, R> Object startUndispatchedOrReturn(ScopeCoroutine<? super T> scope, R receiver, Function2<? super R, ? super Continuation<? super T>, ? extends Object> f) {
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2SuspendWrapper)) {
			NRFunction2SuspendWrapper wrapper = new NRFunction2SuspendWrapper<>(f);
			f = wrapper;
		}
		return Weaver.callOriginal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static final <T, R> Object startUndispatchedOrReturnIgnoreTimeout(ScopeCoroutine<? super T> scope, R receiver, Function2<? super R, ? super Continuation<? super T>, ? extends Object> f) {
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2SuspendWrapper)) {
			NRFunction2SuspendWrapper wrapper = new NRFunction2SuspendWrapper<>(f);
			f = wrapper;
		}
		return Weaver.callOriginal();
	}

}
