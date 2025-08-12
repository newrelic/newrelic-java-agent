package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRDelayCancellableContinuation;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRDelayContinuation;
import com.newrelic.instrumentation.kotlin.coroutines_17.NRRunnable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;

/**
 * Used to trace calls to delay function as a segment
 */
@Weave(type = MatchType.Interface, originalName = "kotlinx.coroutines.Delay")
public class Delay_Instrumentation {

    @Trace
    public Object delay(long timeMills, Continuation<? super Unit> continuation) {
        if(!(continuation instanceof NRDelayContinuation)) {
            continuation = new NRDelayContinuation<>(continuation,"Delay");
        }
        return Weaver.callOriginal();
    }

    @Trace
    public void scheduleResumeAfterDelay(long timeMills, CancellableContinuation<? super Unit> continuation) {
        if(!(continuation instanceof NRDelayContinuation)) {
            continuation = new NRDelayCancellableContinuation<>(continuation,"scheduleResumeAfterDelay");
        }
        Weaver.callOriginal();
    }

    @Trace
    public DisposableHandle invokeOnTimeout(long timeMills, Runnable r, CoroutineContext context) {
//        if(!(r instanceof NRRunnable)) {
//            Token token = NewRelic.getAgent().getTransaction().getToken();
//            if(token != null && token.isActive()) {
//                r = new NRRunnable(r, token);
//            } else if (token != null) {
//                token.expire();
//                token = null;
//            }
//        }
        return Weaver.callOriginal();
    }
}
