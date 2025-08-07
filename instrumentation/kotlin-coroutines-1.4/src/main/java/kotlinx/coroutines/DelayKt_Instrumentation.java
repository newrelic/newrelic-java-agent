package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_14.NRDelayContinuation;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave(originalName = "kotlinx.coroutines.DelayKt")
public class DelayKt_Instrumentation {

    @Trace
    public static Object awaitCancellation(Continuation<?> cont) {
        if(!(cont instanceof NRDelayContinuation)) {
            cont = new NRDelayContinuation<>(cont,"AwaitCancellation");
        }
        return Weaver.callOriginal();
    }

    @Trace
    public static Object delay(long timeMillis, Continuation<? super Unit> cont) {
        if(!(cont instanceof NRDelayContinuation)) {
            cont = new NRDelayContinuation<>(cont,"Delay");
        }
        return Weaver.callOriginal();
    }

}
