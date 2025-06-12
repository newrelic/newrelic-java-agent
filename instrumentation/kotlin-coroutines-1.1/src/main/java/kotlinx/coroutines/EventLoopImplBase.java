package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kotlin.coroutines.NRRunnable;

@Weave(type=MatchType.BaseClass)
public abstract class EventLoopImplBase {

	@SuppressWarnings("unused")
	private boolean enqueueImpl(Runnable r) {
		Token token = NewRelic.getAgent().getTransaction().getToken();
		NRRunnable wrapper = new NRRunnable(r, token);
		r = wrapper;
		boolean b = Weaver.callOriginal();
		if(!b) {
			wrapper.expireAndNullToken();
		}
		return b;
		
	}
}
