package kotlinx.coroutines;

import java.util.List;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.coroutines.Continuation;

@Weave(originalName = "kotlinx.coroutines.AwaitAll")
abstract class AwaitAll_Instrumentation<T> {

	@Trace
	public Object await(Continuation<? super List<? extends T>> cont) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","AwaitAll","await");
		Object result = Weaver.callOriginal();
		return result;
	}

}
