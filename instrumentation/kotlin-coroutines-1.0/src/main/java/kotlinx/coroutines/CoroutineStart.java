package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.coroutines.Continuation;

@Weave
public abstract class CoroutineStart {

    @Trace
    public final <T> void invoke(Function1<? super Continuation<? super T>, ? extends Object> f, Continuation<? super T> c) {
        Weaver.callOriginal();
    }
    
    @Trace
    public final <R, T> void invoke(Function2<? super R, ? super Continuation<? super T>, ? extends java.lang.Object> f, R r, Continuation<? super T> c) {
        Weaver.callOriginal();
    }
    
}
