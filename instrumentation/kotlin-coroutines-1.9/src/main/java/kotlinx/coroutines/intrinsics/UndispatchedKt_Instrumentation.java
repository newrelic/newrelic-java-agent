package kotlinx.coroutines.intrinsics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRFunction2SuspendWrapper;
import com.newrelic.instrumentation.kotlin.coroutines_19.Utils;

import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.internal.ScopeCoroutine;

@Weave(originalName = "kotlinx.coroutines.intrinsics.UndispatchedKt")
public class UndispatchedKt_Instrumentation {

	@Trace
	public static <R, T> void startCoroutineUndispatched(Function2<? super R, ? super Continuation<? super T>, ?> f, R receiver,
			Continuation<? super T> cont) {
		String continuationString = Utils.getContinuationString(cont);
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		if(continuationString != null) {
			traced.addCustomAttribute("Continuation", continuationString);
		}
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2SuspendWrapper)) {
            f = new NRFunction2SuspendWrapper<>(f);
		}
		Weaver.callOriginal();
	}

	@Trace
	public static <T, R> Object startUndispatchedOrReturn(ScopeCoroutine<? super T> scope, R receiver,
            Function2<? super R, ? super Continuation<? super T>, ?> f) {
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2SuspendWrapper)) {
            f = new NRFunction2SuspendWrapper<>(f);
		}
		return Weaver.callOriginal();
	}

	@Trace
	public static <T, R> Object startUndispatchedOrReturnIgnoreTimeout(ScopeCoroutine<? super T> scope, R receiver,
            Function2<? super R, ? super Continuation<? super T>, ?> f) {
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2SuspendWrapper)) {
            f = new NRFunction2SuspendWrapper<>(f);
		}
		return Weaver.callOriginal();
	}


}
