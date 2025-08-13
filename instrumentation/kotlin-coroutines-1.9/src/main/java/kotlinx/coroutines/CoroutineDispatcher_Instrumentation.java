package kotlinx.coroutines;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRRunnable;
import com.newrelic.instrumentation.kotlin.coroutines_19.Utils;

import kotlin.coroutines.CoroutineContext;

/*
 * Dispatchers are used to dispatch tasks to another thread.  By wrapping the Runable
 * we can track the Coroutine across threads
 */
@Weave(type = MatchType.BaseClass, originalName = "kotlinx.coroutines.CoroutineDispatcher")
public abstract class CoroutineDispatcher_Instrumentation {

	public void dispatch(CoroutineContext ctx, Runnable r) {
		NRRunnable wrapper = Utils.getRunnableWrapper(r);
		if(wrapper != null) {
			r = wrapper;
		}
		
		Weaver.callOriginal();
	}
}
