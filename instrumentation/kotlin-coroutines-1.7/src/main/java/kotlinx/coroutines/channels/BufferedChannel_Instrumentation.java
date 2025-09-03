package kotlinx.coroutines.channels;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave(originalName = "kotlinx.coroutines.channels.BufferedChannel")
public abstract class BufferedChannel_Instrumentation<E> {

	@Trace(dispatcher = true)
	public Object receive(Continuation<? super E> cont) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","Channel",getClass().getSimpleName(),"receive");

		return Weaver.callOriginal();
	}

	@Trace(dispatcher = true)
	public void cancel(java.util.concurrent.CancellationException ex) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","Channel",getClass().getSimpleName(),"Cancel");

		 Weaver.callOriginal();
	}
	
	@Trace(dispatcher = true)
	public Object send(E e, Continuation<? super Unit> cont) {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","Coroutines","Channel",getClass().getSimpleName(),"send");
		return Weaver.callOriginal();
	}


}
