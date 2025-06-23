package kotlinx.coroutines.intrinsics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_14.NRContinuationWrapper;
import com.newrelic.instrumentation.kotlin.coroutines_14.NRFunction1Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_14.NRFunction2Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_14.Utils;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.SuspendFunction;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.internal.ScopeCoroutine;
import org.jetbrains.annotations.Nullable;

@Weave(originalName = "kotlinx.coroutines.intrinsics.UndispatchedKt")
public class UndispatchedKt_Instrumentation {

	@Trace
	public static <T> void startCoroutineUnintercepted(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> cont) {
		String continuationString = Utils.getContinuationString(cont);
		if(!(cont instanceof SuspendFunction)) {
			if(!(cont instanceof NRContinuationWrapper) && Utils.continueWithContinuation(continuationString)) {
                cont = new NRContinuationWrapper<>(cont, continuationString);
			}
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function1");
		if(continuationString != null) {
			traced.addCustomAttribute("Continuation", continuationString);
		}
		if(!(f instanceof NRFunction1Wrapper)) {
            f = new NRFunction1Wrapper<>(f);
		}
		Weaver.callOriginal();
	}

	@Trace
	public static final <R, T> void startCoroutineUnintercepted(Function2<? super R, ? super Continuation<? super T>, ? extends Object> f, R receiver, Continuation<? super T> cont) {
		@Nullable
		String continuationString = Utils.getContinuationString(cont);
		if(!(cont instanceof SuspendFunction)) {
			if(!(cont instanceof NRContinuationWrapper) && Utils.continueWithContinuation(continuationString)) {
                cont = new NRContinuationWrapper<>(cont, continuationString);
			}			
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		if(continuationString != null) {
			traced.addCustomAttribute("Continuation", continuationString);
		}
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2Wrapper)) {
            f = new NRFunction2Wrapper<>(f);
		}
		Weaver.callOriginal();
	}

	@Trace
	public static final <T> void startCoroutineUndispatched(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> cont) {
		String continuationString = Utils.getContinuationString(cont);
		if(!(cont instanceof SuspendFunction)) {
			if(!(cont instanceof NRContinuationWrapper) && Utils.continueWithContinuation(continuationString)) {
				NRContinuationWrapper<? super T> wrapper = new NRContinuationWrapper<>(cont, continuationString);
				cont = wrapper;
			}			
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function1");
		if(continuationString != null) {
			traced.addCustomAttribute("Continuation", continuationString);
		}
		if(!(f instanceof NRFunction1Wrapper)) {
            f = new NRFunction1Wrapper<>(f);
		}
		Weaver.callOriginal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static final <R, T> void startCoroutineUndispatched(Function2<? super R, ? super Continuation<? super T>, ? extends Object> f, R receiver, Continuation<? super T> cont) {
		String continuationString = Utils.getContinuationString(cont);
		if(!(cont instanceof SuspendFunction)) {
			if(!(cont instanceof NRContinuationWrapper) && Utils.continueWithContinuation(continuationString)) {
                cont = new NRContinuationWrapper<>(cont, continuationString);
			}			
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		if(continuationString != null) {
			traced.addCustomAttribute("Continuation", continuationString);
		}
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2Wrapper)) {
            f = new NRFunction2Wrapper<>(f);
		}
		Weaver.callOriginal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static final <T, R> Object startUndispatchedOrReturn(ScopeCoroutine<? super T> scope, R receiver, Function2<? super R, ? super Continuation<? super T>, ? extends Object> f) {
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2Wrapper)) {
            f = new NRFunction2Wrapper<>(f);
		}
		return Weaver.callOriginal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static final <T, R> Object startUndispatchedOrReturnIgnoreTimeout(ScopeCoroutine<? super T> scope, R receiver, Function2<? super R, ? super Continuation<? super T>, ? extends Object> f) {
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		traced.addCustomAttribute("Suspend-Type", "Function2");
		traced.addCustomAttribute("Receiver", receiver.getClass().getName());
		if(!(f instanceof NRFunction2Wrapper)) {
            f = new NRFunction2Wrapper<>(f);
		}
		return Weaver.callOriginal();
	}

}
