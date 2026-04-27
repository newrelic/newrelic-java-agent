package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import kotlin.Unit;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;

@Weave(originalName = "kotlinx.coroutines.CancellableContinuationImpl")
public abstract class CancellableContinuationImpl_Instrumentation<T> {

    @Trace
    public void resumeWith(Object obj) {
        Weaver.callOriginal();
    }

    @Trace
    public java.lang.Object tryResumeWithException(Throwable t) {

        return Weaver.callOriginal();
    }

    @Trace
    public Object tryResume(T t, Object o) {
        return Weaver.callOriginal();
    }

    @Trace
    public <R extends T> java.lang.Object tryResume(R r, Object obj, Function3<? super Throwable, ? super R, ? super CoroutineContext, Unit> function3) {
        return Weaver.callOriginal();
    }

    @Trace
    public void completeResume(Object object) {
        Weaver.callOriginal();
    }

    @Trace
    public boolean cancel(Throwable t) {
        return Weaver.callOriginal();
    }

    @Trace
    public void invokeOnCancellation(Function1<? super Throwable, Unit> function1) {
        Weaver.callOriginal();
    }

    @Trace
    public void resumeUndispatched(CoroutineDispatcher dispatcher, T t) {
        Weaver.callOriginal();
    }

    @Trace
    public void resumeUndispatchedWithException(CoroutineDispatcher dispatcher, Throwable t) {
        Weaver.callOriginal();
    }

    @Trace
    public <R extends T> void resume(R r, Function3<? super Throwable, ? super R, ? super CoroutineContext, Unit> function3) {
        Weaver.callOriginal();
    }
}
