package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
abstract class InvokeOnCancelling {

    @Trace
    public void invoke(Throwable t) {
        Weaver.callOriginal();
    }

}
