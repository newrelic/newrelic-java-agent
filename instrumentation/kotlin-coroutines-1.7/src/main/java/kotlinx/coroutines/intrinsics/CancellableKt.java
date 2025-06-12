package kotlinx.coroutines.intrinsics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRContinuationWrapper;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRFunction1Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRFunction2Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_17.Utils;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.SuspendFunction;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Weave
public abstract class CancellableKt {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static <T> void startCoroutineCancellable(Function1<? super Continuation<? super T>, ? extends java.lang.Object> f, Continuation<? super T> cont) {
		String continuationString = Utils.getContinuationString(cont);
		if(cont != null && !(cont instanceof SuspendFunction)) {
			if(!(cont instanceof NRContinuationWrapper) && !Utils.ignoreContinuation(continuationString)) {
				NRContinuationWrapper<? super T> wrapper = new NRContinuationWrapper<>(cont, continuationString);
				cont = wrapper;
			}
		}
		if(continuationString != null) {
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("Continuation", continuationString);
		}
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("Suspend-Type", "Function1");
		if(!(f instanceof NRFunction1Wrapper)) {
			NRFunction1Wrapper wrapper = new NRFunction1Wrapper<>(f);
			f = wrapper;
		}
		Weaver.callOriginal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Trace
	public static <R, T> void startCoroutineCancellable(Function2<? super R, ? super Continuation<? super T>, ? extends java.lang.Object> f, R receiver, Continuation<? super T> cont, Function1<? super java.lang.Throwable, kotlin.Unit> onCancellation) {
		String continuationString = Utils.getContinuationString(cont);
		if(!(cont instanceof SuspendFunction)) {
			// create continuation wrapper if needed
			if(cont != null && !Utils.ignoreContinuation(continuationString) && !(cont instanceof NRContinuationWrapper)) {
				NRContinuationWrapper<? super T> wrapper = new NRContinuationWrapper<>(cont, continuationString);
				cont = wrapper;
			}
		}
		if(continuationString != null) {
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("Continuation", continuationString);
		}
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("Suspend-Type", "Function2");
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("Receiver", receiver.getClass().getName());

		if(onCancellation != null && !(onCancellation instanceof NRFunction1Wrapper)) {
			NRFunction1Wrapper wrapper = new NRFunction1Wrapper<>(onCancellation);
			onCancellation = wrapper;

		}
		if(!(f instanceof NRFunction2Wrapper)) {
			NRFunction2Wrapper<? super R, ? super Continuation<? super T>, ? extends java.lang.Object> wrapper = new NRFunction2Wrapper<>(f);
			f = wrapper;
		}
		Weaver.callOriginal();
	}

	@Trace
	public static void startCoroutineCancellable(Continuation<? super kotlin.Unit> completion, Continuation<?> cont) {
		String completionString = Utils.getContinuationString(completion);
		if(completion != null && !(completion instanceof SuspendFunction)) {
			// create continuation wrapper if needed
			if(!Utils.ignoreContinuation(completionString) && !(completion instanceof NRContinuationWrapper)) {
				NRContinuationWrapper<? super kotlin.Unit> wrapper = new NRContinuationWrapper<>(completion, completionString);
				completion = wrapper;
			}
		}
		String continuationString = Utils.getContinuationString(cont);
		if(cont != null && !(cont instanceof SuspendFunction)) {
			// create continuation wrapper if needed
			if(cont != null && !Utils.ignoreContinuation(continuationString) && !(cont instanceof NRContinuationWrapper)) {
				NRContinuationWrapper<?> wrapper = new NRContinuationWrapper<>(cont, continuationString);
				cont = wrapper;
			}
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		if(completionString != null) {
			traced.addCustomAttribute("Completion", completionString);
		}
		if(continuationString != null) {
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("Continuation", continuationString);
		}
		traced.addCustomAttribute("Suspend-Type", "None");
		Weaver.callOriginal();
	}

}
