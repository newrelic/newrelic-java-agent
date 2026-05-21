package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import kotlin.coroutines.Continuation;

@Weave(originalName = "kotlinx.coroutines.Deferred", type = MatchType.Interface)
public class Deferred_Instrumentation<T> {

    @Trace
    public Object await(Continuation<? super T>  continuation) {
        return Weaver.callOriginal();
    }
}
