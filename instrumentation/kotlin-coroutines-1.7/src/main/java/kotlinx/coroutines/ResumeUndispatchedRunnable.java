package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
abstract class ResumeUndispatchedRunnable {
	
	@NewField
	private Token token = null;

	public ResumeUndispatchedRunnable(CoroutineDispatcher dispatcher, CancellableContinuation<? super kotlin.Unit> cont) {
		Token t = NewRelic.getAgent().getTransaction().getToken();
		if(t != null && t.isActive()) {
			token = t;
		} else if(t != null) {
			t.expire();
			t = null;
		}
	}

	@Trace(async = true)
	public void run() {
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		Weaver.callOriginal();
	}
}
