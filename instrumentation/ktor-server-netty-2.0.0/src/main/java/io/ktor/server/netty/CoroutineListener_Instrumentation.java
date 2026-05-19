package io.ktor.server.netty;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.util.concurrent.Future;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CancellableContinuation;

@Weave(originalName = "io.ktor.server.netty.CoroutineListener")
abstract class CoroutineListener_Instrumentation<T, F extends Future<T>> {

	@NewField
	Segment segment = null;
	
	public CoroutineListener_Instrumentation(F future, CancellableContinuation<? super T> continuation, Function2<? super java.lang.Throwable, ? super kotlin.coroutines.Continuation<? super T>, kotlin.Unit> exception) {
		
	}
	
	@Trace
	public void invoke(Throwable t) {
		if(segment == null) {
			segment = NewRelic.getAgent().getTransaction().startSegment("CoroutineListener");
		}
		Weaver.callOriginal();
	}
	
	public void operationComplete(F f) {
		if(segment != null) {
			segment.end();
			segment = null;
		}
		Weaver.callOriginal();
	}
}
