package kotlin.coroutines.jvm.internal;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.suspends.SuspendsUtils;
import kotlin.coroutines.Continuation;

@Weave(type = MatchType.BaseClass)
public abstract class BaseContinuationImpl implements Continuation<Object> {

	@Trace
	public void resumeWith(Object result) {
		Weaver.callOriginal();
	}

	protected Object invokeSuspend(Object result) {
		ExitTracer tracer = SuspendsUtils.getSuspendTracer(this);
		Object value = Weaver.callOriginal();
		if (tracer != null) {
			tracer.finish(0, value);
		}
		return value;
	}

	public abstract kotlin.coroutines.jvm.internal.CoroutineStackFrame getCallerFrame();
	public abstract StackTraceElement getStackTraceElement();
}
