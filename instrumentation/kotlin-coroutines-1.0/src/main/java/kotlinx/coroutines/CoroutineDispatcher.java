package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kotlin.coroutines.NRWrappedRunnable;

import kotlin.coroutines.CoroutineContext;;

@Weave(type=MatchType.BaseClass)
public abstract class CoroutineDispatcher {

    @Trace
    public void dispatch(CoroutineContext ctx, Runnable r) {
        if(!NRWrappedRunnable.class.isInstance(r)) {
            NRWrappedRunnable wrapper = new NRWrappedRunnable(r, NewRelic.getAgent().getTransaction().getToken());
            r = wrapper;
        }
        Weaver.callOriginal();
    }
    
}
