package com.newrelic.instrumentation.kotlin.coroutines_14;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.DispatchedTask;

public class NRRunnable implements Runnable {
	
	private Runnable delegate = null;
	private Token token = null;
	private static boolean isTransformed = false;
	
	public NRRunnable(Runnable r,Token t) {
		if(!isTransformed) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			isTransformed = true;
		}
		delegate = r;
		token = t;
	}

	@Override
	@Trace(async = true)
	public void run() {
		boolean nameSet = false;
		if(delegate != null && delegate instanceof DispatchedTask) {
			DispatchedTask<?> task = (DispatchedTask<?>)delegate;
			Continuation<?> cont_delegate = task.getDelegate$kotlinx_coroutines_core();
			if(cont_delegate != null) {
				String cont_string = Utils.getContinuationString(cont_delegate);
				if(cont_string == null) cont_string = cont_delegate.getClass().getName();
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom","DispatchedTask",Utils.getContinuationString(cont_delegate));
				nameSet = true;
			}
		}
		if(!nameSet) {
			String delegateType = delegate != null ? delegate.getClass().getName() : "null";
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Kotlin","AsyncRunnableWrapper",delegateType);			
		}
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		if(delegate != null) {
			delegate.run();
		}
	}

}
