package kotlinx.coroutines;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "kotlinx.coroutines.CoroutineExceptionHandler", type = MatchType.Interface)
public class CoroutineExceptionHandler_Instrumentation {

    @NewField
    public ExitTracer tracer = null;

    public void handleException(kotlin.coroutines.CoroutineContext ctx, java.lang.Throwable t) {
        if(tracer != null) {
            tracer.finish(t);
        }
        Weaver.callOriginal();
    }
}
