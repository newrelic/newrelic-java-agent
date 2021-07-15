package io.vertx.core.impl;

import java.util.concurrent.Executor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.NRRunnableWrapper;

@Weave
public abstract class TaskQueue {

	@Trace
	public void execute(Runnable task, Executor executor) {
		if(task == null || !(task instanceof NRRunnableWrapper)) {
			NRRunnableWrapper wrapper = new NRRunnableWrapper(task, NewRelic.getAgent().getTransaction().getToken());
			task = wrapper;
		}
		Weaver.callOriginal();
	}
	
}
