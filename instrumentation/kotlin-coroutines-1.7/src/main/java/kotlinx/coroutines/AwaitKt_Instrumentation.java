package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import java.util.Collection;
import java.util.List;

@Weave(originalName = "kotlinx.coroutines.AwaitKt")
public class AwaitKt_Instrumentation {

    @Trace
    public static <T> Object awaitAll(Deferred<? extends T>[] deferreds, Continuation<? super List<? extends T>> continuation) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","AwaitKt","awaitAll");
        return Weaver.callOriginal();
    }

    @Trace
    public static <T> Object awaitAll(Collection<? extends Deferred<? extends T>> deferreds, Continuation<? super List<? extends T>> continuation) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","AwaitKt","awaitAll");
        return Weaver.callOriginal();
    }

    @Trace
    public static Object joinAll(Job[] jobs, Continuation<? super Unit> continuation) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","AwaitKt","joinAll");
        return Weaver.callOriginal();
    }

    @Trace
    public static Object joinAll(Collection<? extends Job> jobs, Continuation<? super Unit>  continuation) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","AwaitKt","joinAll");
        return Weaver.callOriginal();
    }

}
