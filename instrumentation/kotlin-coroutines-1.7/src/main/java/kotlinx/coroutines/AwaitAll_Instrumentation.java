package kotlinx.coroutines;

import java.util.List;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.coroutines.Continuation;

@Weave(originalName = "kotlinx.coroutines.AwaitAll")
abstract class AwaitAll_Instrumentation<T> {

	@NewField
	private Segment segment = null;

	public AwaitAll_Instrumentation(kotlinx.coroutines.Deferred<? extends T>[] deferreds) {
		segment = NewRelic.getAgent().getTransaction().startSegment("AwaitAll");
	}

	public Object await(Continuation<? super List<? extends T>> cont) {
        Object result = Weaver.callOriginal();
		if(segment != null) {
			segment.end();
			segment = null;
		}
		return result;
	}

}
