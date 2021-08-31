package kotlinx.coroutines.intrinsics;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Weave
public abstract class CancellableKt {

	@Trace
	public static <T> void startCoroutineCancellable(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> c) {
		Weaver.callOriginal();
	}

	@Trace
	public static <R, T> void startCoroutineCancellable(Function2<? super R, ? super Continuation<? super T>, ? extends Object> f, R r, Continuation<? super T> c) {
		Weaver.callOriginal();
	}

}
