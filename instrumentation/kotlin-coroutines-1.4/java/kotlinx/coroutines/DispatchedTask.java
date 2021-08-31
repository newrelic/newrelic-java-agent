package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines.Utils;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;

@Weave(type=MatchType.BaseClass)
public abstract class DispatchedTask<T> {

	public abstract Continuation<T> getDelegate$kotlinx_coroutines_core();
	
	@Trace(async=true)
	public void run() {
		Continuation<T> continuation = getDelegate$kotlinx_coroutines_core();
		if(continuation != null) {
			CoroutineContext context = continuation.getContext();
			if(continuation instanceof DispatchedContinuation) {
				DispatchedContinuation<T> dispatched = (DispatchedContinuation<T>)continuation;
				continuation = dispatched.continuation;
			}
			
			if(context != null && !Utils.ignoreDispatched(continuation.getClass(), context)) {
				Token t = Utils.getToken(context);
				if(t != null) t.link();
				String cName = Utils.getCoroutineName(context, continuation.getClass());
				if(cName != null)
					NewRelic.getAgent().getTracedMethod().setMetricName("Custom","DispatchedTask",cName);
			}
		}
		Weaver.callOriginal();
	}
	
}
