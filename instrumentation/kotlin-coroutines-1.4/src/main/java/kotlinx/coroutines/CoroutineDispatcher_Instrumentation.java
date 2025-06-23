package kotlinx.coroutines;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_14.NRRunnable;
import com.newrelic.instrumentation.kotlin.coroutines_14.Utils;

import kotlin.coroutines.CoroutineContext;

@Weave(type = MatchType.BaseClass, originalName = "kotlinx.coroutines.CoroutineDispatcher")
public abstract class CoroutineDispatcher_Instrumentation {

	public abstract boolean isDispatchNeeded(CoroutineContext coroutineContext);

	public void dispatch(CoroutineContext ctx, Runnable r) {
        if (isDispatchNeeded(ctx)) {
            NRRunnable wrapper = Utils.getRunnableWrapper(r);
            if (wrapper != null) {
                r = wrapper;
            }
        }

        Weaver.callOriginal();
	}
}
