package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type=MatchType.BaseClass)
public abstract class CancelHandlerBase {

    @Trace
    public void invoke(Throwable t) {
        Weaver.callOriginal();
    }
}
