package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_14.Utils;

@Weave(originalName = "kotlinx.coroutines.EventLoopImplBase")
public abstract class EventLoopImplBase_Instrumentation {

	public void schedule(long nanos, DelayedTask_Instrumentation task) {
		if(task.token == null) {
			Token t = NewRelic.getAgent().getTransaction().getToken();
			if(t != null) {
				if(!t.isActive()) {
					t.expire();
					t = null;
				} else {
					task.token = t;
				}
			}
		}
		Weaver.callOriginal();
	}

	@Weave(type = MatchType.BaseClass, originalName = "kotlinx.coroutines.EventLoopImplBase$DelayedTask")
	public static abstract class DelayedTask_Instrumentation {

		public DelayedTask_Instrumentation(long nanos) {
			if(Utils.DELAYED_ENABLED) {
				token = NewRelic.getAgent().getTransaction().getToken();
			}
		}

		public long nanoTime = Weaver.callOriginal();

		@NewField
		protected Token token = null;

		public int scheduleTask(long delay, DelayedTaskQueue_Instrumentation queue, EventLoopImplBase_Instrumentation eventLoop) {
			if(token == null) {
				token = NewRelic.getAgent().getTransaction().getToken();
			}

			return Weaver.callOriginal();
		}

		public void dispose() {
			if(token != null) {
				token.expire();
				token = null;
			}
			Weaver.callOriginal();
		}
	}

	@Weave(originalName = "kotlinx.coroutines.EventLoopImplBase$DelayedResumeTask")
	static class DelayedResumeTask_Instrumentation extends DelayedTask_Instrumentation {


		public DelayedResumeTask_Instrumentation(long nanos, kotlinx.coroutines.CancellableContinuation<? super kotlin.Unit> cont) {
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

	@Weave(originalName = "kotlinx.coroutines.EventLoopImplBase$DelayedRunnableTask")
	static class DelayedRunnableTask_Instrumentation extends DelayedTask_Instrumentation {

		public DelayedRunnableTask_Instrumentation(long nanos, Runnable r) {
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

	@Weave(originalName = "kotlinx.coroutines.EventLoopImplBase$DelayedTaskQueue")
	public static class DelayedTaskQueue_Instrumentation {

	}
}
