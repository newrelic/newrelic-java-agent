package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kotlin.coroutines.NRWrappedRunnable;

@Weave(type=MatchType.BaseClass)
public abstract class EventLoopBase {

    @SuppressWarnings("unused")
    private boolean enqueueImpl(Runnable r) {
        
        NRWrappedRunnable wrapper = null;
        if(!NRWrappedRunnable.class.isInstance(r)) {
            wrapper = new NRWrappedRunnable(r, NewRelic.getAgent().getTransaction().getToken());
            r = wrapper;
        }
        boolean b = Weaver.callOriginal();
        if(!b && wrapper != null) {
            wrapper.expireAndNullToken();
        }
        return b;
        
    }

}
