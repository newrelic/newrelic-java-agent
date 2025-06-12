package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_15.NRRunnable;
import com.newrelic.instrumentation.kotlin.coroutines_15.Utils;

@Weave(type = MatchType.BaseClass)
abstract class DispatcherExecutor {

    public void execute(Runnable r) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","DispatcherExecutor","execute");
        NRRunnable wrapper = Utils.getRunnableWrapper(r);
        if(wrapper != null) {
            r = wrapper;
        }
        Weaver.callOriginal();
    }
}
