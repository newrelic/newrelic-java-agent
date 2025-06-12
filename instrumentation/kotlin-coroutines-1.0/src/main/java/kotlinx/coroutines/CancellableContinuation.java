package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type=MatchType.Interface)
public abstract class CancellableContinuation<T> {
    
    @Trace
    public void resumeUndispatched(CoroutineDispatcher d, T t) {
        Weaver.callOriginal();
    }
    
    @Trace
    public void resumeUndispatchedWithException(CoroutineDispatcher d, java.lang.Throwable t) {
        NewRelic.noticeError(t);
        Weaver.callOriginal();
    }

}
