package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_17.Utils;

@Weave
public abstract class EventLoopImplBase {

	@Weave(type = MatchType.BaseClass) 
	public static abstract class DelayedTask {

		public DelayedTask(long nanos) {
			if(Utils.DELAYED_ENABLED) {
				token = NewRelic.getAgent().getTransaction().getToken();
			}
		}

		public long nanoTime = Weaver.callOriginal();

		@NewField
		protected Token token = null;

		public void dispose() {
			if(token != null) {
				token.expire();
				token = null;
			}
			Weaver.callOriginal();
		}
	}

	@Weave
	static class DelayedResumeTask extends DelayedTask {


		public DelayedResumeTask(long nanos, kotlinx.coroutines.CancellableContinuation<? super kotlin.Unit> cont) {
			super(nanos);
		}

		@Trace(async = true)
		public void run() {
			if(token != null) {
				token.linkAndExpire();
				token = null;
			}
			
			NewRelic.recordResponseTimeMetric("Custom/Kotlin/DelayedResumeTask", nanoTime);
			Weaver.callOriginal();
		}
	}

	@Weave
	static class DelayedRunnableTask extends DelayedTask {

		public DelayedRunnableTask(long nanos, Runnable r) {
			super(nanos);
		}

		@Trace(async = true)
		public void run() {
			if(token != null) {
				token.linkAndExpire();
				token = null;
			}
			NewRelic.recordResponseTimeMetric("Custom/Kotlin/DelayedResumeTask", nanoTime);
			Weaver.callOriginal();
		}
	}

	@Weave
	public static class DelayedTaskQueue {

	}
}
