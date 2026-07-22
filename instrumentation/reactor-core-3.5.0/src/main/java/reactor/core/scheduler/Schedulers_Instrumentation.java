package reactor.core.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.NRRunnableWrapper;
import com.nr.instrumentation.reactor.ReactorUtils;

import reactor.core.Disposable;

@Weave(originalName = "reactor.core.scheduler.Schedulers")
public class Schedulers_Instrumentation {

	@Trace
	static Disposable directSchedule(ScheduledExecutorService exec, Runnable task, Disposable parent, long delay, TimeUnit unit) {
		NRRunnableWrapper wrapper = ReactorUtils.getRunnableWrapper(task);
		if(wrapper != null) {
			task = wrapper;
		}
		
		return Weaver.callOriginal();
	}
	
	public static Scheduler single() {
		return Weaver.callOriginal();
	}
	
	@Trace
	static Disposable workerSchedule(ScheduledExecutorService exec,
			Disposable.Composite tasks,
			Runnable task,
			long delay,
			TimeUnit unit) {
		NRRunnableWrapper wrapper = ReactorUtils.getRunnableWrapper(task);
		if(wrapper != null) {
			task = wrapper;
		}
		
		return Weaver.callOriginal();
	}
}
