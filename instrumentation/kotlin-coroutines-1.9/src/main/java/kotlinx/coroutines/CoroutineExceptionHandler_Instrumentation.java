package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "kotlinx.coroutines.CoroutineExceptionHandler")
public abstract class CoroutineExceptionHandler_Instrumentation {

        @Trace
        public void handleException(kotlin.coroutines.CoroutineContext ctx, java.lang.Throwable t) {
                NewRelic.noticeError(t);
                Weaver.callOriginal();
        }
}
