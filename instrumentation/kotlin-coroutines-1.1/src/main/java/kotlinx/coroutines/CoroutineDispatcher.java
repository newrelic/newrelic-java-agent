package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kotlin.coroutines.NRRunnable;

import kotlin.coroutines.CoroutineContext;;

@Weave(type=MatchType.BaseClass)
public abstract class CoroutineDispatcher {

	@Trace(excludeFromTransactionTrace=true)
	public void dispatch(CoroutineContext ctx, Runnable r) {
		if(!NRRunnable.class.isInstance(r)) {
			NRRunnable wrapper = new NRRunnable(r, NewRelic.getAgent().getTransaction().getToken());
			r = wrapper;
		}
		Weaver.callOriginal();
	}
	
}
