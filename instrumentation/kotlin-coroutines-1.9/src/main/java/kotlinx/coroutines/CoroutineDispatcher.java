package kotlinx.coroutines;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRRunnable;
import com.newrelic.instrumentation.kotlin.coroutines_19.Utils;

import kotlin.coroutines.CoroutineContext;

@Weave(type = MatchType.BaseClass)
public abstract class CoroutineDispatcher {

	public void dispatch(CoroutineContext ctx, Runnable r) {
		NRRunnable wrapper = Utils.getRunnableWrapper(r);
		if(wrapper != null) {
			r = wrapper;
		}
		
		Weaver.callOriginal();
	}
}
