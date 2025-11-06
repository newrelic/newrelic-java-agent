package kotlin.coroutines.jvm.internal;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.suspends.SuspendsUtils;
import kotlin.coroutines.Continuation;

@Weave(type = MatchType.BaseClass, originalName = "kotlin.coroutines.jvm.internal.BaseContinuationImpl")
public abstract class BaseContinuationImpl_Instrumentation implements Continuation<Object> {

	protected Object invokeSuspend(Object result) {
		ExitTracer tracer = SuspendsUtils.getSuspendTracer(this);
        Object value = null;
        try {
            value = Weaver.callOriginal();
        } catch (Exception e) {
			if(tracer != null) {
				tracer.finish(e);
			}
			throw e;
        }
        if (tracer != null) {
			tracer.finish(0, value);
		}
		return value;
	}

}
