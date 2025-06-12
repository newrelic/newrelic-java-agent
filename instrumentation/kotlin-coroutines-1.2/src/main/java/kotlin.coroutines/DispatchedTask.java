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
			String cName = Utils.getCoroutineName(context, continuation);
			
			if(!notCreated(cName) && context != null && !Utils.ignoreDispatched(cName)) {
				Token t = Utils.getToken(context);
				if(t != null) t.link();
				if(cName != null)
					NewRelic.getAgent().getTracedMethod().setMetricName("Custom","DispatchedTask",cName);
			}
		}
		Weaver.callOriginal();
	}
	
	private boolean notCreated(String cName) {
		if(cName.equals(Utils.CREATEMETHOD1)) return true;
		if(cName.equals(Utils.CREATEMETHOD2)) return true;
		return false;
	}
}
