package kotlin.coroutines.intrinsics;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines.NRContinuationWrapper;
import com.newrelic.instrumentation.kotlin.coroutines.Utils;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Weave
abstract class IntrinsicsKt__IntrinsicsJvmKt {

	public static final <T> Continuation<Unit> createCoroutineUnintercepted(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> c) {
		String name = Utils.getCoroutineName(c.getContext(), c.getClass());
		Continuation<Unit> result = Weaver.callOriginal();
		if(!Utils.ignoreContinuation(name)) {
			NRContinuationWrapper<Unit> wrapper = new NRContinuationWrapper<Unit>(result, name);
			result = wrapper;
		}
		return result;
	}

	@Trace
	public static final <R, T> Continuation<kotlin.Unit> createCoroutineUnintercepted(Function2<? super R, ? super Continuation<? super T>, ? extends Object> f, R r, Continuation<? super T> c) {
		String name = Utils.getCoroutineName(c.getContext(), c.getClass());
		Continuation<Unit> result = Weaver.callOriginal();
		if(!Utils.ignoreContinuation(name)) {
			NRContinuationWrapper<Unit> wrapper = new NRContinuationWrapper<Unit>(result, name);
			result = wrapper;
		}
		return result;
	}

}
