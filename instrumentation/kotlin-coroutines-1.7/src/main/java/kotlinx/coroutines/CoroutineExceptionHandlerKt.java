package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class CoroutineExceptionHandlerKt {

        @Trace
        public static void handleCoroutineException(kotlin.coroutines.CoroutineContext ctx,Throwable t) {
                Weaver.callOriginal();
        }
}
