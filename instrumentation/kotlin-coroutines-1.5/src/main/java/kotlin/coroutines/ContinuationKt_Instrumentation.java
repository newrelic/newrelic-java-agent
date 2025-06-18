package kotlin.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_15.NRFunction1Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_15.NRFunction2Wrapper;

import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Weave(originalName = "kotlin.coroutines.ContinuationKt")
public abstract class ContinuationKt_Instrumentation {

    @Trace
    public static <T> void startCoroutine(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> cont ) {
        if(!(f instanceof NRFunction1Wrapper)) {
            f = new NRFunction1Wrapper<>(f);
        }
        Weaver.callOriginal();
    }

    @Trace
    public static <R, T> void startCoroutine(Function2<? super R, ? super Continuation<? super T>, ? extends Object> f, R receiver, Continuation<? super T> cont) {
        if(!(f instanceof NRFunction2Wrapper)) {
            f = new NRFunction2Wrapper<>(f);
        }
        Weaver.callOriginal();
    }


}
