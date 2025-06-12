package kotlin.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRFunction1Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRFunction2Wrapper;

import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Weave
public abstract class ContinuationKt {

        @Trace
        public static <T> void startCoroutine(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> cont ) {
                if(!(f instanceof NRFunction1Wrapper)) {
                        NRFunction1Wrapper<? super Continuation<? super T>, ? extends Object> wrapper = new NRFunction1Wrapper<>(f);
                        f = wrapper;
                }
                Weaver.callOriginal();
        }

        @Trace
        public static <R, T> void startCoroutine(Function2<? super R, ? super Continuation<? super T>, ? extends Object> f, R receiver, Continuation<? super T> cont) {
                if(!(f instanceof NRFunction2Wrapper)) {
                        NRFunction2Wrapper<? super R, ? super Continuation<? super T>, ? extends Object> wrapper = new NRFunction2Wrapper<>(f);
                        f = wrapper;
                }
                Weaver.callOriginal();
        }


}
