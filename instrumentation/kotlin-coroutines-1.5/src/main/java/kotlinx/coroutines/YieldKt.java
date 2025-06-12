package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_15.Utils;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave
public class YieldKt {

    @Trace
    public static Object yield(Continuation<? super Unit> cont) {
        String cont_string = Utils.getContinuationString(cont);
        if(cont_string != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Yield","yield",cont_string);
        }
        return Weaver.callOriginal();
    }
}
